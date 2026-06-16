#!/usr/bin/env bash
# ============================================================
# FitTracker - lance l'app en local en UNE commande (profil dev).
#
#   ./scripts/dev-run.sh
#
# Demarre un PostgreSQL jetable (Docker), attend qu'il soit pret, puis lance
# l'app sur http://localhost:8080. Le profil dev seede l'utilisateur de demo
# (test@fittracker.dev / ChangeMe123!). Ctrl-C arrete l'app ET supprime la base.
#
# Port DB configurable (defaut 5433 pour eviter un conflit avec un autre
# Postgres sur 5432) :  DB_PORT=5434 ./scripts/dev-run.sh
# ============================================================
set -euo pipefail

CONTAINER=fittracker-dev-db
DB_PORT="${DB_PORT:-5433}"

cd "$(dirname "$0")/.."

cleanup() {
  echo
  echo "==> Arret : suppression de la base de demo ($CONTAINER)..."
  docker rm -f "$CONTAINER" >/dev/null 2>&1 || true
}
trap cleanup EXIT

echo "==> PostgreSQL jetable sur le port ${DB_PORT}"
if docker ps -a --format '{{.Names}}' | grep -qx "$CONTAINER"; then
  docker rm -f "$CONTAINER" >/dev/null 2>&1 || true
fi
docker run -d --name "$CONTAINER" \
  -e POSTGRES_DB=fittracker -e POSTGRES_USER=fittracker -e POSTGRES_PASSWORD=fittracker \
  -p "${DB_PORT}:5432" postgres:16-alpine >/dev/null

printf "==> Attente de PostgreSQL"
until docker exec "$CONTAINER" pg_isready -U fittracker -d fittracker >/dev/null 2>&1; do
  printf "."
  sleep 1
done
echo " pret"

echo "==> Lancement de FitTracker sur http://localhost:8080  (Ctrl-C pour tout arreter)"
echo "    Demo : test@fittracker.dev / ChangeMe123!  -  smoke test : ./scripts/dev-smoke.sh"
SPRING_PROFILES_ACTIVE=dev \
  SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:${DB_PORT}/fittracker" \
  SPRING_DATASOURCE_USERNAME=fittracker \
  SPRING_DATASOURCE_PASSWORD=fittracker \
  ./mvnw spring-boot:run
