# Docker Runtime Implementation Plan

> **For agentic workers:** Use `dot-plugin:executing-plans` to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Issue:** Run the WMS API in Docker while using PostgreSQL, Redis, and Mailpit already hosted on HomeServer.

**Goal:** Provide a reproducible image and Compose command without placing credentials or dependency containers in the repository.

**Architecture:** Docker builds the executable Spring Boot JAR in a Java 17 builder stage and copies only that JAR into a Java 17 JRE runtime stage. Compose starts only `wms-api`, injects `.env`, publishes the configured API port, and persists exports through a bind mount.

**Tech stack:** Docker, Docker Compose, Java 17, Gradle, Spring Boot 4.1.1.

## File structure

| File | Responsibility |
| --- | --- |
| `Dockerfile` | Builds the executable JAR and runs it as a non-root user. |
| `docker-compose.yml` | Starts only the API container with `.env` and the exports bind mount. |
| `.dockerignore` | Prevents credentials, local outputs, and irrelevant files from entering the image context. |
| `docs/docker.md` | Documents setup, external dependency addresses, lifecycle commands, and Swagger URL. |

## Tasks

### Task 1: Add container build and runtime configuration

**Description:** Build WMS in a Java 17 multi-stage image and expose it through a single Compose service.

**Skill:** `backend-development`

**Files:**

- Create: `Dockerfile`
- Create: `docker-compose.yml`
- Create: `.dockerignore`

- [x] **Step 1: Add the multi-stage `Dockerfile`.**

```dockerfile
FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /workspace

COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

COPY src src
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

RUN groupadd --system spring && useradd --system --gid spring spring
COPY --from=builder --chown=spring:spring /workspace/build/libs/*.jar app.jar
RUN mkdir /app/exports && chown -R spring:spring /app

USER spring
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

- [x] **Step 2: Add the single-service `docker-compose.yml`.**

```yaml
services:
  wms-api:
    build:
      context: .
    env_file:
      - .env
    environment:
      WMS_EXPORT_DIRECTORY: /app/exports
    ports:
      - "${WMS_SERVER_PORT:-8081}:${WMS_SERVER_PORT:-8081}"
    volumes:
      - ./exports:/app/exports
    restart: unless-stopped
```

- [x] **Step 3: Add `.dockerignore`.**

```gitignore
.git
.gradle
build
.env
.env.*
!.env.example
exports
.idea
.vscode
out
*.iml
*.log
```

- [x] **Step 4: Validate the Compose configuration without starting containers.**

Run: `docker compose config`

Expected: one `wms-api` service, an interpolated port mapping, `.env` as its environment file, and no PostgreSQL, Redis, or Mailpit service.

- [x] **Step 5: Build the image.**

Run: `docker compose build`

Expected: a successful `wms-api` image build; the image contains `/app/app.jar` and does not copy `.env`.

### Task 2: Document Docker operation against HomeServer services

**Description:** Explain the only supported Compose topology and the commands needed to use it safely.

**Skill:** `writing-clearly-and-concisely`

**Files:**

- Create: `docs/docker.md`

- [x] **Step 1: Document prerequisite configuration.**

State that users copy `.env.example` to `.env`, choose `SPRING_PROFILES_ACTIVE`, and set `WMS_DB_HOST`, `WMS_REDIS_HOST`, and `WMS_MAIL_HOST` to a LAN hostname or IP address reachable from the Docker engine. State that `localhost` inside `wms-api` means the API container and cannot reach HomeServer services.

- [x] **Step 2: Document lifecycle commands.**

```bash
docker compose build
docker compose up -d
docker compose logs --tail=100 -f wms-api
docker compose down
```

- [x] **Step 3: Document manual verification.**

After startup, open:

```text
http://localhost:${WMS_SERVER_PORT}/swagger-ui/index.html
```

State that exports are available in the local `./exports` directory and that `docker compose down` does not delete HomeServer data.

- [x] **Step 4: Run Java tests.**

Run: `./gradlew clean test`

Expected: `BUILD SUCCESSFUL`.

**Verification:** `docker compose config`, `docker compose build`, and `./gradlew clean test` complete successfully. Compose contains only `wms-api`, receives no committed credentials, and connects to external dependencies through environment variables.
