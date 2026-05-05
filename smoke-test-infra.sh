#!/usr/bin/env bash
# Smoke test: verify all infrastructure is reachable
set -e

echo "→ Testing MySQL..."
docker exec collab-mysql mysqladmin ping -h localhost -u collab -pcollabpass --silent \
  && echo "  ✓ MySQL OK" || { echo "  ✗ MySQL FAILED"; exit 1; }

echo "→ Testing Redis..."
docker exec collab-redis redis-cli ping | grep -q PONG \
  && echo "  ✓ Redis OK" || { echo "  ✗ Redis FAILED"; exit 1; }

echo "→ Testing Kafka..."
docker exec collab-kafka kafka-topics --bootstrap-server localhost:9092 --list > /dev/null \
  && echo "  ✓ Kafka OK" || { echo "  ✗ Kafka FAILED"; exit 1; }

echo "→ Testing Kafka UI..."
curl -sf http://localhost:8090/actuator/health > /dev/null \
  && echo "  ✓ Kafka UI OK" || echo "  ⚠ Kafka UI not ready yet (takes ~30s)"

echo ""
echo "All infrastructure healthy. Next: run Eureka + services."
