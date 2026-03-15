# Getting Started — Quantum Auth Framework

This guide walks you through integrating the Quantum Auth Framework into a new or existing application. The framework provides entropy-backed JWTs sourced from a quantum random-number generator (or a CSPRNG fallback), with optional IP binding, honey-token traps, and quantum watermarking.

---

## Prerequisites

| Requirement | Version |
|---|---|
| Java (for the Spring Boot starter) | 17 + |
| Maven | 3.8 + |
| Docker / Docker Compose | 24 + |
| Redis | 7.2 (provided via Docker Compose) |
| Python | 3.9 + (for the entropy miner) |

---

## Step 1 — Build and Install the Starter Library

The starter is not yet published to Maven Central, so you must install it to your local repository first.

```bash
# From the repo root
cd java/quantum-jwt-starter
mvn install -DskipTests
```

This installs `com.quantum:quantum-jwt-starter:1.0.0-SNAPSHOT` into `~/.m2`.

---

## Step 2 — Add the Dependency

Add the starter and the Redis client to your Spring Boot project's `pom.xml`:

```xml
<!-- Quantum JWT Starter -->
<dependency>
    <groupId>com.quantum</groupId>
    <artifactId>quantum-jwt-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>

<!-- Spring Data Redis (required when redis-enabled=true) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

If you want the automatic Bearer-token security filter, also add:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

---

## Step 3 — Start the Infrastructure

A complete Docker Compose stack (Redis, entropy miner, Prometheus, Grafana) is provided in `infrastructure/`.

```bash
# From the repo root
cd infrastructure
docker compose up -d
```

| Service | Default address |
|---|---|
| Redis | `localhost:6379` |
| Prometheus | `http://localhost:9090` |
| Grafana | `http://localhost:3001` |

> **Redis ACL note** — the provided `infrastructure/redis/users.acl` defines role-scoped users. Your application should authenticate as the `app` user (password `app-password`). See [infrastructure/redis/users.acl](../infrastructure/redis/users.acl) for the full ACL definition.

---

## Step 4 — Configure Your Application

Add the following to your `application.yml` (or `application.properties`):

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password: app-password   # matches the 'app' user in users.acl

quantum:
  jwt:
    # Required
    issuer: my-service          # default: "quantum-auth-service"

    # Token lifetime (ISO-8601 duration)
    token-ttl: PT15M            # default: 15 minutes

    # IP binding — ties the token to the client's IP address
    ip-binding-enabled: true    # default: true

    # Quantum watermark claim embedded in every token
    watermark-enabled: true     # default: true
    watermark-secret: change-me-in-production  # default: "quantum-default-watermark-secret"

    # Redis entropy pool (set false to use local CSPRNG instead)
    redis-enabled: true         # default: true

    # IBM Quantum backend (optional — omit to use CSPRNG fallback)
    # ibm-quantum-token: YOUR_IBM_TOKEN
    # ibm-backend-name: ibm_sherbrooke   # default: "ibmq_qasm_simulator"
```

### Local / offline mode

Set `redis-enabled: false` to skip Redis entirely. The starter will use a local in-memory entropy pool backed by `java.security.SecureRandom`. No miner is needed in this mode.

---

## Step 5 — Run the Entropy Miner

The miner is a Python daemon that continuously fills the Redis entropy pool with quantum (or CSPRNG) randomness. It must be running before your application boots in `redis-enabled: true` mode.

### With Docker Compose (recommended)

The miner is already included in `infrastructure/docker-compose.yml` and starts automatically with `docker compose up`.

### Running manually

```bash
cd quantum-miner

# Create the environment file
cat > .env <<'EOF'
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=miner-password   # matches the 'miner' user in users.acl
MIN_POOL_SIZE=50
MAX_POOL_SIZE=500
BATCH_SIZE=10
KEY_SIZE_BYTES=32
# IBM_QUANTUM_TOKEN=YOUR_TOKEN  # omit to use CSPRNG fallback
# IBM_BACKEND_NAME=ibm_sherbrooke
EOF

pip install -r requirements.txt
python miner.py
```

The miner will log each batch it pushes to Redis and will fall back to `secrets.token_bytes()` automatically if the IBM Quantum service is unavailable or no token is configured.

---

## Step 6 — Issue and Validate Tokens

`QuantumJwtService` is auto-configured as a Spring bean. Inject it wherever you need to issue or verify tokens.

### Injecting the service

```java
import com.quantum.jwt.core.QuantumJwtService;
import org.springframework.web.bind.annotation.*;
import io.jsonwebtoken.Claims;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final QuantumJwtService jwtService;

    public AuthController(QuantumJwtService jwtService) {
        this.jwtService = jwtService;
    }
    // ...
}
```

### Generating a token

`generateToken` takes three arguments: the subject (e.g. username), a map of additional claims to embed, and the client IP address.

```java
@PostMapping("/token")
public ResponseEntity<Map<String, String>> login(
        @RequestParam String username,
        HttpServletRequest request) {

    String clientIp = request.getRemoteAddr();

    // With no extra claims:
    String token = jwtService.generateToken(username, new HashMap<>(), clientIp);

    // With extra claims (e.g. role, tenant):
    Map<String, Object> claims = Map.of("role", "admin", "tenantId", "acme-corp");
    String token = jwtService.generateToken(username, claims, clientIp);

    return ResponseEntity.ok(Map.of("token", token));
}
```

### Validating a token

```java
import io.jsonwebtoken.Claims;
import com.quantum.jwt.exceptions.QuantumJwtException;

@GetMapping("/me")
public ResponseEntity<String> profile(
        @RequestHeader("Authorization") String bearerHeader,
        HttpServletRequest request) {

    String token = bearerHeader.replace("Bearer ", "");
    String clientIp = request.getRemoteAddr();

    try {
        Claims payload = jwtService.validateToken(token, clientIp);
        String subject = payload.getSubject();
        return ResponseEntity.ok("Hello, " + subject);
    } catch (QuantumJwtException e) {
        return ResponseEntity.status(401).body("Invalid token: " + e.getMessage());
    }
}
```

`validateToken` raises `QuantumJwtException` (a `RuntimeException`) on any failure: expired token, IP mismatch, poisoned key, or tampered signature.

### Generating a honey token (decoy / trap)

Honey tokens look like real tokens but are flagged internally. Any attempt to use one triggers an alert.

```java
String honeyToken = jwtService.generateHoneyToken("decoy_user", new HashMap<>(), clientIp);
```

### Using the automatic security filter

If `spring-boot-starter-security` is on the classpath, `QuantumAuthenticationFilter` is registered automatically. It intercepts every request, extracts the `Authorization: Bearer <token>` header, calls `validateToken`, and populates the `SecurityContext`. No additional code is needed in your controllers.

---

## SDK Quick Reference

The framework ships identical functionality for three additional runtimes.

### Python

```python
from quantum_jwt import QuantumJwtClient

client = QuantumJwtClient(
    redis_host="localhost",
    redis_port=6379,
    redis_password="app-password",
    token_ttl_seconds=900,
    ip_binding_enabled=True,
)

token = client.generate_token("alice", extra_claims={"role": "viewer"}, client_ip="10.0.0.1")
payload = client.validate_token(token, client_ip="10.0.0.1")
print(payload["sub"])  # alice
```

Install: `pip install -e python/`

### Node.js / TypeScript

```typescript
import { QuantumClient } from "@quantum-auth/jwt-client";

const client = new QuantumClient({ redisHost: "localhost" });
await client.connect();

const token = await client.generateToken("alice", { role: "viewer" }, "10.0.0.1");
const payload = await client.validateToken(token, "10.0.0.1");
console.log(payload.sub); // alice

await client.disconnect();
```

Install: `npm install` inside `nodejs/`

### .NET

```csharp
using QuantumJwt;

var client = new QuantumJwtClient(new QuantumJwtOptions
{
    RedisConnectionString = "localhost:6379,password=app-password",
    TokenTtl = TimeSpan.FromMinutes(15),
    IpBindingEnabled = true,
});

string token = await client.GenerateTokenAsync("alice", new Dictionary<string, object> { ["role"] = "viewer" }, "10.0.0.1");
var payload = await client.ValidateTokenAsync(token, "10.0.0.1");
Console.WriteLine(payload.Subject); // alice
```

Install: `dotnet build` inside `dotnet/`

---

## Monitoring

Once the stack is running, open Grafana at `http://localhost:3001` (default credentials `admin / admin`). The pre-configured Prometheus datasource at `http://localhost:9090` scrapes:

- Entropy pool size and key throughput from the miner (`miner_stats` Redis hash)
- Application-level token issuance and validation metrics (exposed by the example Spring Boot app at `GET /api/metrics`)

The React dashboard (`dashboard/`) provides a live view of pool entropy, token lifecycle events, and security alerts. Run it locally with:

```bash
cd dashboard
npm install
npm start   # http://localhost:3000
```

---

## Next Steps

- Read the [API Reference](api-reference.md) for the full method signatures and error types.
- Read the [Architecture Overview](ARCHITECTURE.md) to understand how entropy flows from the quantum source to the token.
- Review the [Threat Model](threat-model.md) for the security assumptions and attack surface.
- See `java/examples/spring-boot-app/` for a complete working Spring Boot application.
