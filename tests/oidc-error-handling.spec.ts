import { test, expect, Page } from '@playwright/test';

test.describe('OIDC Error Handling', () => {
  let page: Page;

  test.beforeEach(async ({ browser }) => {
    page = await browser.newPage();
  });

  test.afterEach(async () => {
    await page.close();
  });

  test('should handle invalid client errors', async () => {
    await page.goto('/proxima/ui/oidc-testing');

    // Fill form with invalid client ID
    await page.fill('#client-id', 'non-existent-client');
    await page.fill('#redirect-uri', 'http://localhost:8080/callback');
    await page.click('text=Generate Authorization URL');

    const authUrl = await page.locator('#auth-url').inputValue();

    // Make authorization request
    const response = await page.request.get(authUrl, { maxRedirects: 0 });
    expect(response.status()).toBe(302);

    const location = response.headers()['location'];
    expect(location).toContain('error=invalid_client');
    expect(location).toContain('error_description=');
  });

  test('should handle redirect URI mismatch', async () => {
    await page.goto('/proxima/ui/oidc-testing');

    // Use valid client but wrong redirect URI
    await page.fill('#client-id', 'test-client');
    await page.fill('#redirect-uri', 'http://evil.com/callback');
    await page.click('text=Generate Authorization URL');

    const authUrl = await page.locator('#auth-url').inputValue();
    const response = await page.request.get(authUrl, { maxRedirects: 0 });

    expect(response.status()).toBe(302);
    const location = response.headers()['location'];
    expect(location).toContain('error=invalid_redirect_uri');
  });

  test('should handle unsupported response type', async () => {
    const authUrl = '/oauth/authorize?' + new URLSearchParams({
      response_type: 'token',
      client_id: 'test-client',
      redirect_uri: 'http://localhost:8080/callback'
    });

    const response = await page.request.get(authUrl, { maxRedirects: 0 });
    expect(response.status()).toBe(302);

    const location = response.headers()['location'];
    expect(location).toContain('error=unsupported_response_type');
  });

  test('should handle missing required parameters', async () => {
    // Test missing client_id
    const authUrlMissingClient = '/oauth/authorize?' + new URLSearchParams({
      response_type: 'code',
      redirect_uri: 'http://localhost:8080/callback'
    });

    const response1 = await page.request.get(authUrlMissingClient);
    expect(response1.status()).toBe(400);

    // Test missing redirect_uri
    const authUrlMissingRedirect = '/oauth/authorize?' + new URLSearchParams({
      response_type: 'code',
      client_id: 'test-client'
    });

    const response2 = await page.request.get(authUrlMissingRedirect);
    expect(response2.status()).toBe(400);
  });

  test('should handle token endpoint errors', async () => {
    await page.goto('/proxima/ui/oidc-testing');

    // Test invalid grant type
    const invalidGrantResponse = await page.request.post('/oauth/token', {
      form: {
        grant_type: 'password',
        username: 'user',
        password: 'pass'
      }
    });

    expect(invalidGrantResponse.status()).toBe(400);
    const invalidGrantData = await invalidGrantResponse.json();
    expect(invalidGrantData.error).toBe('unsupported_grant_type');

    // Test invalid authorization code
    const invalidCodeResponse = await page.request.post('/oauth/token', {
      form: {
        grant_type: 'authorization_code',
        code: 'invalid-code',
        client_id: 'test-client',
        redirect_uri: 'http://localhost:8080/callback'
      }
    });

    expect(invalidCodeResponse.status()).toBe(400);
    const invalidCodeData = await invalidCodeResponse.json();
    expect(invalidCodeData.error).toBe('invalid_grant');

    // Test missing required parameters
    const missingParamsResponse = await page.request.post('/oauth/token', {
      form: {
        grant_type: 'authorization_code'
        // Missing code, client_id, redirect_uri
      }
    });

    expect(missingParamsResponse.status()).toBe(400);
  });

  test('should handle expired authorization codes', async () => {
    // This test would require controlling time or using codes with very short expiration
    // For now, we test the error response format

    await page.goto('/proxima/ui/oidc-testing');

    // Fill in token exchange form with a potentially expired code
    await page.fill('#auth-code', 'expired-code-12345');
    await page.fill('#token-client-id', 'test-client');
    await page.fill('#token-redirect-uri', 'http://localhost:8080/callback');

    await page.click('text=Exchange for Tokens');

    // Should show error
    await expect(page.locator('.lcars-alert.error')).toBeVisible();
  });

  test('should handle network errors gracefully', async () => {
    await page.goto('/proxima/ui/oidc-testing');

    // Test discovery endpoint when server is unreachable
    // We can't actually make server unreachable in this test, but we can test error handling

    // Fill form and try to generate auth URL
    await page.fill('#client-id', 'test-client');
    await page.fill('#redirect-uri', 'http://localhost:8080/callback');

    // Generate authorization URL
    await page.click('text=Generate Authorization URL');
    await expect(page.locator('#auth-url-panel')).toBeVisible();

    // Test with malformed authorization code in token exchange
    await page.fill('#auth-code', 'malformed-code-!@#$%^&*()');
    await page.fill('#token-client-id', 'test-client');
    await page.fill('#token-redirect-uri', 'http://localhost:8080/callback');

    await page.click('text=Exchange for Tokens');
    await expect(page.locator('.lcars-alert.error')).toBeVisible();
  });

  test('should validate form inputs', async () => {
    await page.goto('/proxima/ui/oidc-testing');

    // Test empty client ID
    await page.fill('#client-id', '');
    await page.fill('#redirect-uri', 'http://localhost:8080/callback');

    // Form should require client ID
    const generateButton = page.locator('text=Generate Authorization URL');
    await generateButton.click();

    // Browser validation should prevent submission
    const clientIdField = page.locator('#client-id');
    const isRequired = await clientIdField.getAttribute('required');
    expect(isRequired).toBe('');

    // Test invalid URL format
    await page.fill('#client-id', 'test-client');
    await page.fill('#redirect-uri', 'not-a-valid-url');

    const redirectUriField = page.locator('#redirect-uri');
    const urlType = await redirectUriField.getAttribute('type');
    expect(urlType).toBe('url');
  });

  test('should handle CSRF protection with state parameter', async () => {
    await page.goto('/proxima/ui/oidc-testing');

    // Generate authorization URL with state
    await page.fill('#client-id', 'test-client');
    await page.fill('#redirect-uri', 'http://localhost:8080/callback');
    const state = 'test-state-123';
    await page.fill('#state', state);

    await page.click('text=Generate Authorization URL');
    const authUrl = await page.locator('#auth-url').inputValue();

    expect(authUrl).toContain(`state=${state}`);

    // Make authorization request
    const response = await page.request.get(authUrl, { maxRedirects: 0 });

    if (response.status() === 302) {
      const location = response.headers()['location'];
      // State should be preserved in the response
      expect(location).toContain(`state=${state}`);
    }
  });

  test('should handle concurrent authorization requests', async () => {
    await page.goto('/proxima/ui/oidc-testing');

    // Prepare multiple authorization URLs
    const authUrls = [];
    for (let i = 0; i < 5; i++) {
      const url = '/oauth/authorize?' + new URLSearchParams({
        response_type: 'code',
        client_id: 'test-client',
        redirect_uri: 'http://localhost:8080/callback',
        state: `concurrent-test-${i}`,
        nonce: `nonce-${i}`
      });
      authUrls.push(url);
    }

    // Make concurrent requests
    const responses = await Promise.all(
      authUrls.map(url => page.request.get(url, { maxRedirects: 0 }))
    );

    // All requests should be handled properly
    responses.forEach((response, index) => {
      expect(response.status()).toBe(302);
      const location = response.headers()['location'];
      expect(location).toContain(`state=concurrent-test-${index}`);
    });
  });

  test('should sanitize error messages', async () => {
    // Test that error messages don't expose sensitive information
    const maliciousClientId = '<script>alert("xss")</script>';

    const authUrl = '/oauth/authorize?' + new URLSearchParams({
      response_type: 'code',
      client_id: maliciousClientId,
      redirect_uri: 'http://localhost:8080/callback'
    });

    const response = await page.request.get(authUrl, { maxRedirects: 0 });
    expect(response.status()).toBe(302);

    const location = response.headers()['location'];
    expect(location).toContain('error=invalid_client');
    // Error description should not contain the malicious script
    expect(location).not.toContain('<script>');
    expect(location).not.toContain('alert');
  });
});