# Docker Runtime Design

## Chosen approach

WMS runs as one `wms-api` container. Docker Compose builds the application locally and loads its runtime configuration from the untracked `.env` file. PostgreSQL, Redis, and Mailpit remain external services on HomeServer; Compose does not create substitutes for them.

The Dockerfile uses two stages. The build stage uses Java 17 and the Gradle Wrapper to produce the Spring Boot executable JAR. The runtime stage uses a Java 17 JRE, contains only the JAR, creates `/app/exports`, and runs as a non-root user. This keeps build tools out of the deployed image.

`docker-compose.yml` maps `${WMS_SERVER_PORT:-8081}` from the host to the same container port, mounts `./exports` at `/app/exports`, loads `.env` through `env_file`, and sets `restart: unless-stopped`.

`.dockerignore` excludes Git metadata, Gradle caches, build outputs, IDE files, `.env`, and generated exports. The image therefore never receives local credentials or generated report files in its build context.

## Database design

No database schema, migration, or seed change is required. `WMS_DB_*`, `WMS_REDIS_*`, and `WMS_MAIL_*` remain runtime environment variables and must identify services reachable from the Docker host.

## OpenAPI design

No endpoint or OpenAPI schema changes are required. Swagger remains available through the existing application route:

```text
http://localhost:${WMS_SERVER_PORT}/swagger-ui/index.html
```

## Testing strategy

The existing JUnit suite continues to run with `./gradlew clean test`. Docker verification builds the image with `docker compose build`, starts it with `docker compose up -d`, and inspects the application log with `docker compose logs --tail=100 wms-api`. Manual verification opens Swagger at the mapped application port after the external HomeServer dependencies are reachable.
