# syntax=docker/dockerfile:1

FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew

COPY src/main src/main
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:21-jre-jammy

RUN apt-get update \
    && apt-get install --yes --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

RUN groupadd --system logiops \
    && useradd --system --gid logiops --home-dir /app --shell /usr/sbin/nologin logiops

WORKDIR /app

COPY --from=builder --chown=logiops:logiops /workspace/build/libs/logiops-*.jar app.jar

USER logiops

EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=3s --start-period=20s --retries=5 \
  CMD ["curl", "--fail", "--silent", "--show-error", "http://localhost:8080/actuator/health"]

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
