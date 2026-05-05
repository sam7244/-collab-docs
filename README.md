# Collaborative Document Editor

Real-time collaborative document editing built with microservices, WebSockets, Kafka event streaming, and CRDT-based conflict resolution.

> **Status:** 🚧 In development. See [PROJECT.md](./PROJECT.md) for the spec and [PROMPTS.md](./PROMPTS.md) for the build plan.

## Quickstart

### Prerequisites
- Docker & Docker Compose
- Java 17+
- Node.js 20+
- Maven 3.9+
- Claude Code (for development)

### 1. Start infrastructure
```bash
docker compose up -d
./scripts/smoke-test-infra.sh
```

You should see green checks for MySQL, Redis, and Kafka.

- Kafka UI → http://localhost:8090
- MySQL → localhost:3306 (user: `collab`, pass: `collabpass`)
- Redis → localhost:6379

### 2. Run backend (once services are built)
```bash
cd backend
mvn spring-boot:run -pl eureka-server
# In separate terminals:
mvn spring-boot:run -pl api-gateway
mvn spring-boot:run -pl auth-service
mvn spring-boot:run -pl document-service
mvn spring-boot:run -pl collaboration-service
```

### 3. Run frontend
```bash
cd frontend
npm install
npm run dev
```

### 4. Open
- Frontend: http://localhost:5173
- Eureka dashboard: http://localhost:8761
- Gateway: http://localhost:8080

## Architecture

See [PROJECT.md](./PROJECT.md#architecture) for the full diagram and service breakdown.

## How to Build This

1. Open Claude Code in this directory: `claude`
2. Say: *"Read PROJECT.md first. That's the source of truth."*
3. Open [PROMPTS.md](./PROMPTS.md) and work through phases in order
4. Test after each phase before moving on

## Features

- ✅ Infrastructure scaffolding (MySQL, Redis, Kafka, Kafka UI)
- ⏳ Authentication (JWT)
- ⏳ Document CRUD
- ⏳ Real-time collaborative editing (Yjs CRDT)
- ⏳ Presence & cursor sharing
- ⏳ Kafka event streaming
- ⏳ Version persistence

## License

MIT
