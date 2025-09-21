# OIDC Configuration Guide

## Overview

This guide covers how to configure OpenID Connect (OIDC) authentication in Proxima. OIDC configuration is done through preset configuration files and the web UI.

## Configuration Structure

### Basic OIDC Preset Configuration

```json
{
  "presets": [
    {
      "name": "oidc_preset",
      "displayName": "OIDC Authentication",
      "headers": {},
      "oidcConfig": {
        "enabled": true,
        "subject": "user@example.com",
        "tokenExpirationSeconds": 3600,
        "algorithm": "RS256",
        "keyId": "default",
        "email": "user@example.com",
        "name": "Test User",
        "preferredUsername": "testuser",
        "groups": ["admin", "users"],
        "customClaims": {
          "department": "IT",
          "role": "admin",
          "permissions": ["read", "write", "admin"]
        },
        "scopes": ["openid", "profile", "email"],
        "clientId": "my-client-app",
        "redirectUri": "http://localhost:8080/callback"
      }
    }
  ]
}
```

## Configuration Parameters

### Core OIDC Settings

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `enabled` | boolean | Yes | false | Enables OIDC for this preset |
| `subject` | string | Yes | - | JWT subject claim (sub) |
| `tokenExpirationSeconds` | number | No | 3600 | Token expiration time |
| `algorithm` | string | No | "RS256" | Token signing algorithm |
| `keyId` | string | No | "default" | Key ID for token signing |

### User Claims

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `email` | string | No | User email address |
| `name` | string | No | User full name |
| `preferredUsername` | string | No | Preferred username |
| `groups` | array | No | User groups/roles |
| `customClaims` | object | No | Additional custom claims |

### Client Configuration

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `clientId` | string | Yes | OIDC client identifier |
| `redirectUri` | string | Yes | Callback URL for authorization flow |
| `scopes` | array | No | Requested scopes (default: ["openid", "profile", "email"]) |

## Docker Configuration

### Docker Compose Setup

```yaml
version: '3.8'
services:
  proxima:
    image: proxima:latest
    ports:
      - "8080:8080"
    volumes:
      - ./config-local.json:/app/config-local.json
    environment:
      - SPRING_CONFIG_LOCATION=file:/app/config-local.json
      - PROXIMA_OIDC_ISSUER=http://localhost:8080
```

### Environment Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `PROXIMA_OIDC_ISSUER` | OIDC issuer URL | `http://localhost:8080` |
| `PROXIMA_OIDC_KEY_STORE_PATH` | Path to key store | `/app/keys` |
| `PROXIMA_OIDC_DEFAULT_EXPIRATION` | Default token expiration | `3600` |

## Multi-Client Configuration

```json
{
  "presets": [
    {
      "name": "client_a",
      "displayName": "Client A - Web App",
      "oidcConfig": {
        "enabled": true,
        "subject": "webapp@example.com",
        "clientId": "web-app-client",
        "redirectUri": "http://localhost:3000/callback",
        "scopes": ["openid", "profile", "email"],
        "customClaims": {
          "app_type": "web",
          "permissions": ["read", "write"]
        }
      }
    },
    {
      "name": "client_b",
      "displayName": "Client B - Mobile App",
      "oidcConfig": {
        "enabled": true,
        "subject": "mobile@example.com",
        "clientId": "mobile-app-client",
        "redirectUri": "myapp://callback",
        "scopes": ["openid", "profile"],
        "tokenExpirationSeconds": 7200,
        "customClaims": {
          "app_type": "mobile",
          "permissions": ["read"]
        }
      }
    }
  ]
}
```

## Advanced Configuration

### Custom Claims Examples

```json
{
  "customClaims": {
    "organization": "ACME Corp",
    "department": "Engineering",
    "team": "Platform",
    "level": "senior",
    "permissions": [
      "api:read",
      "api:write",
      "admin:users"
    ],
    "features": {
      "beta_access": true,
      "advanced_ui": true
    },
    "metadata": {
      "created_at": "2024-01-01T00:00:00Z",
      "last_login": "2024-12-01T10:30:00Z"
    }
  }
}
```

### Security Configuration

```json
{
  "oidcConfig": {
    "enabled": true,
    "subject": "secure-user@example.com",
    "tokenExpirationSeconds": 1800,
    "algorithm": "RS256",
    "keyId": "production-key-2024",
    "customClaims": {
      "aud": ["api.example.com", "admin.example.com"],
      "iss": "https://auth.example.com",
      "security_level": "high",
      "mfa_required": true
    }
  }
}
```

## Key Management

### RSA Key Generation

1. **Via UI**: Navigate to JWT Management → Generate RSA Key Pair
2. **Via API**:
   ```bash
   curl -X POST http://localhost:8080/proxima/api/jwt/keys/rsa \
     -H "Content-Type: application/json" \
     -d '{"keyId": "my-production-key"}'
   ```

### Key Rotation

```json
{
  "oidcConfig": {
    "keyId": "production-key-2024-q1",
    "algorithm": "RS256"
  }
}
```

## Configuration Validation

### Validation Checklist

- [ ] OIDC is enabled (`enabled: true`)
- [ ] Subject is configured
- [ ] Client ID is set
- [ ] Redirect URI is valid
- [ ] RSA key exists for specified keyId
- [ ] Custom claims are valid JSON
- [ ] Token expiration is reasonable

### Common Issues

#### Invalid Redirect URI
```json
{
  "error": "invalid_redirect_uri",
  "error_description": "The redirect_uri must be registered for this client"
}
```

**Solution**: Ensure redirect URI matches exactly (including protocol, host, port, and path).

#### Missing RSA Key
```json
{
  "error": "key_not_found",
  "error_description": "RSA key 'my-key' not found"
}
```

**Solution**: Generate the RSA key pair or use existing keyId.

#### Invalid Custom Claims
```json
{
  "error": "invalid_claims",
  "error_description": "Custom claims must be valid JSON"
}
```

**Solution**: Validate JSON format of customClaims object.

## Testing Configuration

### Manual Testing
1. Navigate to `/proxima/ui/jwt`
2. Generate test token with OIDC preset
3. Verify token at [jwt.io](https://jwt.io)
4. Test authorization flow

### API Testing
```bash
# Get OIDC discovery metadata
curl http://localhost:8080/.well-known/openid_configuration

# Get JWKS for token validation
curl http://localhost:8080/oauth/jwks

# Start authorization flow
curl "http://localhost:8080/oauth/authorize?response_type=code&client_id=my-client&redirect_uri=http://localhost:8080/callback&scope=openid profile&state=xyz123"
```

## Troubleshooting

### Common Problems

1. **Token not injected into requests**
   - Check if preset is activated
   - Verify OIDC is enabled in preset
   - Ensure token is not expired

2. **Authorization flow fails**
   - Verify client ID and redirect URI
   - Check browser developer console for errors
   - Validate discovery endpoint response

3. **Token validation fails**
   - Confirm RSA key is properly generated
   - Check issuer URL configuration
   - Verify JWKS endpoint accessibility

### Debug Mode

Enable debug logging by adding to configuration:
```json
{
  "logging": {
    "level": {
      "com.freesidenomad.proxima.service": "DEBUG"
    }
  }
}
```

## Best Practices

### Security
- Use short token expiration times (≤ 1 hour)
- Rotate keys regularly
- Validate redirect URIs strictly
- Use HTTPS in production

### Performance
- Cache tokens appropriately
- Use reasonable custom claims size
- Monitor token generation performance

### Maintenance
- Document client configurations
- Version control configuration files
- Test configuration changes thoroughly