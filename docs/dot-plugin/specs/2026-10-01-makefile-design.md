# Makefile Design

## Chosen approach

WMS uses one root `Makefile` as a safe command interface for local Gradle and Docker Compose workflows. The Makefile does not parse, print, or commit credentials. Targets that start the Spring Boot process source the existing `.env` file in the recipe shell; Docker targets pass that file to Docker Compose.

The supported targets are:

| Target | Command | Purpose |
| --- | --- | --- |
| `help` | `make help` | Lists available targets. |
| `run` | `make run` | Sources `.env` and runs `./gradlew bootRun` in the foreground. |
| `test` | `make test` | Runs `./gradlew clean test`. |
| `build` | `make build` | Produces the Spring Boot executable JAR. |
| `docker-build` | `make docker-build` | Builds the API image. |
| `docker-up` | `make docker-up` | Builds when needed and starts only `wms-api` in the background. |
| `docker-logs` | `make docker-logs` | Follows the API container log. |
| `docker-down` | `make docker-down` | Stops the API container. |
| `swagger` | `make swagger` | Prints the Swagger UI URL. |

The Makefile always uses the project's single `.env` file. A preflight target checks that the file exists and returns a clear message otherwise. Environment-specific behavior remains in `application-dev.properties`, `application-stg.properties`, and `application-prod.properties`, selected by `SPRING_PROFILES_ACTIVE`.

## Database design

No database schema or migration changes are required. PostgreSQL, Redis, and Mailpit remain external HomeServer services configured through the selected environment file.

## OpenAPI design

No endpoint or API response change is required. `make swagger` prints the existing Swagger UI route using `WMS_SERVER_PORT`, with `8081` as a fallback.

## Testing strategy

`make help`, `make test`, and `make build` validate the Makefile's Gradle workflows. `make docker-build` validates the Docker workflow using the existing Compose configuration. The environment preflight is checked by running a target with an absent `ENV_FILE` and asserting that it fails without starting the application.
