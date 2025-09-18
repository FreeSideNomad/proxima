# Proxima - JWT Header Injection Reverse Proxy

**Stage 3**: Web UI with LCARS theme + Route Configuration

## Features

- ✅ **Star Trek LCARS-themed Web UI** for configuration management
- ✅ **Path-based route rewriting** (e.g., `/api/users/**` → `http://localhost:8081`)
- ✅ **Dynamic preset switching** via web interface and REST API
- ✅ **YAML-based configuration** with multiple header presets
- ✅ **Named presets** (admin_user, regular_user, api_client)
- ✅ **Configuration validation** with error reporting
- ✅ **Route testing** and diagnostics
- ✅ **Reverse proxy** functionality for all HTTP methods
- ✅ **Health monitoring** and system status
- ✅ **Docker support** with volume mapping
- ✅ **Comprehensive testing** (unit, integration, UI)

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

3. **Access LCARS Web UI**:
   ```bash
   # Open in browser
   open http://localhost:8080/ui
   ```

4. **Test route rewriting**:
   ```bash
   # Test user service route
   curl http://localhost:8080/proxy/api/users/123

   # Test invoice service route
   curl http://localhost:8080/proxy/api/invoices/456
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

**Basic Configuration:**
- `PROXIMA_DOWNSTREAM_URL`: Default downstream URL (default: http://httpbin.org)
- `PROXIMA_ACTIVE_PRESET`: Active preset name (default: admin_user)

**Header Presets:**
- `PROXIMA_ADMIN_AUTH`: Admin authorization token
- `PROXIMA_USER_AUTH`: Regular user authorization token
- `PROXIMA_API_AUTH`: API client authorization token

**Route Configuration:**
- `PROXIMA_USERS_SERVICE_URL`: User service URL (default: http://localhost:8081)
- `PROXIMA_INVOICES_SERVICE_URL`: Invoice service URL (default: http://localhost:8082)
- `PROXIMA_AUTH_SERVICE_URL`: Auth service URL (default: http://localhost:8083)

### Volume Configuration

Mount a config directory to `/app/config` with your custom `application.yml`:

```yaml
proxima:
  downstream:
    url: http://your-default-service.com
  active-preset: admin_user
  presets:
    - name: admin_user
      display-name: "Admin User"
      headers:
        Authorization: "Bearer admin-jwt-token"
        X-User-Role: "admin"
        X-User-ID: "admin-001"
  routes:
    - path-pattern: "/api/users/**"
      target-url: "http://user-service:8081"
      description: "User management service"
      enabled: true
    - path-pattern: "/api/invoices/**"
      target-url: "http://invoice-service:8082"
      description: "Invoice processing service"
      enabled: true
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

## Web Interface

### LCARS-Themed UI
Access the Star Trek LCARS-themed web interface at `http://localhost:8080/ui`:

- **Dashboard** - System overview and quick actions
- **Presets** - Manage and switch header presets
- **Headers** - View current header configuration
- **Routes** - Configure path-based routing
- **Status** - System health and diagnostics

### Route Testing
The web interface includes built-in route testing:
- Test path patterns and see resolved URLs
- Validate configuration changes
- Monitor system health

## API Endpoints

### Configuration Management
- `GET /api/config/presets` - List all header presets
- `GET /api/config/presets/{name}` - Get specific preset
- `GET /api/config/active-preset` - Get currently active preset
- `POST /api/config/presets/{name}/activate` - Switch active preset
- `GET /api/config/headers` - Get current headers being injected
- `GET /api/config/info` - Get configuration summary
- `GET /api/config/validate` - Validate current configuration

### Route Management
- `GET /api/config/routes` - List all route rules
- `GET /api/config/routes/test/{path}` - Test route resolution

## Next Stages

- **Stage 4**: JWT token generation utility with key management
- **Stage 5**: Advanced features (key rotation, metrics, authentication)