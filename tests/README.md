# Test Infrastructure for Proxima OIDC Implementation

This directory contains the test infrastructure set up for comprehensive testing of OIDC features in Proxima.

## Test Structure

```
tests/
├── e2e/                      # End-to-end tests using Playwright
│   ├── fixtures/            # Test data and configuration fixtures
│   ├── global-setup.js      # Global test setup (runs once before all tests)
│   ├── global-teardown.js   # Global test cleanup (runs once after all tests)
│   └── oidc-flow.spec.js    # OIDC authentication flow E2E tests
└── README.md               # This file
```

## Test Categories

### 1. Unit Tests (Maven/Spring Boot)
- Located in `src/test/java/`
- Test individual components, services, and models
- Use Spring Boot Test framework
- Coverage tracked with JaCoCo

### 2. Integration Tests (Maven/Spring Boot)
- Located in `src/test/java/` with `*IntegrationTest.java` suffix
- Test component interactions and Spring context
- Use TestContainers for external dependencies when needed

### 3. End-to-End Tests (Playwright)
- Located in `tests/e2e/`
- Test complete OIDC flows in a real browser environment
- Validate full authentication flows, token exchange, and API integration

## Running Tests

### Unit Tests
```bash
mvn test -Dtest="!*IntegrationTest"
```

### Integration Tests
```bash
mvn test -Dtest="*IntegrationTest"
```

### E2E Tests
```bash
# Install dependencies first
npm install

# Install browser binaries
npx playwright install

# Run all E2E tests
npm test

# Run with UI (for debugging)
npm run test:ui

# Run in headed mode (see browser)
npm run test:headed

# Debug specific test
npm run test:debug
```

### All Tests (CI)
```bash
# This runs the complete test suite as configured in GitHub Actions
mvn test && npm test
```

## Test Configuration

### Spring Boot Test Configuration
- **Test Profile**: `application-test.yml` with test-specific settings
- **Test Configuration**: `test-config.json` with OIDC test data
- **Test Presets**: Configured test user presets (default, admin, api_client)

### Playwright Configuration
- **Config File**: `playwright.config.js`
- **Base URL**: `http://localhost:8080`
- **Browsers**: Chromium, Firefox, WebKit
- **Reporters**: HTML report + JUnit XML for CI
- **Artifacts**: Screenshots, videos, traces on failure

## OIDC Test Scenarios

The E2E tests cover the following OIDC scenarios:

1. **Discovery Endpoint** - Validates OIDC discovery document
2. **Authorization Code Flow** - Tests complete OAuth2/OIDC flow
3. **Token Exchange** - Validates authorization code to token exchange
4. **JWT Token Structure** - Verifies token format and claims
5. **Multiple User Presets** - Tests different user configurations
6. **JWKS Endpoint** - Validates public key distribution
7. **Token Caching** - Tests performance and caching behavior
8. **Token Expiration** - Validates token lifecycle management

## Test Data Management

### Test User Presets
- **default**: Standard user with basic claims
- **admin**: Administrative user with elevated permissions
- **api_client**: Service account for API access

### Test Configuration
Test configurations are isolated from production and include:
- Shorter token expiration times for faster testing
- Test-specific OIDC client configurations
- Mock downstream services on different ports

## Continuous Integration

Tests are integrated into GitHub Actions workflow:
- **Unit Tests**: Run on every PR and push
- **Integration Tests**: Run after unit tests pass
- **E2E Tests**: Run after integration tests pass
- **Build/Package**: Only runs if all tests pass

### Artifacts
- Test reports (HTML + XML)
- Coverage reports (JaCoCo)
- E2E test videos and screenshots
- Playwright traces for debugging

## Local Development

### Running Tests During Development
```bash
# Watch mode for specific test
npx playwright test oidc-flow.spec.js --headed

# Run tests against local development server
mvn spring-boot:run -Dspring.profiles.active=test &
npm test
```

### Debugging E2E Tests
```bash
# Debug mode with browser DevTools
npm run test:debug

# Record new tests
npx playwright codegen http://localhost:8080
```

## Future Enhancements

1. **Performance Testing** - Load testing for OIDC endpoints
2. **Security Testing** - Penetration testing for authentication flows
3. **Cross-browser Testing** - Extended browser matrix
4. **Mobile Testing** - Testing on mobile viewports
5. **API Testing** - Direct API endpoint testing with REST Assured

## Test-Driven Development Workflow

For OIDC implementation, follow this TDD approach:

1. **Write failing tests first** - Create tests for new OIDC features
2. **Run tests to confirm failure** - Ensure tests fail as expected
3. **Implement minimal code** - Write just enough code to pass tests
4. **Run tests to confirm pass** - Verify implementation works
5. **Refactor and optimize** - Improve code while keeping tests green
6. **Add more tests** - Expand coverage and edge cases

This ensures comprehensive test coverage and reliable OIDC implementation.