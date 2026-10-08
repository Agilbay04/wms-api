GRADLE := ./gradlew
COMPOSE := docker compose

.DEFAULT_GOAL := help
.PHONY: help check-env run test integration-test build docker-build docker-up docker-logs docker-down swagger

help:
	@printf '%s\n' 'Available commands:' \
		'  make run          Start Spring Boot locally using .env' \
		'  make test         Run all unit tests' \
		'  make integration-test Run opt-in migration, inbound, and inventory integration tests' \
		'  make build        Build the executable JAR' \
		'  make docker-build Build the Docker image' \
		'  make docker-up    Start wms-api with Docker Compose' \
		'  make docker-logs  Follow wms-api logs' \
		'  make docker-down  Stop wms-api' \
		'  make swagger      Print the Swagger UI URL'

check-env:
	@test -f .env || { echo "Missing .env. Copy .env.example and configure it first."; exit 1; }

run: check-env
	@set -a; . ./.env; set +a; $(GRADLE) bootRun

test:
	$(GRADLE) clean test

integration-test: check-env
	@set -a; . ./.env; set +a; \
	if [ "$${WMS_TEST_DB_NAME:-}" != "wms-integration-test" ]; then \
		echo "WMS_TEST_DB_NAME must be wms-integration-test."; exit 1; \
	fi; \
	if [ -z "$${WMS_TEST_DB_HOST:-}" ] || [ -z "$${WMS_TEST_DB_USERNAME:-}" ]; then \
		echo "Set WMS_TEST_DB_HOST and WMS_TEST_DB_USERNAME in .env first."; exit 1; \
	fi; \
	$(GRADLE) integrationTest --rerun-tasks --tests '*FlywayMigrationIntegrationTest' --tests '*InboundIntegrationTest' --tests '*StockConcurrencyIntegrationTest' --tests '*StockQueryIntegrationTest'

build:
	$(GRADLE) bootJar

docker-build: check-env
	$(COMPOSE) build

docker-up: check-env
	$(COMPOSE) up -d --build

docker-logs: check-env
	$(COMPOSE) logs --tail=100 -f wms-api

docker-down: check-env
	$(COMPOSE) down

swagger: check-env
	@set -a; . ./.env; set +a; printf 'http://localhost:%s/swagger-ui/index.html\n' "$${WMS_SERVER_PORT:-8081}"
