#!/usr/bin/env bash
# ============================================================
# build-and-push.sh
# Build l'image Docker FitTracker, la tague avec le SHA git
# court et un tag latest, puis simule un push vers un registry.
#
# Usage :
#   ./scripts/build-and-push.sh                # build + tag, pas de push reel
#   ./scripts/build-and-push.sh --push         # push reel vers $REGISTRY
#
# Variables d'env :
#   REGISTRY      registry cible (defaut: ghcr.io/issa9595)
#   IMAGE_NAME    nom de l'image (defaut: fittracker)
# ============================================================
set -euo pipefail

REGISTRY="${REGISTRY:-ghcr.io/issa9595}"
IMAGE_NAME="${IMAGE_NAME:-fittracker}"
DO_PUSH="${1:-}"

if ! command -v docker >/dev/null 2>&1; then
  echo "[build-and-push] docker introuvable, abandon." >&2
  exit 1
fi

if [[ -d .git ]]; then
  GIT_SHA="$(git rev-parse --short=12 HEAD 2>/dev/null || echo unknown)"
else
  GIT_SHA="local"
fi

FULL_TAG="${REGISTRY}/${IMAGE_NAME}:${GIT_SHA}"
LATEST_TAG="${REGISTRY}/${IMAGE_NAME}:latest"
LOCAL_TAG="${IMAGE_NAME}:${GIT_SHA}"

echo "[build-and-push] build image locale ${LOCAL_TAG}"
docker build -t "${LOCAL_TAG}" .

echo "[build-and-push] tag ${LOCAL_TAG} -> ${FULL_TAG}"
docker tag "${LOCAL_TAG}" "${FULL_TAG}"

echo "[build-and-push] tag ${LOCAL_TAG} -> ${LATEST_TAG}"
docker tag "${LOCAL_TAG}" "${LATEST_TAG}"

if [[ "${DO_PUSH}" == "--push" ]]; then
  echo "[build-and-push] push ${FULL_TAG}"
  docker push "${FULL_TAG}"
  echo "[build-and-push] push ${LATEST_TAG}"
  docker push "${LATEST_TAG}"
else
  echo "[build-and-push] simulation : aucune image n'est poussee. Ajouter --push pour pousser."
fi

echo "[build-and-push] termine."
echo "[build-and-push]   image locale : ${LOCAL_TAG}"
echo "[build-and-push]   tag distant  : ${FULL_TAG}"
echo "[build-and-push]   tag latest   : ${LATEST_TAG}"
