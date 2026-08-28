# DepotIQ

## Run With Docker

### Requirements

- Docker Desktop installed and running
- WSL integration enabled for your Ubuntu distribution

### Start the application

From WSL, open the project folder and run:

```bash
cd ~/DepotIQ
docker compose up --build -d
```

Check that every service is running:

```bash
docker compose ps
```

Open DepotIQ in your browser:

```text
http://localhost:5173
```

Development login:

```text
Username: admin
Password: admin123
```

### Service URLs

```text
Frontend:    http://localhost:5173
Backend API: http://localhost:8081
ML service:  http://localhost:8000
PostgreSQL:  localhost:5432
```

The backend is exposed on port `8081` so port `8080` remains available for another local project.

### Check service logs

```bash
docker compose logs --tail=100 backend
docker compose logs --tail=100 frontend
docker compose logs --tail=100 ml-service
```

Useful health checks:

```bash
curl -u admin:admin123 http://localhost:8081/api/auth/me
curl http://localhost:8000/health
```

### Restart after code changes

Rebuild and restart the full stack:

```bash
docker compose up --build -d
```

Restart one service only:

```bash
docker compose restart backend
docker compose restart frontend
docker compose restart ml-service
```

### Stop the application

```bash
docker compose down
```

This keeps the PostgreSQL data volume. Do not run `docker compose down -v` unless you intentionally want to delete all local DepotIQ database data.
Do not add `-v` unless you intentionally want to delete the Docker database,
generated ML data, and trained-model volumes. The next start recreates and
retrains the ML artifacts from source.

### Common problem: a service does not start

Check the service logs first:

```bash
docker compose logs --tail=100 backend
docker compose logs --tail=100 frontend
docker compose logs --tail=100 ml-service
```

Then rebuild the affected service:

```bash
docker compose up --build -d backend
```
