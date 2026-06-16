#!/usr/bin/env bash
# ============================================================
# FitTracker - demo securite Phase 6 en UNE commande (app deja lancee).
#
#   ./scripts/dev-smoke.sh
#
# Verifie : 401 sans token, login du user de demo, 200 avec token.
# Base configurable :  BASE=http://localhost:8080 ./scripts/dev-smoke.sh
# ============================================================
set -euo pipefail

BASE="${BASE:-http://localhost:8080}"

echo "1) GET /api/v1/users/me sans token (attendu 401)"
curl -s -o /dev/null -w "   -> HTTP %{http_code}\n" "$BASE/api/v1/users/me"

echo "2) POST /api/v1/auth/login (user de demo)"
TOKEN=$(curl -s -X POST "$BASE/api/v1/auth/login" \
  -H 'Content-Type: application/json' \
  -d '{"email":"test@fittracker.dev","password":"ChangeMe123!"}' \
  | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')

if [ -z "$TOKEN" ]; then
  echo "   -> echec du login (l'app est-elle lancee ? cf. ./scripts/dev-run.sh)"
  exit 1
fi
echo "   -> token obtenu : ${TOKEN:0:24}..."

echo "3) GET /api/v1/users/me avec token (attendu 200)"
curl -s -o /dev/null -w "   -> HTTP %{http_code}\n" \
  "$BASE/api/v1/users/me" -H "Authorization: Bearer $TOKEN"
