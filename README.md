# Proxima - JWT Header Injection Reverse Proxy

**Stage 2**: Configuration file support with multiple header presets

## Features

- ✅ Reverse proxy functionality for all HTTP methods
- ✅ YAML-based configuration with multiple header presets
- ✅ Named presets (admin_user, regular_user, api_client)
- ✅ Dynamic preset switching via REST API
- ✅ Configuration validation
- ✅ Health check endpoints
- ✅ Docker support with volume mapping
- ✅ Comprehensive unit and integration tests

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

3. **Switch presets via API**:
   ```bash
   # List all presets
   curl http://localhost:8080/api/config/presets

   # Get current active preset
   curl http://localhost:8080/api/config/active-preset

   # Switch to regular user preset
   curl -X POST http://localhost:8080/api/config/presets/regular_user/activate
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
- `PROXIMA_ACTIVE_PRESET`: Active preset name (default: admin_user)
- `PROXIMA_ADMIN_AUTH`: Admin authorization token
- `PROXIMA_USER_AUTH`: Regular user authorization token
- `PROXIMA_API_AUTH`: API client authorization token

### Volume Configuration

Mount a config directory to `/app/config` with your custom `application.yml`:

```yaml
proxima:
  downstream:
    url: http://your-api-server.com
  active-preset: admin_user
  presets:
    - name: admin_user
      display-name: "Admin User"
      headers:
        Authorization: "Bearer admin-jwt-token"
        X-User-Role: "admin"
        X-User-ID: "admin-001"
    - name: regular_user
      display-name: "Regular User"
      headers:
        Authorization: "Bearer user-jwt-token"
        X-User-Role: "user"
        X-User-ID: "user-001"
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

## API Endpoints

### Configuration Management
- `GET /api/config/presets` - List all header presets
- `GET /api/config/presets/{name}` - Get specific preset
- `GET /api/config/active-preset` - Get currently active preset
- `POST /api/config/presets/{name}/activate` - Switch active preset
- `GET /api/config/headers` - Get current headers being injected
- `GET /api/config/info` - Get configuration summary
- `GET /api/config/validate` - Validate current configuration

## Next Stages

- Stage 3: Web UI for configuration management (LCARS theme)
- Stage 4: JWT token generation utility
- Stage 5: Advanced features (key rotation, metrics)