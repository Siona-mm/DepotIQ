```markdown
# DepotIQ

## Run With Docker

### Requirements

- Docker Desktop installed and running
- WSL integration enabled for your Ubuntu distribution

### Start the application

From WSL, run:

```bash
cd ~/DepotIQ
docker compose up --build -d
```

On the first run, the `model-trainer` service:

1. Generates the deterministic synthetic retail dataset.
2. Creates leakage-safe 3-, 7-, 14-, and 30-day targets.
3. Trains the four demand-forecasting models.
4. Stores the generated data and models in Docker volumes.

First-run training can take a few minutes. Follow its progress with:

```bash
docker compose logs -f model-trainer
```

Later starts reuse the trained models unless the training pipeline changes or an artifact is missing.

Check the services:

```bash
docker compose ps -a
```

After successful startup, `model-trainer` should show `Exited (0)` and the application services should be running or healthy.

Open DepotIQ at:

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

The backend uses port `8081` so port `8080` remains available for another local project.

### Check service logs

```bash
docker compose logs --tail=100 model-trainer
docker compose logs --tail=100 backend
docker compose logs --tail=100 frontend
docker compose logs --tail=100 ml-service
```

Useful health checks:

```bash
curl -u admin:admin123 http://localhost:8081/api/auth/me
curl http://localhost:8000/health
curl http://localhost:8000/recommendations/models
```

The models response should report `"artifactAvailable": true` for all four planning horizons.

### Restart after code changes

Rebuild and restart the full stack:

```bash
docker compose up --build -d
```

Restart one application service:

```bash
docker compose restart backend
docker compose restart frontend
docker compose restart ml-service
```

### Retrain the models

Training runs automatically when required. To force complete retraining:

```bash
docker compose run --rm model-trainer python scripts/bootstrap_models.py --force
docker compose restart ml-service backend
```

### Stop the application

```bash
docker compose down
```

This preserves the PostgreSQL data, generated ML data, and trained models. Do not add `-v` unless you intentionally want to delete all three Docker volumes. The next startup will recreate the data and retrain the models.

### Common problem: a service does not start

Check the affected service logs first:

```bash
docker compose logs --tail=100 model-trainer
docker compose logs --tail=100 backend
docker compose logs --tail=100 frontend
docker compose logs --tail=100 ml-service
```

Then rebuild the affected service. For example:

```bash
docker compose up --build -d backend
```
