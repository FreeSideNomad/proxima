# Proxima - JWT Header Injection Reverse Proxy

**Stage 1 MVP**: Basic reverse proxy with static header injection

## Features

- ✅ Reverse proxy functionality for all HTTP methods
- ✅ Static header injection (configurable via YAML/environment variables)
- ✅ Health check endpoints
- ✅ Docker support with volume mapping
- ✅ Unit and integration tests

## Quick Start

### Local Development

1. **Run with Maven**:
   ```bash
   mvn spring-boot:run
   ```

2. **Test the proxy**:
   ```bash
   curl -X GET http://localhost:8080/proxy/anything
   ```

### Docker

1. **Build and run with Docker Compose**:
   ```bash
   docker-compose up --build
   ```

2. **Test the proxy**:
   ```bash
   curl -X GET http://localhost:8080/proxy/anything
   ```

## Configuration

### Environment Variables

- `PROXIMA_DOWNSTREAM_URL`: Target server URL (default: http://httpbin.org)
- `PROXIMA_HEADERS_AUTHORIZATION`: Authorization header value
- `PROXIMA_HEADERS_X_USER_ROLE`: Custom user role header

### Volume Configuration

Mount a config directory to `/app/config` with your custom `application.yml`:

```yaml
proxima:
  downstream:
    url: http://your-api-server.com
  headers:
    authorization: "Bearer your-jwt-token"
    x-user-role: "admin"
    x-custom-header: "custom-value"
```

## Testing

Run tests:
```bash
mvn test
```

## Health Check

Check application health:
```bash
curl http://localhost:8080/actuator/health
```

## Architecture

```
Client → Proxima Proxy → Downstream Service
         (injects headers)
```

## Next Stages

- Stage 2: Configuration file support with multiple presets
- Stage 3: Web UI for configuration management
- Stage 4: JWT token generation utility
- Stage 5: Advanced features (key rotation, metrics)