# Docker local setup

Docker Compose starts the DepotIQ frontend, backend, ML service, and PostgreSQL database together. It is an alternative to running each service in a separate terminal.

## Prerequisites

Install [Docker Desktop](https://www.docker.com/products/docker-desktop/) and make sure it is running. On Windows, enable the WSL 2 backend during Docker Desktop setup.

From the project root, verify Docker is available:

```powershell
docker --version
docker compose version
```

## Start the application

From the repository root:

```powershell
docker compose up --build
```

The first build downloads the container images and dependencies, so it can take a few minutes. Later starts are faster.

Open the application at [http://localhost:5173](http://localhost:5173).

The individual services are also available at:

| Service | URL |
| --- | --- |
| Frontend | `http://localhost:5173` |
| Backend API | `http://localhost:8080` |
| ML health check | `http://localhost:8000/health` |
| PostgreSQL | `localhost:5432` |

The default accounts are `admin` / `admin123`, `manager` / `manager123`, and `viewer` / `viewer123`.

## Stop and restart

Stop the running services with `Ctrl+C`, then run:

```powershell
docker compose down
```

The PostgreSQL data is kept in the `depotiq-postgres-data` Docker volume, so it is available when you start again.

To start without rebuilding unchanged images:

```powershell
docker compose up
```

## Working on the project afterwards

You can edit the project normally while Docker is running.

- Frontend source changes refresh automatically through Vite.
- After changing backend or ML service code, rebuild and restart the affected container:

```powershell
docker compose up --build backend ml-service
```

- After changing dependencies or a Dockerfile, rebuild the related service with the same command.
- Flyway database migrations run automatically when the backend starts.

## ML model data

The ML service can start and respond to its health check without local model artifacts. To generate recommendations from the training dataset, keep the required local files in the existing ignored locations:

```text
data/raw/retail_store_inventory.csv
ml-service/models/
```

These files are copied into the ML image when they are available locally; they are intentionally not committed to Git.

## Reset the local Docker database

Use this only when you want to delete the Docker-managed local database and start with fresh Flyway seed data:

```powershell
docker compose down -v
docker compose up --build
```

`-v` deletes the Docker PostgreSQL volume. It does not delete files in the repository.

## Common issues

- **Port already in use:** stop the local process using ports 5173, 8080, 8000, or 5432, then restart Compose.
- **Docker command not found:** install Docker Desktop, close and reopen PowerShell, then start Docker Desktop.
- **Frontend cannot reach the backend:** wait until the backend logs show that Spring Boot has started, then refresh the browser.
