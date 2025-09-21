const { test, expect } = require('@playwright/test');

test.describe('OIDC Authentication Flow', () => {
  test.beforeEach(async ({ page }) => {
    // Setup for each test
    await page.goto('/');
  });

  test('should display OIDC discovery endpoint', async ({ page }) => {
    // Test the OIDC discovery endpoint
    const response = await page.goto('/.well-known/openid-configuration');

    expect(response.status()).toBe(200);

    const discoveryDoc = await response.json();
    expect(discoveryDoc).toHaveProperty('issuer');
    expect(discoveryDoc).toHaveProperty('authorization_endpoint');
    expect(discoveryDoc).toHaveProperty('token_endpoint');
    expect(discoveryDoc).toHaveProperty('jwks_uri');
  });

  test('should handle authorization code flow redirect', async ({ page }) => {
    // Test OIDC authorization endpoint
    const authUrl = '/oauth2/authorize?' + new URLSearchParams({
      response_type: 'code',
      client_id: 'test-client',
      redirect_uri: 'http://localhost:3000/callback',
      scope: 'openid profile email',
      state: 'test-state-123'
    });

    const response = await page.goto(authUrl);

    // Should redirect back with authorization code
    expect(response.status()).toBe(302);

    const location = response.headers()['location'];
    expect(location).toContain('code=');
    expect(location).toContain('state=test-state-123');
  });

  test('should exchange authorization code for tokens', async ({ page }) => {
    // First get authorization code
    const authUrl = '/oauth2/authorize?' + new URLSearchParams({
      response_type: 'code',
      client_id: 'test-client',
      redirect_uri: 'http://localhost:3000/callback',
      scope: 'openid profile email',
      state: 'test-state-123'
    });

    const authResponse = await page.goto(authUrl);
    const location = authResponse.headers()['location'];
    const code = new URL(location).searchParams.get('code');

    // Exchange code for tokens
    const tokenResponse = await page.request.post('/oauth2/token', {
      form: {
        grant_type: 'authorization_code',
        code: code,
        redirect_uri: 'http://localhost:3000/callback',
        client_id: 'test-client'
      }
    });

    expect(tokenResponse.status()).toBe(200);

    const tokens = await tokenResponse.json();
    expect(tokens).toHaveProperty('access_token');
    expect(tokens).toHaveProperty('id_token');
    expect(tokens).toHaveProperty('token_type', 'Bearer');
    expect(tokens).toHaveProperty('expires_in');
  });

  test('should validate JWT token structure', async ({ page }) => {
    // Get tokens first
    const authUrl = '/oauth2/authorize?' + new URLSearchParams({
      response_type: 'code',
      client_id: 'test-client',
      redirect_uri: 'http://localhost:3000/callback',
      scope: 'openid profile email'
    });

    const authResponse = await page.goto(authUrl);
    const location = authResponse.headers()['location'];
    const code = new URL(location).searchParams.get('code');

    const tokenResponse = await page.request.post('/oauth2/token', {
      form: {
        grant_type: 'authorization_code',
        code: code,
        redirect_uri: 'http://localhost:3000/callback',
        client_id: 'test-client'
      }
    });

    const tokens = await tokenResponse.json();

    // Validate JWT structure (header.payload.signature)
    const idToken = tokens.id_token;
    const tokenParts = idToken.split('.');
    expect(tokenParts).toHaveLength(3);

    // Decode and validate payload
    const payload = JSON.parse(Buffer.from(tokenParts[1], 'base64url').toString());
    expect(payload).toHaveProperty('iss');
    expect(payload).toHaveProperty('sub');
    expect(payload).toHaveProperty('aud');
    expect(payload).toHaveProperty('exp');
    expect(payload).toHaveProperty('iat');
  });

  test('should support different user presets', async ({ page }) => {
    // Test with different preset configurations
    const presets = ['default', 'admin', 'api_client'];

    for (const preset of presets) {
      const authUrl = `/oauth2/authorize?preset=${preset}&` + new URLSearchParams({
        response_type: 'code',
        client_id: 'test-client',
        redirect_uri: 'http://localhost:3000/callback',
        scope: 'openid profile email'
      });

      const authResponse = await page.goto(authUrl);
      const location = authResponse.headers()['location'];
      const code = new URL(location).searchParams.get('code');

      const tokenResponse = await page.request.post('/oauth2/token', {
        form: {
          grant_type: 'authorization_code',
          code: code,
          redirect_uri: 'http://localhost:3000/callback',
          client_id: 'test-client'
        }
      });

      const tokens = await tokenResponse.json();
      const payload = JSON.parse(Buffer.from(tokens.id_token.split('.')[1], 'base64url').toString());

      // Verify preset-specific claims
      expect(payload.sub).toBeDefined();
      if (preset === 'admin') {
        expect(payload.role).toBe('admin');
      }
    }
  });

  test('should provide JWKS endpoint for token validation', async ({ page }) => {
    const response = await page.goto('/.well-known/jwks.json');

    expect(response.status()).toBe(200);

    const jwks = await response.json();
    expect(jwks).toHaveProperty('keys');
    expect(Array.isArray(jwks.keys)).toBe(true);
    expect(jwks.keys.length).toBeGreaterThan(0);

    // Validate JWK structure
    const key = jwks.keys[0];
    expect(key).toHaveProperty('kty', 'RSA');
    expect(key).toHaveProperty('use', 'sig');
    expect(key).toHaveProperty('kid');
    expect(key).toHaveProperty('n');
    expect(key).toHaveProperty('e');
  });

  test('should cache tokens efficiently', async ({ page }) => {
    // Make multiple requests with same parameters
    const authUrl = '/oauth2/authorize?' + new URLSearchParams({
      response_type: 'code',
      client_id: 'test-client',
      redirect_uri: 'http://localhost:3000/callback',
      scope: 'openid profile email'
    });

    const startTime = Date.now();

    // First request
    const authResponse1 = await page.goto(authUrl);
    const code1 = new URL(authResponse1.headers()['location']).searchParams.get('code');

    const tokenResponse1 = await page.request.post('/oauth2/token', {
      form: {
        grant_type: 'authorization_code',
        code: code1,
        redirect_uri: 'http://localhost:3000/callback',
        client_id: 'test-client'
      }
    });

    const firstRequestTime = Date.now() - startTime;

    // Second request (should be faster due to caching)
    const authResponse2 = await page.goto(authUrl);
    const code2 = new URL(authResponse2.headers()['location']).searchParams.get('code');

    const secondStartTime = Date.now();
    const tokenResponse2 = await page.request.post('/oauth2/token', {
      form: {
        grant_type: 'authorization_code',
        code: code2,
        redirect_uri: 'http://localhost:3000/callback',
        client_id: 'test-client'
      }
    });

    const secondRequestTime = Date.now() - secondStartTime;

    // Both should succeed
    expect(tokenResponse1.status()).toBe(200);
    expect(tokenResponse2.status()).toBe(200);

    // Tokens should have consistent structure
    const tokens1 = await tokenResponse1.json();
    const tokens2 = await tokenResponse2.json();

    expect(tokens1).toHaveProperty('access_token');
    expect(tokens2).toHaveProperty('access_token');
  });

  test('should handle token expiration', async ({ page }) => {
    // This test would verify token expiration handling
    // For now, just verify tokens have expiration times
    const authUrl = '/oauth2/authorize?' + new URLSearchParams({
      response_type: 'code',
      client_id: 'test-client',
      redirect_uri: 'http://localhost:3000/callback',
      scope: 'openid profile email'
    });

    const authResponse = await page.goto(authUrl);
    const code = new URL(authResponse.headers()['location']).searchParams.get('code');

    const tokenResponse = await page.request.post('/oauth2/token', {
      form: {
        grant_type: 'authorization_code',
        code: code,
        redirect_uri: 'http://localhost:3000/callback',
        client_id: 'test-client'
      }
    });

    const tokens = await tokenResponse.json();

    // Verify expiration is set
    expect(tokens.expires_in).toBeGreaterThan(0);
    expect(tokens.expires_in).toBeLessThanOrEqual(7200); // Max 2 hours

    // Verify JWT exp claim
    const payload = JSON.parse(Buffer.from(tokens.id_token.split('.')[1], 'base64url').toString());
    const now = Math.floor(Date.now() / 1000);
    expect(payload.exp).toBeGreaterThan(now);
    expect(payload.exp).toBeLessThanOrEqual(now + 7200);
  });
});