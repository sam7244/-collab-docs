#!/bin/bash
# Quick health check for all services
check() {
  local name=$1 url=$2
  if curl -s --max-time 2 "$url" | grep -q "UP\|pong\|\["; then
    echo "  [OK] $name"
  else
    echo "  [DOWN] $name — restart with: ./mvnw -pl $3 spring-boot:run > logs/$3.log 2>&1 &"
  fi
}

echo "=== Infrastructure ==="
docker compose -f "$(dirname "$0")/../docker-compose.yml" ps --format "table {{.Name}}\t{{.Status}}" 2>/dev/null | grep -v NAME

echo ""
echo "=== Services ==="
check "Eureka    (8761)" "http://localhost:8761/actuator/health" "eureka-server"
check "Gateway   (8080)" "http://localhost:8080/ping"             "api-gateway"
check "Auth      (8081)" "http://localhost:8081/actuator/health"  "auth-service"
check "Document  (8082)" "http://localhost:8082/actuator/health"  "document-service"
check "Collab    (8083)" "http://localhost:8083/actuator/health"  "collaboration-service"
echo ""
echo "=== Frontend ==="
if lsof -ti:3000 &>/dev/null; then echo "  [OK] React frontend (3000)"; else echo "  [DOWN] React frontend — run: cd frontend && npm run dev"; fi
