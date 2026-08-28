# DepotIQ

## Run with Docker

### Requirements

Before starting, ensure that:

* Docker Desktop is installed and running.
* WSL integration is enabled for your Ubuntu distribution.

### Start the application

From WSL, open the project directory and start the application:

```bash
cd ~/DepotIQ
docker compose up --build -d
```

During the first run, the `model-trainer` service:

1. Generates the synthetic retail dataset.
2. Creates leakage-safe 3-, 7-, 14-, and 30-day targets.
3. Trains the four demand-forecasting models.
4. Stores the generated data and trained models in Docker volumes.

The first training run may take a few minutes. Follow its progress with:

```bash
docker compose logs -f model-trainer
```

Later starts reuse the existing models unless a required artifact is missing or the training pipeline has changed.

### Check the services

```bash
docker compose ps -a
```

After a successful startup:

* `model-trainer` should show `Exited (0)`.
* The remaining application services should show `running` or `healthy`.

### Open the application

Visit:

```text
http://localhost:5173
```

Use the development credentials:

```text
Username: admin
Password: admin123
```

## Service URLs

| Service     | URL                     |
| ----------- | ----------------------- |
| Frontend    | `http://localhost:5173` |
| Backend API | `http://localhost:8081` |
| ML service  | `http://localhost:8000` |
| PostgreSQL  | `localhost:5432`        |

The backend uses port `8081`, leaving port `8080` available for another local project.

## Logs and Health Checks

### View service logs

```bash
docker compose logs --tail=100 model-trainer
docker compose logs --tail=100 backend
docker compose logs --tail=100 frontend
docker compose logs --tail=100 ml-service
```

### Run health checks

```bash
curl -u admin:admin123 http://localhost:8081/api/auth/me
curl http://localhost:8000/health
curl http://localhost:8000/recommendations/models
```

The models endpoint should report `"artifactAvailable": true` for all four planning horizons.

## Restart After Code Changes

Rebuild and restart the complete application:

```bash
docker compose up --build -d
```

Restart an individual application service:

```bash
docker compose restart backend
docker compose restart frontend
docker compose restart ml-service
```

## Retrain the Models

Training runs automatically when required. To force complete retraining:

```bash
docker compose run --rm model-trainer python scripts/bootstrap_models.py --force
docker compose restart ml-service backend
```

## Stop the Application

```bash
docker compose down
```

This preserves the PostgreSQL database, generated ML data, and trained models.

Do not add `-v` unless you intentionally want to delete all these Docker volumes. After deletion, the next startup will regenerate the data and retrain the models.

## Troubleshooting

If a service does not start, inspect its logs first:

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

---

## Copyright

Copyright © 2026 DepotIQ Contributors. All rights reserved.

The source code and materials in this repository are publicly available for viewing, academic evaluation, and portfolio purposes only.

No permission is granted to copy, modify, distribute, sublicense, or reuse this source code or substantial portions of it in another project without prior written permission from the respective copyright holder(s).
