# Quantum JWT Framework

A multi-language, quantum-entropy-powered JWT authentication framework. This monorepo contains client libraries, an entropy miner, a monitoring dashboard, and infrastructure configurations to provide cryptographically superior JWT security.

## Philosophy: Entropy-First Security

Most JWT implementations rely on a single static secret or periodic rotation of classical keys. The **Quantum JWT Framework** takes a different approach:
1. **Quantum Entropy Pool**: A dedicated miner fetches true quantum randomness (via IBM Quantum) and fills a Redis-backed entropy pool.
2. **Ephemeral Per-Token Keys**: Every token uses a unique key popped from the pool. Keys are stored only for the token's lifetime.
3. **Protocol-Level Security**: Features like IP binding, honey tokens, and quantum watermarking are built-in and opt-out.

---

## Project Structure

- `quantum-miner/`: Python service that generates entropy and fills the Redis pool.
- `java/`: Spring Boot starter for seamless integration into Java microservices.
- `python/`: Production-ready Python client library.
- `nodejs/`: TypeScript client library with full type safety.
- `dotnet/`: .NET 8 C# client library and ASP.NET Core integrations.
- `dashboard/`: React-based real-time monitoring dashboard.
- `infrastructure/`: Docker Compose, Prometheus, Grafana, and Redis configurations.

---

## Getting Started

### 1. Prerequisites
- Docker & Docker Compose
- Node.js (for Dashboard)
- Python 3.9+ (for Miner/Client)
- Java 17+ (for Java components)
- .NET 8 SDK (for .NET components)

### 2. Launch the Infrastructure
```bash
cd infrastructure
docker compose up -d
```
This starts Redis (with ACLs), Prometheus, and Grafana.

### 3. Start the Entropy Miner
```bash
cd quantum-miner
# (Optional) Add IBM_QUANTUM_TOKEN to .env for real quantum source
python3 -m pip install -r requirements.txt
python3 miner.py
```

### 4. Use a Client Library (Example: Python)
```python
from quantum_jwt.client import QuantumJwtClient

client = QuantumJwtClient(redis_host="localhost", redis_port=6379)
token = client.generate_token("user_123", client_ip="192.168.1.5")
payload = client.validate_token(token, client_ip="192.168.1.5")
print(f"Validated user: {payload['sub']}")
```

---

## Monitoring
- **Dashboard**: `cd dashboard && npm install && npm start` (Runs on port 3000)
- **Grafana**: `http://localhost:3001` (Default: admin/quantum-admin)
- **Prometheus**: `http://localhost:9090`

## Verification
Run tests for any component using the standard test runner for that language (pytest, jest, xunit, maven).
