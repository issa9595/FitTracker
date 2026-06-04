# ============================================================
# FitTracker - Makefile
# Cibles courtes pour les operations recurrentes.
# ============================================================

SHELL := /usr/bin/env bash

IMAGE_NAME ?= fittracker
REGISTRY   ?= ghcr.io/issa9595

.PHONY: help build verify clean lint format docker-build docker-run docker-push

help:
	@echo "Cibles disponibles :"
	@echo "  build         Compile et package (skip tests)"
	@echo "  verify        Lint + tests unitaires (mvn verify)"
	@echo "  lint          Spotless check + Checkstyle"
	@echo "  format        Spotless apply (formate le code)"
	@echo "  clean         mvn clean"
	@echo "  docker-build  Build de l'image Docker"
	@echo "  docker-run    Run de l'image en local (port 8080)"
	@echo "  docker-push   ./scripts/build-and-push.sh --push"

build:
	./mvnw -B package -DskipTests

verify:
	./mvnw -B verify

lint:
	./mvnw -B spotless:check checkstyle:check

format:
	./mvnw -B spotless:apply

clean:
	./mvnw -B clean

docker-build:
	./scripts/build-and-push.sh

docker-run: docker-build
	docker run --rm -p 8080:8080 --name $(IMAGE_NAME) $(IMAGE_NAME):$$(git rev-parse --short=12 HEAD)

docker-push:
	./scripts/build-and-push.sh --push
