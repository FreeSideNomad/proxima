# Proxima - JWT Header Injection Reverse Proxy

**Stage 4**: JWT Token Generation Utility with Key Management ✅

## 🚀 Quick Start with Docker

### Option 1: Simple Setup (Proxima + Your Backend)

```bash
# Create a config.json file
mkdir proxima-config
cat > proxima-config/config.json << 'EOF'
{
  "downstream": {
    "url": "http://your-backend-service:8081"
  },
  "activePreset": "default",
  "presets": [
    {
      "name": "default",
      "displayName": "Default Headers",
      "headers": {
        "Authorization": "Bearer your-jwt-token",
        "X-Forwarded-By": "Proxima-Proxy",
        "X-User-ID": "user-123"
      },
      "headerMappings": {}
    }
  ],
  "routes": [
    {
      "pathPattern": "/api/**",
      "targetUrl": "http://your-backend-service:8081",
      "description": "API routes",
      "enabled": true
    }
  ],
  "reservedRoutes": [
    "/proxima/**",
    "/actuator/**"
  ]
}
EOF

# Run Proxima
docker run -d \
  --name proxima \
  -p 8080:8080 \
  -v $(pwd)/proxima-config:/app/config \
  ghcr.io/freesidenomad/proxima:latest
```

### Option 2: Complete Demo with Docker Compose

```yaml
# docker-compose.yml
version: '3.8'
services:
  proxima:
    image: ghcr.io/freesidenomad/proxima:latest
    ports:
      - "8080:8080"
    volumes:
      - ./config:/app/config
    depends_on:
      - backend
    environment:
      - SPRING_PROFILES_ACTIVE=docker

  backend:
    image: nginx:alpine
    ports:
      - "8081:80"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf:ro

  # Optional: Database or other services
  # database:
  #   image: postgres:15
  #   environment:
  #     POSTGRES_DB: myapp
  #     POSTGRES_USER: user
  #     POSTGRES_PASSWORD: password
```

**Sample nginx.conf for demo backend:**
```nginx
events { worker_connections 1024; }
http {
    server {
        listen 80;
        location / {
            add_header Content-Type application/json;
            return 200 '{"message": "Hello from backend!", "headers": "$http_authorization", "user_id": "$http_x_user_id"}';
        }
    }
}
```

### Quick Test

```bash
# Start the demo
docker-compose up -d

# Test the proxy (should inject headers and forward to backend)
curl http://localhost:8080/api/test

# Access the LCARS Web UI
open http://localhost:8080/proxima/ui

# Generate JWT tokens
open http://localhost:8080/proxima/ui/jwt
```

## Features

- ✅ **Star Trek LCARS-themed Web UI** for configuration management
- ✅ **Partial path matching** (e.g., `/api/users` matches `/api/users/123`)
- ✅ **Advanced routing patterns** with wildcard support (`/**`, `/*`, `*`)
- ✅ **Header remapping** (e.g., `Authorization` → `Source-Authorization`)
- ✅ **Transparent HTTP error handling** (pass-through status codes)
- ✅ **Dynamic preset switching** via web interface and REST API
- ✅ **JSON-based configuration** with live editing and validation
- ✅ **Named presets** with custom headers and mappings
- ✅ **Configuration validation** with error reporting
- ✅ **Route testing** and diagnostics
- ✅ **Reverse proxy** functionality for all HTTP methods
- ✅ **Comprehensive request logging** with timing and status
- ✅ **Health monitoring** and system status
- ✅ **Docker support** with volume mapping
- ✅ **Reserved route protection** (prevents conflicts with admin/API routes)
- ✅ **Nginx downstream integration** for request inspection
- ✅ **Comprehensive testing** (unit, integration, UI)
- ✅ **JWT Token Generation** with HMAC (HS256) and RSA (RS256) algorithms
- ✅ **Cryptographic Key Management** with creation, deletion, and public key export
- ✅ **JWT Web Interface** with token generation, key management, and copy functionality
- ✅ **JWKS Discovery Endpoint** for public key distribution
- ✅ **Advanced JWT Features** with custom claims, expiration control, and key rotation

## Development & Local Setup

### Local Development

1. **Start the test nginx server** (for receiving proxied requests):
   ```bash
   docker-compose -f docker-compose.dev.yml up -d
   ```

2. **Run Proxima with Maven**:
   ```bash
   mvn spring-boot:run
   ```

3. **Test the proxy with nginx downstream**:
   ```bash
   # Any path that doesn't match reserved routes goes to downstream
   curl -X GET http://localhost:8080/test
   ```

4. **Access LCARS Web UI**:
   ```bash
   # Open in browser
   open http://localhost:8080/proxima/ui
   ```

5. **Test route configuration**:
   ```bash
   # Test partial matching routes (no /proxy prefix needed!)
   curl http://localhost:8080/api/users/123
   curl http://localhost:8080/api/companies/acme

   # Test legacy endpoints
   curl http://localhost:8080/v1/legacy/endpoint

   # Test custom routes (will route to nginx)
   curl http://localhost:8080/custom-path

   # Test header remapping (check nginx response)
   curl -H "Authorization: Bearer test-token" http://localhost:8080/test
   ```

6. **View nginx request logs**:
   ```bash
   # Nginx returns JSON with request details
   curl http://localhost:8081/
   ```

### Docker

1. **Build and run with Docker Compose**:
   ```bash
   docker-compose up --build
   ```

2. **Test the proxy**:
   ```bash
   curl -X GET http://localhost:8080/anything
   ```

## Configuration

### Environment-Aware Configuration

Proxima automatically detects whether it's running in Docker or local development and loads the appropriate configuration:

- **Docker Environment**: Uses `config.json` with Docker service names (e.g., `http://nginx:80`)
- **Local Development**: Uses `config-local.json` with localhost URLs (e.g., `http://localhost:8081`)

Environment detection checks for:
- Docker container indicators (`/.dockerenv`, `/proc/1/cgroup`)
- Docker environment variables (`DOCKER_CONTAINER=true`)
- Container hostname patterns

### JSON Configuration (config.json)

Proxima now uses a `config.json` file for dynamic configuration that can be edited via the web UI:

```json
{
  "downstream": {
    "url": "http://nginx:80"
  },
  "activePreset": "admin_user",
  "presets": [
    {
      "name": "admin_user",
      "displayName": "Admin User",
      "headers": {
        "Authorization": "Bearer admin-jwt-token",
        "X-User-Role": "admin",
        "X-User-ID": "admin-001",
        "X-Forwarded-By": "Proxima-Proxy"
      },
      "headerMappings": {
        "Authorization": "Source-Authorization",
        "User-Agent": "Source-User-Agent"
      }
    }
  ],
  "routes": [
    {
      "pathPattern": "/api/users",
      "targetUrl": "http://users-service:8081",
      "description": "User management service",
      "enabled": true
    },
    {
      "pathPattern": "/api/companies",
      "targetUrl": "http://companies-service:8082",
      "description": "Company management service",
      "enabled": true
    },
    {
      "pathPattern": "/v1/legacy",
      "targetUrl": "http://legacy-service:9000",
      "description": "Legacy API endpoints",
      "enabled": true
    }
  ],
  "reservedRoutes": [
    "/proxima/**",
    "/actuator/**"
  ]
}
```

### Local Development Configuration (config-local.json)

For local development, create a `config-local.json` with localhost URLs:

```json
{
  "downstream": {
    "url": "http://localhost:8081"
  },
  "activePreset": "admin_user",
  "presets": [
    {
      "name": "admin_user",
      "displayName": "Admin User",
      "headers": {
        "Authorization": "Bearer admin-jwt-token",
        "X-User-Role": "admin"
      }
    }
  ],
  "routes": [
    {
      "pathPattern": "/api/users",
      "targetUrl": "http://localhost:8082",
      "description": "User management service",
      "enabled": true
    }
  ],
  "reservedRoutes": [
    "/proxima/**",
    "/actuator/**"
  ]
}
```

### Configuration Options

#### Header Presets
Each preset supports:
- **headers**: Headers to inject into downstream requests
- **headerMappings**: Remapping of incoming headers (e.g., `Authorization` → `Source-Authorization`)

#### Route Patterns
Supports multiple pattern types:
- **Exact match**: `/api/users` (matches only `/api/users`)
- **Prefix match**: `/api/users` (also matches `/api/users/123`, `/api/users/123/profile`)
- **Single wildcard**: `/api/users/*` (matches `/api/users/123` but not `/api/users/123/profile`)
- **Multi-level wildcard**: `/api/users/**` (matches `/api/users/123/profile/settings`)
- **Pattern wildcard**: `/api/*/details` (matches `/api/users/details`, `/api/companies/details`)

#### Routing Logic
Routes are processed in order:
1. First matching route wins
2. Disabled routes are skipped
3. Reserved routes are blocked
4. Unmatched routes go to default downstream

### Reserved Routes

The following routes are reserved for Proxima's admin interface and cannot be used in custom routing:
- `/proxima/**` - All Proxima API and UI endpoints
  - `/proxima/api/**` - Configuration API endpoints
  - `/proxima/ui/**` - Web UI interface (dashboard, presets, routes, etc.)
- `/actuator/**` - Spring Boot actuator endpoints

**Important**: Only `/proxima/**` and `/actuator/**` are reserved. All other routes including `/api/**`, `/ui/**`, `/dashboard/**` etc. are now available for your services!

All non-reserved routes will be forwarded to the configured downstream service (nginx by default).

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
- **JWT Tokens** - Generate tokens and manage cryptographic keys
- **Status** - System health and diagnostics

### Route Testing
The web interface includes built-in route testing:
- Test path patterns and see resolved URLs
- Validate configuration changes
- Monitor system health

## API Endpoints

### Configuration Management
- `GET /proxima/api/config` - Get complete configuration
- `POST /proxima/api/config` - Update complete configuration (with validation)
- `POST /proxima/api/config/active-preset` - Set active preset
- `GET /proxima/api/config/reserved-routes` - Get list of reserved routes

### Route Management
- `POST /proxima/api/config/routes` - Add new route (with validation)
- `DELETE /proxima/api/config/routes/{index}` - Delete route by index

### Preset Management
- `POST /proxima/api/config/presets` - Add new preset
- `DELETE /proxima/api/config/presets/{index}` - Delete preset by index

### JWT Management
- `POST /proxima/api/jwt/tokens` - Generate JWT tokens with custom claims
- `POST /proxima/api/jwt/keys/hmac` - Generate HMAC symmetric keys (HS256)
- `POST /proxima/api/jwt/keys/rsa` - Generate RSA key pairs (RS256)
- `GET /proxima/api/jwt/keys` - List all cryptographic keys
- `GET /proxima/api/jwt/keys/{keyId}/public` - Export RSA public key
- `DELETE /proxima/api/jwt/keys/{keyId}` - Delete cryptographic key
- `GET /proxima/api/jwt/.well-known/jwks.json` - JWKS discovery endpoint

### Legacy Endpoints (for backward compatibility)
- `GET /proxima/api/config/presets` - List all header presets
- `GET /proxima/api/config/presets/{name}` - Get specific preset
- `POST /proxima/api/config/presets/{name}/activate` - Switch active preset

### Testing and Development
Use the nginx test server to see forwarded requests and header remapping:
```bash
# Start nginx test server
docker-compose -f docker-compose.dev.yml up -d

# Test route matching (no /proxy prefix needed!)
curl http://localhost:8080/api/users/123        # Routes to users-service
curl http://localhost:8080/api/companies/acme  # Routes to companies-service
curl http://localhost:8080/v1/legacy/endpoint  # Routes to legacy-service
curl http://localhost:8080/my-custom-route     # Routes to nginx

# Test header remapping (with admin_user preset)
curl -H "Authorization: Bearer test" http://localhost:8080/test

# Check what nginx received (shows remapped headers)
curl http://localhost:8081/
# Look for "Source-Authorization" instead of "Authorization"
```

### Request Logging
Proxima logs all requests with detailed information:
```
INFO  - PROXY: GET /api/users/123 from 127.0.0.1 -> http://users-service:8081/123 (headers: admin_user)
INFO  - PROXY SUCCESS: GET /api/users/123 from 127.0.0.1 -> http://users-service:8081/123 completed in 45ms (status: 200)
INFO  - BLOCKED: GET /proxima/api/config from 127.0.0.1 - Reserved route
```

## Development Roadmap

- ✅ **Stage 1**: Basic reverse proxy with static header injection
- ✅ **Stage 2**: Configuration file support with multiple header presets
- ✅ **Stage 3**: Web UI with LCARS theme + Route Configuration
- ✅ **Stage 4**: JWT token generation utility with key management
- 🔄 **Stage 5**: Advanced features (key rotation, metrics, authentication)