# OIDC Overview

## Introduction

OpenID Connect (OIDC) integration in Proxima enables full OAuth 2.0 authorization code flow, allowing the reverse proxy to act as both an OIDC provider and client. This implementation supports:

- **Authorization Code Flow**: Complete OAuth 2.0/OIDC specification compliance
- **Custom Claims**: Configurable user attributes and permissions
- **Multi-Client Support**: Multiple OIDC clients with different configurations
- **Token Management**: Automatic token generation, caching, and refresh
- **Discovery Endpoints**: Standard OIDC provider metadata and JWKS

## Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   OIDC Client   │───▶│  Proxima OIDC   │───▶│  Downstream     │
│                 │    │    Provider     │    │   Services      │
└─────────────────┘    └─────────────────┘    └─────────────────┘
        │                       │                       │
        │ 1. Authorization      │ 3. Token Injection    │
        │    Request            │    to Requests        │
        │                       │                       │
        │ 2. Access Token       │ 4. Proxied Request    │
        │    Response           │    with Headers       │
        └───────────────────────┼───────────────────────┘
                                │
                        ┌─────────────────┐
                        │   Token Cache   │
                        │   & Management  │
                        └─────────────────┘
```

## Key Components

### 1. Authorization Endpoint
- **Path**: `/oauth/authorize`
- **Purpose**: Initiates OIDC authorization flow
- **Parameters**: `response_type`, `client_id`, `redirect_uri`, `scope`, `state`, `nonce`

### 2. Token Endpoint
- **Path**: `/oauth/token`
- **Purpose**: Exchanges authorization code for tokens
- **Grant Type**: `authorization_code`

### 3. Discovery Endpoint
- **Path**: `/.well-known/openid_configuration`
- **Purpose**: Provides OIDC provider metadata
- **Standard**: OpenID Connect Discovery 1.0

### 4. JWKS Endpoint
- **Path**: `/oauth/jwks`
- **Purpose**: Provides public keys for token validation
- **Format**: JSON Web Key Set (JWKS)

## Benefits

### Security
- **State Parameter**: CSRF protection
- **Nonce Validation**: Replay attack prevention
- **Short-lived Codes**: Authorization codes expire quickly
- **Token Rotation**: Automatic token refresh capabilities

### Flexibility
- **Custom Claims**: Add organization-specific user attributes
- **Multiple Clients**: Support different applications with varying scopes
- **Configurable Expiration**: Adjust token lifetimes per use case
- **Algorithm Support**: RS256 for asymmetric key signing

### Integration
- **Seamless Proxying**: Automatic token injection into downstream requests
- **Standard Compliance**: Works with any OIDC-compliant client
- **Discovery Support**: Automatic client configuration via metadata
- **JWT Compatibility**: Tokens work with jwt.io and other validation tools

## Use Cases

### Development & Testing
- Mock OIDC provider for development environments
- Testing applications requiring OIDC authentication
- Prototype development with realistic auth flows

### API Gateway
- Centralized authentication for microservices
- Token validation and injection for downstream APIs
- Cross-cutting security concerns handling

### Legacy Integration
- Add OIDC support to applications without native support
- Bridge between legacy systems and modern auth
- Gradual migration to OIDC-based authentication

## Implementation Status

### ✅ Completed (Issues #20-22)
- Core OIDC models and configuration
- Basic token generation and caching
- Header preset OIDC integration

### 🚧 In Progress
- **Issue #28**: Authorization Code Flow implementation
- **Issue #29**: Discovery and JWKS endpoints
- **Issue #30**: UI for OIDC management
- **Issue #31**: Comprehensive testing suite
- **Issue #32**: Documentation and examples

## Getting Started

1. **Configure OIDC Preset**: Enable OIDC in header preset configuration
2. **Set Client Details**: Configure client ID, redirect URI, and scopes
3. **Generate Keys**: Create RSA key pairs for token signing
4. **Test Flow**: Use authorization endpoint to initiate flow
5. **Validate Tokens**: Verify tokens using discovery metadata

## Next Steps

- Review [OIDC Configuration Guide](OIDC-Configuration)
- Explore [Integration Examples](OIDC-Integration-Examples)
- Check [API Reference](OIDC-API-Reference)
- Follow [Testing Guide](OIDC-Testing)