# Airflow Store Data Ingestion

Airflow is optional. The normal application stack runs without it. Enable this profile when DepotIQ should import CSV files from an external-system inbox on a schedule.

## Pipeline

1. An ERP, POS, warehouse export, or a team member places a CSV in `data/imports/incoming/`.
2. Airflow checks the inbox every five minutes.
3. The DAG posts each file to DepotIQ's existing validated import API.
4. The backend validates and normalizes rows, then upserts sales and inventory data in PostgreSQL.
5. The backend records an audit log and refreshes demand forecasts and recommendations using the saved model. It does not retrain the model.
6. Airflow moves successful files to `data/imports/processed/`. Failed files move to `data/imports/failed/` for review.

## Run Airflow

Start DepotIQ and the optional Airflow services:

```bash
docker compose --profile airflow up -d --build
```

Open Airflow at `http://localhost:8082`.

Development login:

```text
Username: admin
Password: admin123
```

The DAG is named `import_store_sales`. It runs every five minutes, or you can trigger it manually in the Airflow UI.

## Test an Import

Copy a valid CSV into the inbox, then trigger `import_store_sales` or wait for its next run:

```bash
cp frontend/public/sample_sales_inventory_import.csv data/imports/incoming/
```

Use the DAG task log to inspect failures. A failed CSV is preserved in `data/imports/failed/` rather than discarded.
