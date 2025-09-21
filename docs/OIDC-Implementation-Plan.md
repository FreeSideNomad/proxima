# OIDC Implementation Plan for Proxima

## Overview

This document outlines the comprehensive plan for implementing OpenID Connect (OIDC) authentication support in Proxima. The implementation follows Test-Driven Development (TDD) principles and provides a proxy that automatically handles OIDC flows while injecting configured user tokens.

## Architecture Goals

### Core Concept
Proxima will act as an OIDC-compliant authentication proxy that:
- Handles OIDC Authorization Code flow redirects
- Automatically "authenticates" users by returning pre-configured default tokens
- Injects these tokens as Authorization Bearer headers in proxied requests
- Supports multiple user presets with different subjects and claims
- Caches tokens with configurable expiration (default: 3600 seconds)

### Key Features
- **OIDC Authorization Code Flow**: Full compliance with OIDC spec
- **Automatic Authentication**: No login forms - immediate token return
- **Configurable User Presets**: Multiple user configurations with different claims
- **Token Caching**: Efficient token lifecycle management
- **Bearer Token Injection**: Seamless integration with existing proxy functionality
- **Discovery Endpoints**: Standard OIDC discovery and JWKS endpoints

## Technical Architecture

### Current Proxima Components (Leveraged)
- **HeaderPreset System**: Extended with OIDC-specific properties
- **JwtService**: Enhanced to support OIDC token generation
- **Proxy Filter**: Modified to handle OIDC flows and token injection
- **Configuration Management**: Extended with OIDC settings

### New Components (To Be Implemented)
- **OidcConfiguration**: OIDC-specific configuration model
- **OidcTokenService**: Specialized token generation and caching
- **OidcController**: OIDC endpoints (auth, token, discovery, jwks)
- **TokenCache**: Efficient token storage and lifecycle management

## Implementation Phases

### Phase 1: Core Models and Configuration (Issues #20-21)
**Files to Modify/Create:**
- `src/main/java/com/freesidenomad/proxima/model/OidcConfiguration.java`
- `src/main/java/com/freesidenomad/proxima/model/HeaderPreset.java` (enhanced)
- `config.json.example` (enhanced with OIDC examples)

**Key Changes:**
- Add OIDC-specific properties to HeaderPreset (subject, claims, tokenExpiry)
- Create comprehensive OIDC configuration model
- Update configuration examples

### Phase 2: Token Service Implementation (Issue #22)
**Files to Create:**
- `src/main/java/com/freesidenomad/proxima/service/OidcTokenService.java`
- Integration with existing `JwtService.java`

**Key Features:**
- ID token and access token generation
- Integration with HeaderPreset configurations
- Support for configurable expiration times
- Claims population from preset configurations

### Phase 3: OIDC Endpoints (Issues #23-24)
**Files to Create:**
- `src/main/java/com/freesidenomad/proxima/controller/OidcController.java`

**Endpoints to Implement:**
- `/oauth2/authorize` - Authorization endpoint
- `/oauth2/token` - Token endpoint
- `/.well-known/openid-configuration` - Discovery endpoint
- `/.well-known/jwks.json` - JWKS endpoint

### Phase 4: Token Caching and Lifecycle (Issue #25)
**Files to Create:**
- `src/main/java/com/freesidenomad/proxima/service/TokenCache.java`
- `src/main/java/com/freesidenomad/proxima/service/TokenLifecycleService.java`

**Key Features:**
- Efficient in-memory token storage
- Automatic token refresh and expiration
- Startup token generation
- Preset switching with cache invalidation

### Phase 5: Integration and E2E Testing (Issue #26)
**Files to Create:**
- `src/test/java/com/freesidenomad/proxima/integration/OidcFlowIntegrationTest.java`
- `playwright-tests/oidc-flow.spec.js`

**Test Coverage:**
- Complete OIDC flow testing
- Multi-preset scenarios
- Token caching validation
- Real infrastructure testing with Playwright

## Configuration Examples

### Enhanced HeaderPreset with OIDC Support
```json
{
  "name": "admin",
  "displayName": "Admin User",
  "oidcSubject": "admin-001",
  "oidcClaims": {
    "preferred_username": "admin",
    "email": "admin@example.com",
    "role": "administrator",
    "department": "IT"
  },
  "tokenExpiry": 7200,
  "headers": {
    "X-User-Role": "admin",
    "X-User-ID": "admin-001"
  },
  "headerMappings": {}
}
```

### OIDC Configuration
```json
{
  "oidc": {
    "issuer": "http://localhost:8080",
    "clientId": "proxima-client",
    "defaultExpiry": 3600,
    "supportedScopes": ["openid", "profile", "email"],
    "supportedGrantTypes": ["authorization_code"],
    "supportedResponseTypes": ["code"]
  }
}
```

## Testing Strategy

### Test-Driven Development Approach
1. **Unit Tests First**: Comprehensive unit tests for all components
2. **Integration Tests**: Multi-component integration validation
3. **E2E Tests**: Full OIDC flow testing with Playwright
4. **Regression Test Suite**: Comprehensive test bed for ongoing validation

### Test Categories
- **Unit Tests**: Token generation, caching, configuration validation
- **Integration Tests**: OIDC flow components working together
- **E2E Tests**: Full browser-based OIDC flow testing
- **Performance Tests**: Token caching and generation performance
- **Security Tests**: Token validation and expiration handling

## Implementation Timeline

### Sprint 1: Foundation (Issues #20-21)
- Core models and configuration
- Enhanced HeaderPreset with OIDC support
- Unit tests for configuration validation

### Sprint 2: Token Services (Issue #22)
- OidcTokenService implementation
- Integration with existing JwtService
- Comprehensive unit tests for token generation

### Sprint 3: OIDC Endpoints (Issues #23-24)
- OIDC controller with all required endpoints
- Discovery and JWKS endpoint implementation
- Unit and integration tests for endpoints

### Sprint 4: Caching and Lifecycle (Issue #25)
- Token caching implementation
- Lifecycle management service
- Performance and reliability tests

### Sprint 5: E2E Testing (Issue #26)
- Playwright test suite development
- Full OIDC flow validation
- Multi-scenario testing

### Sprint 6: Documentation and Examples (Issue #27)
- Comprehensive documentation
- Configuration examples
- Developer and user guides

## Success Criteria

### Functional Requirements
- ✅ Complete OIDC Authorization Code flow support
- ✅ Automatic user authentication with preset tokens
- ✅ Configurable user presets with custom claims
- ✅ Token caching with configurable expiration
- ✅ Bearer token injection in proxy headers
- ✅ Standard OIDC discovery endpoints

### Technical Requirements
- ✅ 100% unit test coverage for new components
- ✅ Integration tests for all OIDC flows
- ✅ E2E tests using Playwright
- ✅ Performance benchmarks for token operations
- ✅ Security validation for token handling

### Documentation Requirements
- ✅ Comprehensive API documentation
- ✅ Configuration guides and examples
- ✅ Developer setup and testing guides
- ✅ User guides for OIDC integration

## Related GitHub Issues

- **Epic Issue**: [#19 OIDC Authentication Support Epic](https://github.com/FreeSideNomad/proxima/issues/19)
- **OIDC-1**: [#20 Core OIDC Models and Configuration](https://github.com/FreeSideNomad/proxima/issues/20)
- **OIDC-2**: [#21 Enhanced HeaderPreset with OIDC Support](https://github.com/FreeSideNomad/proxima/issues/21)
- **OIDC-3**: [#22 OIDC Token Service Implementation](https://github.com/FreeSideNomad/proxima/issues/22)
- **OIDC-4**: [#23 OIDC Controller and Authorization Endpoints](https://github.com/FreeSideNomad/proxima/issues/23)
- **OIDC-5**: [#24 OIDC Discovery and JWKS Endpoints](https://github.com/FreeSideNomad/proxima/issues/24)
- **OIDC-6**: [#25 Token Caching and Lifecycle Management](https://github.com/FreeSideNomad/proxima/issues/25)
- **OIDC-7**: [#26 E2E Integration Tests with Playwright](https://github.com/FreeSideNomad/proxima/issues/26)
- **OIDC-8**: [#27 Documentation and Examples](https://github.com/FreeSideNomad/proxima/issues/27)

## Next Steps

1. **Start with Issue #20**: Core OIDC Models and Configuration
2. **Follow TDD Approach**: Write failing tests first, then implement
3. **Regular Integration**: Ensure each component integrates properly
4. **Continuous Testing**: Run full test suite after each implementation
5. **Documentation Updates**: Keep documentation current with implementation

---

*This document serves as the master plan for OIDC implementation in Proxima. For detailed technical requirements and implementation guidelines, refer to the individual GitHub issues linked above.*