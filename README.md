# DepotIQ

DepotIQ is a depot-level demand forecasting and shipment recommendation web app.

It helps a central depot predict store-level product demand, track depot inventory, and recommend how much stock should be dispatched to each store before shelves run low.

## Tech Stack

- **Frontend:** React + Vite
- **Backend:** Java 17, Spring Boot, Maven
- **Database:** PostgreSQL
- **ML service:** Python, FastAPI, pandas, scikit-learn, joblib
- **Version control:** Git + GitHub

## Project Structure

- `frontend/`: React web app and dashboard UI
- `backend/`: Spring Boot REST API and business logic
- `ml-service/`: Python model training and prediction API
- `data/`: local raw and processed datasets, ignored by Git
- `docs/`: project and API documentation
- `reports/`: market research and presentation material

## Datasets

DepotIQ keeps two dataset paths:

- The original Kaggle dataset in `data/raw/` for research and leakage investigation.
- The generated synthetic dataset in `data/synthetic/` for the final forecasting models.

The original Kaggle experiment is documented because it helped reveal data
leakage and weak time-series signal in the source data. The current leakage-safe
models, including the 7-day demand model, use the reproducible synthetic dataset.

Generated datasets and trained model artifacts are **not committed to GitHub**
because they can be recreated locally. The repo only keeps the folder structure
with `.gitkeep` files.

### Synthetic Dataset For Model Training

The synthetic dataset contains 100,000 rows, 10 stores, 25 products, and 400
days. It includes prices, promotions, weather, seasonality, inventory,
warehouses, delivery times, and stockouts.

Generate it locally from the repo root:

```bash
cd ml-service
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
python scripts/generate_synthetic_retail_data.py
```

Then create the leakage-safe multi-day targets:

```bash
python notebooks/08_create_multiday_targets.py
```

Train the 7-day demand model from the same `ml-service` directory:

```bash
python notebooks/12_train_7_day_demand_model.py
```

This saves the local model artifact here:

```text
ml-service/models/demand_model_7_day.joblib
```

### Original Kaggle Dataset For Research

- **Dataset name:** Retail Store Inventory Forecasting Dataset
- **Source:** Kaggle
- **License:** CC0 Public Domain
- **URL:** https://www.kaggle.com/datasets/anirudhchauhan/retail-store-inventory-forecasting-dataset/data

This dataset is kept for the original exploration and leakage investigation. It
is not used for the final leakage-safe forecasting models.

#### Option 1: Download With The Helper Script

If you have a Kaggle account, you can use the Kaggle CLI and the local helper
script to download and unzip the dataset automatically.

Install the Kaggle CLI:

```bash
python -m pip install kaggle
```

Authenticate with Kaggle by setting environment variables:

```bash
export KAGGLE_USERNAME="your_kaggle_username"
export KAGGLE_KEY="your_kaggle_api_key"
```

You can place those same variables in a local `.env` file at the repo root if
you prefer. The `.env` file is ignored by Git.

Then run the downloader from the repo root:

```bash
./ml-service/scripts/download_retail_dataset.sh
```

The script downloads the dataset into `data/raw/`, unzips it, and checks that
this file exists:

```text
data/raw/retail_store_inventory.csv
```

You can also authenticate with Kaggle by placing `kaggle.json` at
`~/.kaggle/kaggle.json` instead of setting environment variables.

#### Option 2: Download Manually

Download the dataset from Kaggle, then place the CSV here:

```text
data/raw/retail_store_inventory.csv
```

If the file downloads to your Windows Downloads folder, copy it into WSL like this:

```bash
cp /mnt/c/Users/User/Downloads/archive.zip ~/DepotIQ/data/raw/
cd ~/DepotIQ/data/raw
unzip archive.zip
```

After unzipping, confirm the CSV exists:

```bash
ls -la ~/DepotIQ/data/raw
```

Expected file:

```text
retail_store_inventory.csv
```

Do not commit the downloaded `.zip` or `.csv` files. They are ignored by `.gitignore`.

## Prerequisites

Install these before running the project:

- Git
- VS Code
- WSL with Ubuntu
- Java 17 JDK
- Maven
- Node.js + npm
- Python 3
- PostgreSQL

Check installed versions:

```bash
java -version
javac -version
mvn -version
node -v
npm -v
python3 --version
psql --version
```

If Java or Maven is missing in WSL:

```bash
sudo apt update
sudo apt install openjdk-17-jdk maven
```

If PostgreSQL is missing:

```bash
sudo apt install postgresql postgresql-contrib
```

## Clone The Repo

From WSL:

```bash
cd ~
git clone https://github.com/Siona-mm/DepotIQ.git
cd DepotIQ
```

If you already have the repo:

```bash
cd ~/DepotIQ
git pull
```

## Open In VS Code

Open the workspace file:

```bash
cd ~/DepotIQ
code DepotIQ.code-workspace
```

When VS Code asks to install recommended extensions, accept it.

Recommended extensions are configured in:

```text
.vscode/extensions.json
```

## Database Setup

Start PostgreSQL:

```bash
sudo service postgresql start
```

Create the local development database and user:

```bash
sudo -u postgres psql
```

Inside the `psql` prompt, run:

```sql
CREATE USER depotiq WITH PASSWORD 'depotiq';
CREATE DATABASE depotiq OWNER depotiq;
GRANT ALL PRIVILEGES ON DATABASE depotiq TO depotiq;
\q
```

If the user already exists:

```sql
ALTER USER depotiq WITH PASSWORD 'depotiq';
```

If the database already exists:

```sql
ALTER DATABASE depotiq OWNER TO depotiq;
GRANT ALL PRIVILEGES ON DATABASE depotiq TO depotiq;
```

Test the database login from the normal WSL terminal:

```bash
PGPASSWORD=depotiq psql -h localhost -U depotiq -d depotiq -c "select 1;"
```

Expected output includes:

```text
?column?
----------
        1
```

The backend database config is in:

```text
backend/src/main/resources/application.properties
```

Default local credentials:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/depotiq
spring.datasource.username=depotiq
spring.datasource.password=depotiq
```

## Run The Backend

From WSL:

```bash
cd ~/DepotIQ/backend
mvn spring-boot:run
```

If it works, the logs should include:

```text
Tomcat started on port 8080
Started DepotIqApplication
```

Backend URL:

```text
http://localhost:8080
```

The app may show a default error or security page at first. That is okay while endpoints are still being built.

## Run The Frontend

From WSL:

```bash
cd ~/DepotIQ/frontend
npm install
npm run dev
```

Vite will print a local URL, usually:

```text
http://localhost:5173
```

## Run The ML Service

From WSL:

```bash
cd ~/DepotIQ/ml-service
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
uvicorn main:app --reload --port 8000
```

ML service health check:

```text
http://localhost:8000/health
```

Expected response:

```json
{"status":"ok"}
```

## VS Code Tasks

Useful tasks are configured in `.vscode/tasks.json`:

- `postgres: start`
- `backend: run`
- `frontend: install`
- `frontend: dev`
- `ml-service: venv`
- `ml-service: install`
- `ml-service: run`

To run a task:

```text
Terminal -> Run Task...
```

Suggested first-time order:

1. `postgres: start`
2. `frontend: install`
3. `ml-service: venv`
4. `ml-service: install`
5. `backend: run`
6. `frontend: dev`
7. `ml-service: run`

## Common Problems

### `java: command not found`

Install Java:

```bash
sudo apt update
sudo apt install openjdk-17-jdk
```

### `mvn: command not found`

Install Maven:

```bash
sudo apt install maven
```

### `password authentication failed for user "depotiq"`

Reset the local Postgres password:

```bash
sudo -u postgres psql
```

Then:

```sql
ALTER USER depotiq WITH PASSWORD 'depotiq';
\q
```

Test again:

```bash
PGPASSWORD=depotiq psql -h localhost -U depotiq -d depotiq -c "select 1;"
```

### `database "depotiq" does not exist`

Create it:

```bash
sudo -u postgres psql
```

Then:

```sql
CREATE DATABASE depotiq OWNER depotiq;
\q
```

### Docker is not running

Docker is optional for now. If Docker is unavailable, use the local PostgreSQL setup above.

## Git Workflow

Before starting work:

```bash
git pull
```

After making changes:

```bash
git status
git add .
git commit -m "Describe your change"
git push
```

Use clear commit messages, for example:

```text
Add product model
Create dashboard layout
Set up ML health endpoint
```
