# Warehouse Management API

A Spring Boot API for warehouse, inventory, inbound, transfer, adjustment, outbound, reporting, and export workflows.

## Requirements

- Java 17
- Access to the existing PostgreSQL and Redis services
- Access to Mailpit SMTP for export notifications
- `make` (optional, for project shortcuts)

The app connects to services already running on your HomeServer; Docker Compose is not required.

## Configure

Copy the template and fill in the application service credentials and host names:

```sh
cp .env.example .env
```

Set `SPRING_PROFILES_ACTIVE=dev`, then configure `WMS_DB_*`, `WMS_REDIS_*`, `WMS_MAIL_*`, and `WMS_JWT_SECRET`. Configure `WMS_SEED_ADMIN_EMAIL`, `WMS_SEED_ADMIN_NAME`, and `WMS_SEED_ADMIN_PASSWORD` for the development admin user. Leave `SEEDER_ENABLE_DB_SETUP=false` unless you intentionally want the dev seeder to write sample master data.

Keep application credentials in `WMS_DB_*`. Integration tests use the separate `WMS_TEST_DB_*` variables and require the dedicated database name `wms-integration-test`; the Makefile validates this before running them. Never use application database credentials for the integration test database.

## Run

```sh
make run
```

Flyway applies pending migrations when the application starts. The default local API port is `8081`; set `WMS_SERVER_PORT` in `.env` if that port is occupied.

Open Swagger UI at [http://localhost:8081/swagger-ui/index.html](http://localhost:8081/swagger-ui/index.html). Use **Authorize** with the access token returned by login. The generated OpenAPI document is at `/v3/api-docs`; the checked-in contract is [docs/openapi/openapi.yaml](docs/openapi/openapi.yaml).

## Run with Docker

Docker Compose starts the API and an Nginx reverse proxy. PostgreSQL, Redis, and Mailpit must already be running and reachable from the Docker host; Compose does not create these services.

1. Install Docker Engine and the Docker Compose plugin. Confirm both commands work:

   ```sh
   docker --version
   docker compose version
   ```

2. Create the environment file:

   ```sh
   cp .env.example .env
   ```

   Edit `.env` and set `SPRING_PROFILES_ACTIVE=dev`, `WMS_DB_*`, `WMS_REDIS_*`, `WMS_MAIL_*`, and `WMS_JWT_SECRET`. Set `WMS_DB_HOST`, `WMS_REDIS_HOST`, and `WMS_MAIL_HOST` to an IP address or hostname reachable from Docker. Do not use `localhost` for services running on the HomeServer: inside a container, `localhost` refers to that container. Keep `SEEDER_ENABLE_DB_SETUP=false` unless you intend to seed the configured development database.

   `WMS_NGINX_PORT` is the host port used to access the API through Nginx (default `8081`). `WMS_SERVER_PORT` applies to local `make run`; in Docker, the API listens on internal port `8081` and is not published directly to the host.

3. From the project root, build the API image and start the API and Nginx containers:

   ```sh
   make docker-up
   ```

   This is equivalent to `docker compose up -d --build`. Compose builds the API image and downloads the Nginx image if needed. On startup, Flyway applies pending database migrations. To build the API image without starting the containers, run `make docker-build` first.

4. Confirm both containers are running and inspect startup logs:

   ```sh
   docker compose ps
   make docker-logs
   ```

   If the API cannot connect to PostgreSQL or Redis, check the corresponding host, port, and credentials in `.env`.

5. Open Swagger UI through Nginx at [http://localhost:8081/swagger-ui/index.html](http://localhost:8081/swagger-ui/index.html). If you changed `WMS_NGINX_PORT`, replace `8081` with that value. Nginx accepts HTTP and forwards requests to the API with the original host and protocol headers. For HTTPS, terminate TLS in front of this Compose stack or configure certificates in Nginx. To call secured endpoints, log in through `POST /api/v1/auth/login`, then enter the returned access token using Swagger UI's **Authorize** button.

6. Stop the API and Nginx when finished:

   ```sh
   make docker-down
   ```

   This stops and removes the containers but preserves `./exports`, the `wms_seed_results` Docker volume, and data in the external PostgreSQL, Redis, and Mailpit services. Export files are available under `./exports` on the host.

## Learn the API flow

Import [docs/postman/warehouse-management.postman_collection.json](docs/postman/warehouse-management.postman_collection.json) into Postman. Set `base_url`, `admin_email`, and `admin_password`, run **Authentication → Login**, then follow the folders in order. Approval requests use `reviewer_token`, so set `reviewer_email` and `reviewer_password` for a different account with the required approval permissions and warehouse access. A record creator cannot approve or reject their own record.

1. Master data: create a warehouse, category, product, and location.
2. Inbound: receive items, submit, approve, and put them away.
3. Stock transfer: move stock between locations and approve it.
4. Adjustment: create and approve a stock correction.
5. Outbound: pick stock, submit, and approve it.
6. Reporting and export: inspect balances and movements, queue a CSV export, poll its status, and download it.

The collection saves the access token, refresh token, and resource IDs as collection variables. JSON bodies and response properties use `snake_case`.

### Error responses

Errors use the same envelope as successful responses: `success` is `false`, `code` is the HTTP status, `data` is `null`, and `errors` contains field messages when available. Bean validation failures return `422`; for example, a blank `refresh_token` is reported under `errors.refresh_token`. Workflow conflicts return `409` with a stable code such as `INBOUND_NOT_REVIEWABLE`, `TRANSFER_NOT_REVIEWABLE`, or `INSUFFICIENT_STOCK`. A creator attempting to review their own request receives `403` with a `*_SELF_REVIEW_FORBIDDEN` code. An export request returns `202`; its status can be `QUEUED`, `RUNNING`, `COMPLETED`, or `FAILED`.

```json
{
  "success": false,
  "code": 422,
  "message": "Request validation failed.",
  "data": null,
  "errors": { "refresh_token": "must not be blank" }
}
```

## Tests

```sh
make test
```

The opt-in integration suite uses PostgreSQL, Redis, and Mailpit configured through `WMS_TEST_DB_*` plus the service environment variables. Run it with:

```sh
make integration-test
```

This command requires a dedicated database named exactly `wms-integration-test` and creates isolated temporary schemas for its tests.

## Mailpit

Export jobs send the generated CSV as an email attachment after the background worker completes. Open the Mailpit web UI on the configured Mailpit host at port `8025` (for example, `http://localhost:8025`) and inspect the message. SMTP uses `WMS_MAIL_HOST` and `WMS_MAIL_PORT` (default `1025`). Generated files are stored under `WMS_EXPORT_DIRECTORY` (default `exports/`).

## API and project docs

- [OpenAPI contract](docs/openapi/openapi.yaml)
- [Postman collection](docs/postman/warehouse-management.postman_collection.json)
