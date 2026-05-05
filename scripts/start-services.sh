#!/bin/bash
# Start all backend services in order: Eureka → Gateway → Auth → Document → Collaboration
# Run from project root: ./scripts/start-services.sh

set -e
BACKEND="$(dirname "$0")/../backend"
LOG_DIR="$(dirname "$0")/../logs"
ENV_FILE="$(dirname "$0")/../.env"
mkdir -p "$LOG_DIR"

# Load env vars from .env if it exists
if [ -f "$ENV_FILE" ]; then
  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a
  echo "==> Loaded environment from .env"
fi

cd "$BACKEND"

echo "==> Starting Eureka Server (port 8761)..."
./mvnw -pl eureka-server spring-boot:run -q > "$LOG_DIR/eureka.log" 2>&1 &
EUREKA_PID=$!
echo "    Eureka PID: $EUREKA_PID"

echo "    Waiting for Eureka to be ready..."
until curl -s http://localhost:8761/actuator/health | grep -q UP; do sleep 2; done
echo "    Eureka is UP"

echo "==> Starting API Gateway (port 8080)..."
./mvnw -pl api-gateway spring-boot:run -q > "$LOG_DIR/gateway.log" 2>&1 &
echo "    Gateway PID: $!"

echo "==> Starting Auth Service (port 8081)..."
./mvnw -pl auth-service spring-boot:run -q > "$LOG_DIR/auth.log" 2>&1 &
echo "    Auth PID: $!"

echo "==> Starting Document Service (port 8082)..."
./mvnw -pl document-service spring-boot:run -q > "$LOG_DIR/document.log" 2>&1 &
echo "    Document PID: $!"

echo "==> Starting Collaboration Service (port 8083)..."
./mvnw -pl collaboration-service spring-boot:run -q > "$LOG_DIR/collab.log" 2>&1 &
echo "    Collab PID: $!"

echo ""
echo "All services starting. Logs in logs/"
echo "  Eureka dashboard:  http://localhost:8761"
echo "  Gateway ping:      http://localhost:8080/ping"
echo "  Kafka UI:          http://localhost:8090"
echo ""
echo "To stop all: kill \$(lsof -ti:8761,8080,8081,8082,8083)"

wait
