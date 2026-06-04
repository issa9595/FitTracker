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
# Stage 2 : JRE minimal via jlink
# Construit un runtime Java reduit ne contenant que les modules
# necessaires a Spring Boot (cf. modules listes). Gain ~80 Mo
# par rapport a eclipse-temurin:21-jre-alpine complet.
# ============================================================
FROM eclipse-temurin:21-jdk-alpine AS jre-builder

# Set d'options conservateur pour Spring Boot + observabilite + JDBC + JNDI/DNS.
# Si une lib runtime requiert un module manquant, on l'ajoute ici.
RUN "$JAVA_HOME/bin/jlink" \
        --add-modules java.base,java.compiler,java.desktop,java.instrument,java.logging,java.management,java.management.rmi,java.naming,java.net.http,java.prefs,java.rmi,java.scripting,java.security.jgss,java.security.sasl,java.sql,java.sql.rowset,java.transaction.xa,java.xml,java.xml.crypto,jdk.crypto.cryptoki,jdk.crypto.ec,jdk.httpserver,jdk.jdwp.agent,jdk.management,jdk.management.agent,jdk.naming.dns,jdk.naming.rmi,jdk.unsupported,jdk.zipfs \
        --no-header-files \
        --no-man-pages \
        --strip-debug \
        --compress=2 \
        --output /opt/jre-min

# ============================================================
# Stage 3 : runtime
# ============================================================
FROM alpine:3.20 AS runtime

LABEL org.opencontainers.image.title="FitTracker"
LABEL org.opencontainers.image.description="Back-end Java de suivi d'entrainement sportif"
LABEL org.opencontainers.image.source="https://github.com/issa9595/FitTracker"
LABEL org.opencontainers.image.licenses="MIT"

ENV JAVA_HOME=/opt/jre
ENV PATH="${JAVA_HOME}/bin:${PATH}"

# Copie du JRE custom + libs necessaires a l'execution Java sur Alpine
COPY --from=jre-builder /opt/jre-min ${JAVA_HOME}

# Utilisateur non-root (wget pour le HEALTHCHECK est deja fourni par busybox d'alpine)
RUN addgroup -S fittracker && adduser -S fittracker -G fittracker
WORKDIR /app

COPY --from=builder /workspace/target/fittracker.jar /app/app.jar
RUN chown -R fittracker:fittracker /app

USER fittracker

EXPOSE 8080

ENV JAVA_OPTS="" \
    SPRING_PROFILES_ACTIVE=prod \
    SERVER_PORT=8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD wget -qO- http://localhost:${SERVER_PORT}/actuator/health >/dev/null || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
