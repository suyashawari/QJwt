# Quantum-Seeded Dynamic QWT Framework

> A production-ready, hybrid-language authentication system bridging Quantum Physics (Python/Qiskit) with Enterprise Security (Java Spring Boot/Redis).

![Architecture](https://img.shields.io/badge/Architecture-Producer--Consumer-blueviolet)
![Python](https://img.shields.io/badge/Python-3.10+-3776ab)
![Java](https://img.shields.io/badge/Java-17+-orange)
![React](https://img.shields.io/badge/React-18-61dafb)

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    ⚛️ Quantum Layer                              │
│  IBM Quantum Computer → Qiskit Runtime → True Randomness        │
└─────────────────────┬───────────────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────────────┐
│                    🐍 Python Miner (Producer)                    │
│  miner.py: Batch generation, Fail-safe fallback, Honey tokens   │
└─────────────────────┬───────────────────────────────────────────┘
                      │ RPUSH
┌─────────────────────▼───────────────────────────────────────────┐
│                    ⚡ Redis Speed Buffer                         │
│  entropy_pool (LLEN > 1000), auth:kid mappings, poison_keys     │
└─────────────────────┬───────────────────────────────────────────┘
                      │ LPOP
┌─────────────────────▼───────────────────────────────────────────┐
│                    ☕ Java Spring Boot (Consumer)                │
│  AuthController: Login, Verify, Status APIs with JWT signing    │
└─────────────────────┬───────────────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────────────┐
│                    ⚛️ React Dashboard                            │
│  FuelGauge, Heatmap, Real-time monitoring                       │
└─────────────────────────────────────────────────────────────────┘
```

## 📁 Project Structure

```
quantum-auth-framework/
├── infrastructure/
│   └── docker-compose.yml       # Redis container
├── quantum-miner/               # Python Producer
│   ├── miner.py                 # 24/7 daemon
│   ├── requirements.txt
│   └── .env.example
├── secure-api/                  # Java Consumer
│   ├── pom.xml
│   └── src/main/java/com/quantum/auth/
│       ├── QuantumAuthApplication.java
│       ├── controller/AuthController.java
│       ├── service/QuantumService.java
│       ├── service/JwtService.java
│       ├── dto/*.java
│       └── exception/*.java
└── dashboard/                   # React Frontend
    ├── package.json
    └── src/
        ├── App.js
        ├── components/FuelGauge.js
        └── components/Heatmap.js
```

## 🚀 Quick Start

### 1. Start Redis
```bash
cd infrastructure
docker-compose up -d
```

### 2. Configure Python Miner
```bash
cd quantum-miner
cp .env.example .env
# Edit .env with your IBM Quantum token
pip install -r requirements.txt
python miner.py
```

### 3. Start Java API
```bash
cd secure-api
./mvnw spring-boot:run
```

### 4. Start Dashboard
```bash
cd dashboard
npm install
npm start
```

## 🔐 API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/login` | Authenticate and get QWT token |
| GET | `/api/auth/verify` | Verify token validity |
| GET | `/api/status` | Get entropy pool stats |
| GET | `/api/health` | Health check |

### Login Example
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"secret123","riskScore":25}'
```

## 🛡️ Security Features

- **Quantum Randomness**: True randomness from IBM Quantum computers
- **Adaptive TTL**: High-risk sessions get 10s tokens; normal get 15min
- **Honey Tokens**: Canary keys to detect breaches
- **IP Blocking**: Automatic block on honey token detection
- **Classical Fallback**: Secure fallback when quantum unavailable

## ⚙️ Configuration

### Python Miner (.env)
```env
IBM_QUANTUM_TOKEN=your_ibm_quantum_token
REDIS_HOST=localhost
REDIS_PORT=6379
MIN_POOL_SIZE=1000
BATCH_SIZE=1024
HONEY_TOKEN_FREQUENCY=500
```

### Java API (application.properties)
```properties
spring.data.redis.host=localhost
spring.data.redis.port=6379
jwt.default-ttl-seconds=900
jwt.high-risk-ttl-seconds=10
```

## 📜 License

MIT License - Use freely for research and production.
