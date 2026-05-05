# Claude Code Prompt Playbook

> **How to use:** Work through phases in order. Paste each prompt into Claude Code, review what it does, test it, commit, then move on. Never skip testing.
>
> **Rule 0:** Always start a Claude Code session with: *"Read PROJECT.md first. That's the source of truth."*

## Setup (One-Time)

### Prompt S1 — Initialize
```
Read PROJECT.md in this directory. That's the spec. Confirm you understand
the architecture, then create the folder structure exactly as described in
the "Folder Structure" section. Create empty directories for now, and an
empty README.md at the root. Do not start coding services yet.
```

### Prompt S2 — Git init
```
Initialize git, create a .gitignore suitable for Java (Maven), Node (npm/pnpm),
IntelliJ, VSCode, and macOS/Windows. Add PROJECT.md, PROMPTS.md, README.md,
and docker-compose.yml (which already exists) and make an initial commit
"chore: initial scaffold".
```

---

## Phase 0 — Infrastructure

### Prompt P0.1 — Verify docker-compose
```
Review the docker-compose.yml at the root. Explain what each service does
and what port it exposes. Then run `docker compose up -d` and verify all
services are healthy with `docker compose ps`. If anything is unhealthy,
investigate and fix.
```

### Prompt P0.2 — Smoke tests
```
Write a shell script `scripts/smoke-test-infra.sh` that verifies:
- MySQL is reachable on 3306 and accepts the configured user/password
- Redis responds to PING on 6379
- Kafka is reachable on 9092 (use kafka-topics.sh or a small Python/Node
  script to list topics)
Run it and confirm all three pass.
```

---

## Phase 1 — Eureka + Gateway

### Prompt P1.1 — Parent Maven POM
```
Create backend/pom.xml as a parent Maven project with:
- Java 17
- Spring Boot 3.2.x (latest stable)
- Spring Cloud 2023.x (matching version)
- Modules: eureka-server, api-gateway, auth-service, document-service,
  collaboration-service (create empty modules, we'll fill them in)
Use dependencyManagement for Spring Boot and Spring Cloud BOMs.
```

### Prompt P1.2 — Eureka Server
```
Implement backend/eureka-server as a Spring Boot app with @EnableEurekaServer.
Port 8761. No self-registration. Verify it runs and the dashboard at
http://localhost:8761 is accessible.
```

### Prompt P1.3 — API Gateway
```
Implement backend/api-gateway as a Spring Cloud Gateway app.
Port 8080. Register with Eureka. For now, add a single test route:
GET /ping → returns "pong" (implement via a simple RouterFunction or
a stub downstream). We will wire real routes as services come online.
```

**Test before moving on:** Visit http://localhost:8761 — gateway should appear. Curl http://localhost:8080/ping → should return pong.

---

## Phase 2 — Auth Service

### Prompt P2.1 — Auth Service skeleton
```
Implement backend/auth-service per PROJECT.md section "2. Auth Service":
- Spring Boot, port 8081, registers with Eureka
- MySQL datasource using credentials from docker-compose
- User JPA entity: id (UUID), email (unique), passwordHash, displayName,
  createdAt
- Flyway or Liquibase migration for the users table
- POST /signup, POST /login, GET /me endpoints
- JWT issuance with jjwt library, 24-hour expiry, HS256, secret from
  application.yml (with env var override)
- BCrypt password hashing
- Global exception handler for 400/401/409 responses with clean JSON
Write a README.md in the service folder documenting the endpoints.
```

### Prompt P2.2 — Gateway integration
```
In api-gateway, add a route:  /api/auth/** → lb://auth-service (strip prefix)
Add a JWT validation filter that:
- Skips /api/auth/signup and /api/auth/login
- For all other /api/** routes, validates the JWT and sets X-User-Id header
  before forwarding downstream
Use the same secret as auth-service (via shared config or env var).
```

### Prompt P2.3 — Postman/HTTP tests
```
Create a file `http/auth.http` with REST Client (VSCode) or IntelliJ HTTP
syntax containing:
- Signup request
- Login request (captures token into a variable)
- GET /me using the captured token
- A negative test: GET /me without token → 401
Walk me through running these; I will run them and confirm.
```

**Test before moving on:** All 4 HTTP tests pass through the gateway.

---

## Phase 3 — Document Service

### Prompt P3.1 — Document Service
```
Implement backend/document-service per PROJECT.md section "3. Document Service":
- Spring Boot, port 8082, Eureka-registered
- Document JPA entity: id (UUID), ownerId (UUID), title, createdAt, updatedAt,
  latestSnapshot (BYTEA/LONGBLOB for Yjs state, nullable)
- CRUD endpoints per spec, all require JWT (read X-User-Id from gateway header)
- List endpoint returns only docs owned by the requesting user
- Flyway migration for documents table
- README.md in service folder
Do NOT implement Kafka consumer yet — that's Phase 7.
```

### Prompt P3.2 — Gateway route
```
Add /api/documents/** → lb://document-service to api-gateway.
Verify JWT filter applies. Test with http/documents.http file containing
the full CRUD flow.
```

**Test:** Full CRUD flow works end-to-end through gateway with JWT.

---

## Phase 4 — Frontend Skeleton

### Prompt P4.1 — React scaffold
```
Create frontend/ with Vite + React + TypeScript.
- Install: react-router-dom, axios, zustand (for auth state), tailwindcss
- Configure tailwind
- Pages: /login, /signup, /documents (list), /documents/:id (editor stub)
- Create src/api/client.ts — axios instance with baseURL
  http://localhost:8080/api, automatic JWT attach from zustand store
- Zustand auth store: token, user, login(), logout(), persist to localStorage
- Protected route wrapper
- Login/Signup pages wired to /api/auth endpoints
- Document list page: fetches /api/documents, shows list, has "New Document"
  button
- Editor page stub: just shows the document title and a plain textarea that
  saves on blur via PATCH. No real-time yet.
- Basic Tailwind styling, nothing fancy
```

**Test:** Full user flow works — signup, login, create doc, edit title, see it in list.

---

## Phase 5 — Collaboration Service (WebSocket echo)

### Prompt P5.1 — WS server skeleton
```
Implement backend/collaboration-service per PROJECT.md:
- Spring Boot, port 8083, Eureka-registered
- Spring WebSocket config with endpoint /ws/documents/{docId}
- Custom WebSocketHandler that:
  - On connect: logs "User connected to doc {docId}"
  - On message (text): echoes back to sender AND broadcasts to all other
    sessions on the same docId
  - On disconnect: logs and cleans up session
- In-memory session registry: Map<docId, Set<WebSocketSession>>
- Do NOT integrate Yjs yet — just echo/broadcast text
- Handle JWT: parse token from query param `?token=...` on connect,
  validate, reject connection if invalid
```

### Prompt P5.2 — Gateway WS route
```
In api-gateway, add a WebSocket route: /ws/** → lb:ws://collaboration-service
Preserve query params (JWT passes via query param since browsers can't set
headers on WebSocket). Test with wscat or a browser console:
`new WebSocket("ws://localhost:8080/ws/documents/abc123?token=...")`
```

**Test:** Two `wscat` clients on same docId see each other's messages.

---

## Phase 6 — Yjs Integration (THE HARD PART)

> This is the heart of the project. Expect this phase to take the longest.
> Test frequently.

### Prompt P6.1 — Frontend Yjs + TipTap
```
In frontend/, install:
- yjs, y-websocket, y-protocols
- @tiptap/react, @tiptap/starter-kit, @tiptap/extension-collaboration,
  @tiptap/extension-collaboration-cursor
Replace the textarea editor with a TipTap editor configured with:
- StarterKit
- Collaboration extension pointing to a Y.Doc
- CollaborationCursor extension (we'll wire it in Phase 8)
- Y.Doc connected via WebsocketProvider to
  ws://localhost:8080/ws/documents/{docId}?token={jwt}
Don't worry about persistence yet — just get real-time sync between two
browser tabs working.
```

### Prompt P6.2 — Backend y-websocket protocol
```
The y-websocket protocol is a binary message protocol. We need to handle it
on the Java side. Options:
A) Use https://github.com/y-crdt/y-crdt Java bindings (yrs) via JNI
B) Use a simpler "dumb broadcast" approach: server doesn't understand Yjs,
   just broadcasts all binary messages to other clients on same doc
Option B is fine for the MVP — Yjs clients will converge regardless.
Persistence becomes trickier (we can't easily snapshot Yjs state server-side),
but we can work around it.

Implement Option B for now:
- Collaboration WebSocketHandler accepts BinaryMessage
- Broadcasts binary messages to all other sessions on same docId
- Does NOT persist the binary state yet
- Keep text message echo for debugging
```

### Prompt P6.3 — Persistence workaround
```
Since server doesn't parse Yjs state, we persist from the CLIENT side:
- On the frontend, debounce 3s after edits and send a POST to
  /api/documents/{id}/snapshot with Y.encodeStateAsUpdate(ydoc) as base64
- Document service accepts the snapshot and stores to latestSnapshot column
- On document open, frontend fetches GET /api/documents/{id}, decodes
  latestSnapshot if present, and applies via Y.applyUpdate(ydoc, snapshot)
This is an acceptable trade-off documented in PROJECT.md Decisions Log.
```

**Test:** Two tabs edit same doc live. Close both. Reopen. Content is still there.

---

## Phase 7 — Kafka Integration

### Prompt P7.1 — Producer in Collaboration Service
```
Add Spring Kafka to collaboration-service.
Create topic `edit-events` (in docker-compose or via app startup).
On every binary WebSocket message received from a client, publish an event:
{
  eventId: UUID,
  docId,
  userId,
  timestamp,
  updateSize: bytes.length
}
to `edit-events` topic. We publish metadata only, not the binary payload
(too large and Kafka isn't where CRDT state belongs).
Also publish to `audit-log` topic: same event plus action="EDIT".
```

### Prompt P7.2 — Consumer in Document Service
```
Add Spring Kafka consumer to document-service.
Listen on `edit-events` topic. For each event:
- Update the document's `updatedAt` field
- Log the event
This demonstrates async event-driven updates without coupling services.
Also add a consumer for `audit-log` that writes to an audit_log table
(id, docId, userId, action, timestamp).
```

**Test:** Edit a doc. Check Kafka topic with `docker exec` + `kafka-console-consumer.sh`. See events flowing. Check audit_log table — rows appear.

---

## Phase 8 — Presence

### Prompt P8.1 — Redis presence in Collaboration Service
```
Add Spring Data Redis to collaboration-service.
On WebSocket connect:
  SADD presence:doc:{docId} {userId}
  EXPIRE presence:doc:{docId} 300
On disconnect:
  SREM presence:doc:{docId} {userId}
New endpoint: GET /presence/{docId} → returns current members
(requires JWT).
Route it via gateway: /api/presence/** → lb://collaboration-service
```

### Prompt P8.2 — Awareness-based cursors
```
Enable TipTap CollaborationCursor extension with user info (name from auth
store, random color per session). Yjs awareness protocol handles cursor
broadcasting automatically — the "dumb broadcast" server from Phase 6
already supports this because awareness messages are just more binary frames.
Add a PresenceBar component at the top of the editor showing avatars of
connected users (fetched from /api/presence/{docId} on open, updated on
awareness changes).
```

**Test:** Two tabs show each other's cursors with colors and names.

---

## Phase 9 — Polish

### Prompt P9.1 — Error handling pass
```
Do an error-handling pass across all services:
- Consistent error response DTO: {code, message, timestamp}
- Frontend: toast notifications on API errors, reconnect logic on WebSocket
  disconnect (exponential backoff, max 5 retries)
- Graceful degradation: if Collaboration service is down, editor shows
  "Reconnecting..." banner, falls back to local-only editing (Yjs works
  offline natively)
- Backend: @ControllerAdvice in each service
```

### Prompt P9.2 — README.md
```
Write a killer README.md at the project root. Sections:
- Title + 1-line pitch
- Animated GIF placeholder (we'll add one after demo)
- Architecture diagram (embed the PNG from docs/)
- Tech stack table
- Features list
- How to run locally (docker compose up, mvn, npm, all commands)
- How it works (brief CRDT explanation, data flow)
- Trade-offs I made and why (the Decisions Log, reformatted)
- What I'd build next at scale (the interview talking points)
- License (MIT)
This README is what a recruiter sees. Make it shine.
```

---

## Phase 10 — Deploy

### Prompt P10.1 — Deploy prep
```
Research which is easier for this stack between Railway and Render free
tiers. We need: 4 Spring Boot services, MySQL, Redis, Kafka.
Kafka on free tiers is often the blocker — consider:
- Upstash Redis (free tier)
- PlanetScale or Railway MySQL
- Redpanda Cloud or Confluent Cloud free tier for Kafka
Recommend a deployment topology and give me step-by-step instructions.
```

### Prompt P10.2 — Containerize
```
Add Dockerfiles for each backend service (multi-stage: build with Maven,
run on eclipse-temurin:17-jre). Add a Dockerfile for frontend (build with
Node, serve with nginx). Update docker-compose.yml to have a prod profile
that uses these images.
```

### Prompt P10.3 — Demo video script
```
Write a 2-minute demo video script showing:
1. Two browser windows side by side
2. Login as two different users
3. Open same doc, edit simultaneously, show cursors
4. Show Kafka UI (or terminal) with events flowing
5. Close/reopen — content persists
6. Brief architecture diagram overlay at the end
Include what to say for each moment.
```

---

## Emergency Prompts

When things break, these help:

### "I don't understand what's failing"
```
Here's the error: [paste full stack trace or error]
Here's what I was trying to do: [1-line description]
Before fixing it, tell me what you think is wrong and why, and ask me
clarifying questions before making changes.
```

### "Claude Code is going in circles"
```
Stop. Read PROJECT.md again. We're in Phase X. The current issue is Y.
Give me 3 possible approaches with trade-offs, then wait for me to pick
one before implementing.
```

### "Something worked but I don't understand it"
```
Walk me through what you just did. For each file you changed, explain
the why in plain English as if I'm preparing to discuss it in an interview.
```

---

## Weekly Review Prompt

Every Sunday, run this:

```
Look at the Phase Checklist in PROJECT.md and tell me:
1. Which phases are complete (verified working)
2. Which phases are in progress
3. What blockers exist
4. What should I prioritize next week given my 10 hours
Also append a summary to a WEEKLY_LOG.md file.
```
