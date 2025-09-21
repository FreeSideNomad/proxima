import { test, expect, Page } from '@playwright/test';

test.describe('OIDC Complete Authorization Flow', () => {
  let page: Page;
  const testClientId = 'e2e-test-client';
  const testRedirectUri = 'http://localhost:8080/test-callback';
  const testSubject = 'e2e-test@example.com';

  test.beforeEach(async ({ browser }) => {
    page = await browser.newPage();

    // Setup: Configure OIDC preset via API
    await setupOidcPreset();
  });

  test.afterEach(async () => {
    await page.close();
  });

  test('should complete full OIDC authorization flow', async () => {
    // Step 1: Navigate to OIDC testing page
    await page.goto('/proxima/ui/oidc-testing');
    await expect(page.locator('.lcars-panel-header')).toContainText('OIDC Authorization Flow Testing');

    // Step 2: Test discovery endpoints
    await page.click('text=Discovery Metadata');
    await expect(page.locator('#discovery-results')).toBeVisible();
    await expect(page.locator('#discovery-results .lcars-alert.success')).toBeVisible();

    await page.click('text=JWKS Endpoint');
    await expect(page.locator('#discovery-results .lcars-alert.success')).toBeVisible();

    // Step 3: Configure authorization flow
    await page.fill('#client-id', testClientId);
    await page.fill('#redirect-uri', testRedirectUri);
    await page.fill('#scope', 'openid profile email');

    // Generate state and nonce
    const state = `test-state-${Date.now()}`;
    const nonce = `test-nonce-${Date.now()}`;
    await page.fill('#state', state);
    await page.fill('#nonce', nonce);

    // Step 4: Generate authorization URL
    await page.click('text=Generate Authorization URL');
    await expect(page.locator('#auth-url-panel')).toBeVisible();

    const authUrl = await page.locator('#auth-url').inputValue();
    expect(authUrl).toContain('/oauth/authorize');
    expect(authUrl).toContain(`client_id=${testClientId}`);
    expect(authUrl).toContain(`redirect_uri=${encodeURIComponent(testRedirectUri)}`);
    expect(authUrl).toContain(`state=${state}`);
    expect(authUrl).toContain(`nonce=${nonce}`);

    // Step 5: Start authorization flow (simulate redirect)
    const authResponse = await page.request.get(authUrl, {
      maxRedirects: 0
    });
    expect(authResponse.status()).toBe(302);

    const redirectLocation = authResponse.headers()['location'];
    expect(redirectLocation).toContain(testRedirectUri);
    expect(redirectLocation).toContain('code=');
    expect(redirectLocation).toContain(`state=${state}`);

    // Step 6: Extract authorization code
    const urlParams = new URLSearchParams(redirectLocation.split('?')[1]);
    const authCode = urlParams.get('code');
    expect(authCode).toBeTruthy();
    expect(authCode).toHaveLength(32); // UUID without dashes

    // Step 7: Exchange authorization code for tokens
    await page.fill('#auth-code', authCode!);
    await page.fill('#token-client-id', testClientId);
    await page.fill('#token-redirect-uri', testRedirectUri);

    await page.click('text=Exchange for Tokens');

    // Wait for token response
    await expect(page.locator('#token-response-panel')).toBeVisible();

    const accessToken = await page.locator('#access-token').inputValue();
    const idToken = await page.locator('#id-token').inputValue();

    expect(accessToken).toBeTruthy();
    expect(idToken).toBeTruthy();
    expect(accessToken.split('.').length).toBe(3); // JWT format
    expect(idToken.split('.').length).toBe(3); // JWT format

    // Step 8: Verify token details
    await expect(page.locator('#token-type')).toContainText('Bearer');
    await expect(page.locator('#expires-in')).toContainText('3600 seconds');

    // Step 9: Test token validation at jwt.io
    await page.click('text=Validate at jwt.io');

    // Wait for new tab to open
    const [jwtIoPage] = await Promise.all([
      page.waitForEvent('popup'),
      // The click should open jwt.io
    ]);

    expect(jwtIoPage.url()).toContain('jwt.io');
    await jwtIoPage.close();

    // Step 10: Test proxy request with injected token
    const proxyResponse = await page.request.get('/api/users/test', {
      headers: {
        'Authorization': `Bearer ${accessToken}`
      }
    });

    expect(proxyResponse.ok()).toBeTruthy();
  });

  test('should handle authorization errors properly', async () => {
    await page.goto('/proxima/ui/oidc-testing');

    // Test with invalid client ID
    await page.fill('#client-id', 'invalid-client');
    await page.fill('#redirect-uri', testRedirectUri);
    await page.click('text=Generate Authorization URL');

    const authUrl = await page.locator('#auth-url').inputValue();
    const authResponse = await page.request.get(authUrl, {
      maxRedirects: 0
    });

    expect(authResponse.status()).toBe(302);
    const redirectLocation = authResponse.headers()['location'];
    expect(redirectLocation).toContain('error=invalid_client');
  });

  test('should handle token exchange errors', async () => {
    await page.goto('/proxima/ui/oidc-testing');

    // Try to exchange invalid authorization code
    await page.fill('#auth-code', 'invalid-code');
    await page.fill('#token-client-id', testClientId);
    await page.fill('#token-redirect-uri', testRedirectUri);

    await page.click('text=Exchange for Tokens');

    // Should show error alert
    await expect(page.locator('.lcars-alert.error')).toBeVisible();
    await expect(page.locator('.lcars-alert.error')).toContainText('Token exchange failed');
  });

  test('should prevent authorization code reuse', async () => {
    await page.goto('/proxima/ui/oidc-testing');

    // Step 1: Complete normal flow to get authorization code
    await page.fill('#client-id', testClientId);
    await page.fill('#redirect-uri', testRedirectUri);
    await page.click('text=Generate Authorization URL');

    const authUrl = await page.locator('#auth-url').inputValue();
    const authResponse = await page.request.get(authUrl, { maxRedirects: 0 });
    const redirectLocation = authResponse.headers()['location'];
    const urlParams = new URLSearchParams(redirectLocation.split('?')[1]);
    const authCode = urlParams.get('code')!;

    // Step 2: Use authorization code once (should succeed)
    await page.fill('#auth-code', authCode);
    await page.fill('#token-client-id', testClientId);
    await page.fill('#token-redirect-uri', testRedirectUri);
    await page.click('text=Exchange for Tokens');
    await expect(page.locator('#token-response-panel')).toBeVisible();

    // Step 3: Try to use same code again (should fail)
    await page.click('text=Clear Form');
    await page.fill('#auth-code', authCode);
    await page.fill('#token-client-id', testClientId);
    await page.fill('#token-redirect-uri', testRedirectUri);
    await page.click('text=Exchange for Tokens');

    await expect(page.locator('.lcars-alert.error')).toBeVisible();
    await expect(page.locator('.lcars-alert.error')).toContainText('already used');
  });

  test('should validate OIDC discovery metadata format', async () => {
    await page.goto('/proxima/ui/oidc-testing');

    await page.click('text=Discovery Metadata');
    await expect(page.locator('#discovery-results')).toBeVisible();

    // Check that discovery response contains required fields
    const discoveryText = await page.locator('#discovery-results pre').textContent();
    const discovery = JSON.parse(discoveryText!);

    expect(discovery).toHaveProperty('issuer');
    expect(discovery).toHaveProperty('authorization_endpoint');
    expect(discovery).toHaveProperty('token_endpoint');
    expect(discovery).toHaveProperty('jwks_uri');
    expect(discovery).toHaveProperty('response_types_supported');
    expect(discovery).toHaveProperty('subject_types_supported');
    expect(discovery).toHaveProperty('id_token_signing_alg_values_supported');

    expect(discovery.response_types_supported).toContain('code');
    expect(discovery.subject_types_supported).toContain('public');
    expect(discovery.id_token_signing_alg_values_supported).toContain('RS256');
  });

  test('should validate JWKS format', async () => {
    await page.goto('/proxima/ui/oidc-testing');

    await page.click('text=JWKS Endpoint');
    await expect(page.locator('#discovery-results')).toBeVisible();

    const jwksText = await page.locator('#discovery-results pre').textContent();
    const jwks = JSON.parse(jwksText!);

    expect(jwks).toHaveProperty('keys');
    expect(Array.isArray(jwks.keys)).toBeTruthy();
    expect(jwks.keys.length).toBeGreaterThan(0);

    const key = jwks.keys[0];
    expect(key).toHaveProperty('kty', 'RSA');
    expect(key).toHaveProperty('use', 'sig');
    expect(key).toHaveProperty('kid');
    expect(key).toHaveProperty('n');
    expect(key).toHaveProperty('e');
  });

  async function setupOidcPreset() {
    // Create RSA key for testing
    const keyResponse = await page.request.post('/proxima/api/jwt/keys/rsa', {
      data: { keyId: 'e2e-test-key' },
      headers: { 'Content-Type': 'application/json' }
    });
    expect(keyResponse.ok()).toBeTruthy();

    // Configure OIDC preset via configuration API
    const config = {
      presets: [{
        name: 'e2e-test-preset',
        displayName: 'E2E Test Preset',
        headers: {},
        oidcConfig: {
          enabled: true,
          subject: testSubject,
          clientId: testClientId,
          redirectUri: testRedirectUri,
          scopes: ['openid', 'profile', 'email'],
          tokenExpirationSeconds: 3600,
          algorithm: 'RS256',
          keyId: 'e2e-test-key',
          email: testSubject,
          name: 'E2E Test User',
          preferredUsername: 'e2etestuser',
          groups: ['test-group'],
          customClaims: {
            test_claim: 'test_value'
          }
        }
      }]
    };

    // This would need an API endpoint to update configuration
    // For now, we assume the preset is configured via application properties
  }
});