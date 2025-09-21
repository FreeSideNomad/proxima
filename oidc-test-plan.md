# OIDC Test Plan for Proxima

## Overview
This document outlines the comprehensive testing strategy for implementing OIDC (OpenID Connect) authentication flow in Proxima reverse proxy.

## Testing Philosophy
Following Test-Driven Development (TDD) approach:
1. Write failing tests first
2. Implement minimal code to pass tests
3. Refactor and enhance

## 1. Unit Tests

### 1.1 Core Models Tests
**File: `AuthorizationCodeTest.java`**
- [ ] Test authorization code generation
- [ ] Test expiration validation
- [ ] Test authorization code immutability
- [ ] Test cleanup of expired codes

**File: `OidcPresetConfigTest.java`**
- [ ] Test preset configuration validation
- [ ] Test claims generation with custom claims
- [ ] Test token expiration settings
- [ ] Test client configuration validation
- [ ] Test default values

**File: `OidcTokensTest.java`**
- [ ] Test token expiration logic
- [ ] Test token validity checking
- [ ] Test token serialization/deserialization

### 1.2 Service Layer Tests
**File: `AuthorizationCodeServiceTest.java`**
- [ ] Test code generation with proper entropy
- [ ] Test code storage and retrieval
- [ ] Test code validation with state/nonce
- [ ] Test automatic cleanup of expired codes
- [ ] Test concurrent code generation

**File: `OAuthControllerTest.java`**
- [ ] Test `/oauth/authorize` endpoint parameter validation
- [ ] Test authorization response generation
- [ ] Test `/oauth/token` endpoint validation
- [ ] Test token exchange for authorization code
- [ ] Test error responses (invalid codes, expired, etc.)

**File: `OidcDiscoveryControllerTest.java`**
- [ ] Test `/.well-known/openid_configuration` response
- [ ] Test `/oauth/jwks` endpoint
- [ ] Test discovery metadata correctness
- [ ] Test JWK format validation

**File: `OidcTokenServiceEnhancedTest.java`**
- [ ] Test enhanced token generation for authorization flow
- [ ] Test token caching with authorization codes
- [ ] Test token refresh scenarios
- [ ] Test multiple client support

### 1.3 UI Component Tests
**File: `OidcPresetFormTest.java`**
- [ ] Test OIDC configuration form validation
- [ ] Test client configuration UI
- [ ] Test preset switching with OIDC
- [ ] Test error handling in forms

## 2. Integration Tests

### 2.1 Authorization Code Flow Tests
**File: `OidcAuthorizationFlowIntegrationTest.java`**
- [ ] Complete authorization code flow from start to finish
- [ ] Test with multiple clients
- [ ] Test state parameter validation
- [ ] Test nonce parameter handling
- [ ] Test redirect URI validation
- [ ] Test token injection into proxy requests

### 2.2 Configuration Integration Tests
**File: `OidcConfigurationIntegrationTest.java`**
- [ ] Test preset configuration with OIDC enabled
- [ ] Test dynamic configuration updates
- [ ] Test configuration validation
- [ ] Test backward compatibility

### 2.3 Token Management Integration Tests
**File: `OidcTokenManagementIntegrationTest.java`**
- [ ] Test token caching across restarts
- [ ] Test token expiration and refresh
- [ ] Test multiple concurrent sessions
- [ ] Test token invalidation

## 3. End-to-End Tests (Playwright)

### 3.1 Complete OIDC Workflow
**File: `oidc-complete-flow.spec.ts`**
```javascript
test('Complete OIDC Authorization Flow', async ({ page }) => {
  // 1. Configure OIDC preset
  await page.goto('/proxima/ui/presets');
  await page.click('text=Create Preset');
  await page.fill('#preset-name', 'oidc-test');
  await page.check('#oidc-enabled');
  await page.fill('#subject', 'test@example.com');
  await page.fill('#client-id', 'test-client');
  await page.fill('#redirect-uri', 'http://localhost:8080/callback');
  await page.click('text=Save');

  // 2. Activate preset
  await page.click('text=Activate');
  await expect(page.locator('.preset-status')).toHaveText('ACTIVE');

  // 3. Initiate authorization flow
  const authUrl = await page.locator('#auth-url').textContent();
  await page.goto(authUrl);

  // 4. Verify authorization response
  await expect(page.url()).toContain('callback');
  await expect(page.url()).toContain('code=');

  // 5. Test token injection in proxy request
  await page.goto('/api/users/test');
  const response = await page.waitForResponse('/api/users/test');
  const headers = await response.allHeaders();
  expect(headers.authorization).toContain('Bearer');
});
```

### 3.2 Multi-Client Testing
**File: `oidc-multi-client.spec.ts`**
- [ ] Test multiple OIDC clients
- [ ] Test client isolation
- [ ] Test different token configurations
- [ ] Test client-specific scopes

### 3.3 Error Scenarios
**File: `oidc-error-handling.spec.ts`**
- [ ] Test invalid authorization codes
- [ ] Test expired tokens
- [ ] Test malformed requests
- [ ] Test network failures
- [ ] Test invalid client configurations

### 3.4 UI/UX Testing
**File: `oidc-ui-functionality.spec.ts`**
- [ ] Test OIDC configuration forms
- [ ] Test preset switching
- [ ] Test authorization flow UI
- [ ] Test error messages and validation
- [ ] Test responsive design

## 4. Performance Tests

### 4.1 Load Testing
**File: `OidcPerformanceTest.java`**
- [ ] Test authorization code generation under load
- [ ] Test token validation performance
- [ ] Test cache performance with high concurrency
- [ ] Test memory usage with many active sessions

### 4.2 Benchmark Tests
- [ ] Authorization endpoint response time
- [ ] Token endpoint response time
- [ ] Token validation speed
- [ ] Cache hit/miss ratios

## 5. Security Tests

### 5.1 Security Validation
**File: `OidcSecurityTest.java`**
- [ ] Test authorization code entropy and randomness
- [ ] Test state parameter validation (CSRF protection)
- [ ] Test nonce replay protection
- [ ] Test token expiration enforcement
- [ ] Test client credentials validation
- [ ] Test redirect URI validation

### 5.2 Penetration Testing Scenarios
- [ ] Authorization code brute force attempts
- [ ] Token replay attacks
- [ ] CSRF attacks on authorization endpoint
- [ ] Client impersonation attempts

## 6. Compatibility Tests

### 6.1 OIDC Compliance
- [ ] Test with real OIDC clients (Postman, curl)
- [ ] Test discovery endpoint compliance
- [ ] Test JWKS endpoint compliance
- [ ] Test with jwt.io for token validation

### 6.2 Browser Compatibility
- [ ] Test in Chrome, Firefox, Safari, Edge
- [ ] Test mobile browsers
- [ ] Test with different viewport sizes

## 7. Regression Tests

### 7.1 Existing Functionality
- [ ] Test non-OIDC presets still work
- [ ] Test basic proxy functionality unchanged
- [ ] Test JWT generation without OIDC
- [ ] Test configuration validation

### 7.2 Migration Tests
- [ ] Test upgrading from non-OIDC to OIDC presets
- [ ] Test configuration migration
- [ ] Test backward compatibility

## 8. Test Data and Setup

### 8.1 Test Configuration Files
- [ ] `test-oidc-config.json` - Complete OIDC configuration
- [ ] `test-multi-client.json` - Multiple client setup
- [ ] `test-minimal-config.json` - Minimal OIDC setup

### 8.2 Mock Services
- [ ] Mock OIDC provider for testing
- [ ] Mock downstream services
- [ ] Test token validation service

## 9. Continuous Integration

### 9.1 CI Pipeline
- [ ] Unit tests run on every commit
- [ ] Integration tests on pull requests
- [ ] E2E tests on release candidates
- [ ] Performance tests on weekly schedule

### 9.2 Test Reporting
- [ ] Code coverage reports (minimum 90%)
- [ ] Performance benchmarks
- [ ] Security scan results
- [ ] E2E test results with screenshots

## 10. Manual Testing Checklist

### 10.1 Functional Testing
- [ ] Complete authorization flow works end-to-end
- [ ] Token injection works in proxy requests
- [ ] Error handling displays appropriate messages
- [ ] Configuration UI is intuitive and responsive

### 10.2 Usability Testing
- [ ] OIDC setup process is clear
- [ ] Error messages are helpful
- [ ] UI flows are logical
- [ ] Documentation is complete

## Test Environment Requirements

### Development Environment
- Java 17+
- Spring Boot test framework
- Testcontainers for integration tests
- Playwright for E2E tests
- JMeter for performance tests

### Test Data
- Sample OIDC configurations
- Test user accounts
- Mock downstream services
- Test certificates and keys

## Success Criteria

- [ ] All unit tests pass (100%)
- [ ] Integration tests cover all critical paths
- [ ] E2E tests cover complete user workflows
- [ ] Performance meets requirements (< 100ms response time)
- [ ] Security tests pass vulnerability scans
- [ ] Code coverage >= 90%
- [ ] All manual test scenarios pass

## Risk Mitigation

### High-Risk Areas
1. **Token Security**: Comprehensive security testing
2. **State Management**: Thorough concurrency testing
3. **Configuration Migration**: Extensive backward compatibility testing
4. **Performance**: Load testing with realistic scenarios

### Mitigation Strategies
- Automated testing in CI/CD pipeline
- Security review by external team
- Performance testing in production-like environment
- Phased rollout with feature flags