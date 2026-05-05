# Collaborative Document Editor — Project Spec

> **This file is the source of truth. Claude Code should read this before any task.**
> Keep it updated as decisions change.

## Goal

Build an interview-portfolio-grade real-time collaborative document editor that demonstrates distributed systems concepts: microservices, WebSockets, CRDT-based conflict resolution, event streaming, and caching.

**Not the goal:** Replacing Google Docs. Production scale. Perfect UX.

## Scope (6-Week Plan)

### In Scope
- 4 microservices: API Gateway, Auth, Document, Collaboration
- Real-time editing via WebSocket + Yjs (CRDT library)
- Kafka event streaming for edit events + audit log
- Redis for presence (online users, cursor positions)
- MySQL for persistent document storage
- Eureka for service discovery
- React frontend with rich text editing
- Docker Compose for local infrastructure
- One deployed demo

### Out of Scope (Talk about as "future work" in interviews)
- Hand-rolled CRDT/OT (use Yjs — production CRDTs are research-level)
- Separate Notification, Search, Presence, Sync services (folded into Collaboration)
- Offline editing, version history UI, fine-grained access control
- Multi-region deployment, horizontal scaling tests

## Architecture

```
┌─────────────┐
│   React     │  ← Frontend (Yjs client, WebSocket, y-websocket)
│  Frontend   │
└──────┬──────┘
       │ HTTPS / WSS
       ▼
┌─────────────────────┐
│   API Gateway       │  ← Spring Cloud Gateway (port 8080)
│   (JWT validation)  │
└──────┬──────────────┘
       │
       ├────────────┬────────────────┬──────────────────┐
       ▼            ▼                ▼                  ▼
  ┌─────────┐  ┌─────────┐  ┌──────────────────┐  ┌──────────┐
  │  Auth   │  │Document │  │  Collaboration   │  │  Eureka  │
  │ Service │  │ Service │  │  Service (WS)    │  │ (8761)   │
  │ (8081)  │  │ (8082)  │  │    (8083)        │  │          │
  └────┬────┘  └────┬────┘  └────────┬─────────┘  └──────────┘
       │           │                 │
       │           │                 ├──► Kafka (edit-events, audit-log)
       ▼           ▼                 ├──► Redis (presence, cursors)
    ┌───────────────────┐            └──► Yjs doc state
    │      MySQL        │
    │  (users, docs)    │
    └───────────────────┘
```

## Services

### 1. API Gateway (port 8080)
- Spring Cloud Gateway
- Routes: `/api/auth/**` → Auth, `/api/documents/**` → Document, `/ws/**` → Collaboration
- Validates JWT on protected routes
- WebSocket pass-through to Collaboration

### 2. Auth Service (port 8081)
- Signup, login, JWT issuance
- User entity: id, email, password (bcrypt), displayName
- Endpoints: `POST /signup`, `POST /login`, `GET /me`
- Stores users in MySQL

### 3. Document Service (port 8082)
- Document CRUD + metadata
- Document entity: id, ownerId, title, createdAt, updatedAt, latestSnapshot (BLOB/JSON of Yjs state)
- Endpoints: `GET /documents`, `POST /documents`, `GET /documents/{id}`, `PATCH /documents/{id}`, `DELETE /documents/{id}`
- Consumes Kafka `edit-events` topic to persist snapshots periodically
- Stores docs in MySQL

### 4. Collaboration Service (port 8083)
- Spring WebSocket server
- Handles `/ws/documents/{docId}` connections
- Integrates with Yjs via `y-websocket` protocol (Java implementation)
- Per-document Yjs document held in memory (loaded from Document Service on first connect)
- Publishes every update to Kafka `edit-events` topic
- Tracks presence in Redis: `presence:doc:{docId}` → set of userIds, `cursor:{docId}:{userId}` → position

### Infrastructure
- **Eureka** (port 8761) — service discovery
- **MySQL** (3306) — persistence
- **Redis** (6379) — presence
- **Kafka** (9092) + **Zookeeper** (2181) — event streaming
- All via Docker Compose

## Tech Stack

| Layer | Choice |
|---|---|
| Backend | Java 17, Spring Boot 3.x |
| Gateway | Spring Cloud Gateway |
| Discovery | Netflix Eureka |
| Messaging | Apache Kafka (Spring Kafka) |
| Cache | Redis (Spring Data Redis) |
| DB | MySQL 8 (Spring Data JPA) |
| WebSocket | Spring WebSocket |
| CRDT | Yjs (frontend), y-protocols binary diff on server |
| Frontend | React 18 + Vite, Yjs + y-websocket, TipTap editor (ProseMirror-based, Yjs-ready) |
| Auth | JWT (jjwt library) |
| Build | Maven (backend), npm/pnpm (frontend) |
| Infra | Docker Compose |
| Deploy | Railway or Render (free tier) |

## Folder Structure

```
collab-docs/
├── docker-compose.yml           # MySQL, Redis, Kafka, Zookeeper, Eureka
├── PROJECT.md                   # this file
├── PROMPTS.md                   # prompts for Claude Code
├── README.md                    # portfolio-facing readme
├── backend/
│   ├── pom.xml                  # parent pom
│   ├── eureka-server/
│   ├── api-gateway/
│   ├── auth-service/
│   ├── document-service/
│   └── collaboration-service/
└── frontend/
    ├── package.json
    ├── vite.config.ts
    └── src/
        ├── App.tsx
        ├── pages/               # Login, Signup, DocList, Editor
        ├── components/          # Editor (TipTap + Yjs), PresenceBar
        ├── hooks/               # useAuth, useDocument, useCollab
        └── api/                 # axios clients per service
```

## Data Flow (Edit Operation)

1. User types in TipTap editor
2. Yjs produces an update (binary diff)
3. `y-websocket` provider sends update over WebSocket to Collaboration Service
4. Collaboration Service:
   - Applies update to in-memory Yjs doc
   - Broadcasts to other connected clients on same doc
   - Publishes update event to Kafka `edit-events` topic
5. Document Service consumes `edit-events`:
   - Debounces (every 5s or N updates)
   - Writes latest Yjs state snapshot to MySQL
6. Audit log consumer (inside Document Service or separate) logs to `audit-log` topic

## Interview Talking Points

When asked about this project, lead with these:

1. **Why microservices here?** Different scaling characteristics — WebSocket connections scale with concurrent users, DB scales with documents, CPU scales with edit rate.
2. **Why Yjs?** Production CRDTs are a research problem. Yjs is battle-tested (used by Evernote, JetBrains). The interesting work was integrating it with Kafka for durable event logs and server-side broadcast.
3. **Why Kafka?** Enables replay, audit logging, and async persistence without blocking the real-time path. Decouples "edit happens" from "edit is saved."
4. **Conflict resolution?** CRDT (specifically Yjs's YATA algorithm) — every operation commutes, so order doesn't matter. Contrast with OT which requires transformation functions.
5. **Trade-offs?** In-memory Yjs docs in Collaboration Service = single point of failure per doc. Production would need sticky sessions + Redis-backed doc state or a cluster-aware solution like Hocuspocus.
6. **What I'd do next?** Horizontal scaling of Collaboration Service with sticky routing by docId, version history UI, fine-grained permissions, offline support (Yjs supports it natively).

## Phase Checklist

- [ ] **Phase 0:** Repo init, docker-compose up, all infra healthy
- [ ] **Phase 1:** Eureka + Gateway running, can register dummy service
- [ ] **Phase 2:** Auth service — signup/login/JWT working via Postman
- [ ] **Phase 3:** Document service — CRUD working via Gateway with JWT
- [ ] **Phase 4:** React frontend — login, doc list, create doc (no real-time yet)
- [ ] **Phase 5:** Collaboration service — basic WebSocket echo working
- [ ] **Phase 6:** Yjs integration — two browser tabs edit same doc live
- [ ] **Phase 7:** Kafka integration — edits flow through Kafka, snapshots saved
- [ ] **Phase 8:** Presence — see who's online, cursor positions visible
- [ ] **Phase 9:** Polish, error handling, README with architecture diagram
- [ ] **Phase 10:** Deploy to Railway/Render, record demo video

## Decisions Log

Record architectural decisions and changes here as you build.

| Date | Decision | Reason |
|---|---|---|
| Day 0 | Use Yjs not custom CRDT | Scope; Yjs is the right tool |
| Day 0 | Fold Sync + Presence into Collaboration service | Simplicity; still demonstrates concepts |
| | | |
