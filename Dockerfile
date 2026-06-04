# syntax=docker/dockerfile:1.7

# ============================================================
# Stage 1 : build
# ============================================================
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /workspace

# Copie du pom.xml d'abord pour beneficier du cache de couche Maven
COPY pom.xml ./
COPY .mvn .mvn
COPY mvnw mvnw

# Resolution offline des dependances (cache exploitable entre builds)
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q dependency:go-offline

# Copie du code source apres pour invalider la cache uniquement si le code change
COPY src ./src
COPY checkstyle.xml ./

# Build du jar (tests sautes : ils tournent dans le job CI 'build')
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q package -DskipTests

# ============================================================
# Stage 2 : runtime
# ============================================================
FROM eclipse-temurin:21-jre-alpine AS runtime

LABEL org.opencontainers.image.title="FitTracker"
LABEL org.opencontainers.image.description="Back-end Java de suivi d'entrainement sportif"
LABEL org.opencontainers.image.source="https://github.com/issa9595/FitTracker"
LABEL org.opencontainers.image.licenses="MIT"

# Outils minimaux pour le HEALTHCHECK (wget est present sur alpine)
RUN apk add --no-cache curl tini

# Utilisateur non-root
RUN addgroup -S fittracker && adduser -S fittracker -G fittracker
WORKDIR /app

COPY --from=builder /workspace/target/fittracker.jar /app/app.jar
RUN chown -R fittracker:fittracker /app

USER fittracker

EXPOSE 8080

ENV JAVA_OPTS="" \
    SPRING_PROFILES_ACTIVE=prod \
    SERVER_PORT=8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD curl -fsS http://localhost:${SERVER_PORT}/actuator/health || exit 1

ENTRYPOINT ["/sbin/tini", "--", "sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
