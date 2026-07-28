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

## Dataset

DepotIQ uses the **Retail Store Inventory Forecasting Dataset** from Kaggle.

- **Dataset name:** Retail Store Inventory Forecasting Dataset
- **Source:** Kaggle
- **License:** CC0 Public Domain
- **URL:** https://www.kaggle.com/datasets/anirudhchauhan/retail-store-inventory-forecasting-dataset/data

The dataset is used for demand forecasting and inventory/shipment recommendation modeling.

Raw dataset files are **not committed to GitHub** because data files can be large and should stay local. The repo only keeps the folder structure with `.gitkeep` files.

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

The backend must use Java 17. Java 11 is not supported.

Expected Java output should include version `17`, for example:

```text
openjdk version "17.x"
```

Expected Maven output should show Maven `3.8.0` or newer and Java version `17`.

If Java or Maven is missing in WSL:

```bash
sudo apt update
sudo apt install openjdk-17-jdk maven
```

If `java -version` shows Java 11, install Java 17 and select it:

```bash
sudo apt install openjdk-17-jdk
sudo update-alternatives --config java
sudo update-alternatives --config javac
```

Then choose the Java 17 option.

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

## Database Migrations

DepotIQ uses Flyway migrations so every teammate can create the same database schema.

Migration files live here:

```text
backend/src/main/resources/db/migration/
```

The first migration is:

```text
V1__create_core_schema.sql
```

It creates the core backend tables:

- `stores`
- `products`
- `store_inventory`
- `depot_inventory`
- `sales_records`
- `demand_forecasts`
- `shipment_recommendations`

Teammates only need to create the empty PostgreSQL database and user. When the backend starts, Flyway automatically runs the migrations and creates the tables.

Run migrations by starting the backend:

```bash
cd ~/DepotIQ/backend
mvn spring-boot:run
```

To confirm the tables were created:

```bash
PGPASSWORD=depotiq psql -h localhost -U depotiq -d depotiq -c "\dt"
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

The backend currently allows local development access to `/api/**` endpoints without login.

## Store API

The Store API is the first backend feature endpoint. It lets the frontend read, create, update, and delete stores supplied by the depot.

Main files:

```text
backend/src/main/java/com/depotiq/models/Store.java
backend/src/main/java/com/depotiq/models/StoreType.java
backend/src/main/java/com/depotiq/repositories/StoreRepository.java
backend/src/main/java/com/depotiq/dtos/store/CreateStoreRequest.java
backend/src/main/java/com/depotiq/dtos/store/UpdateStoreRequest.java
backend/src/main/java/com/depotiq/dtos/store/StoreResponse.java
backend/src/main/java/com/depotiq/mappers/StoreMapper.java
backend/src/main/java/com/depotiq/services/StoreService.java
backend/src/main/java/com/depotiq/controllers/StoreController.java
backend/src/main/java/com/depotiq/config/SecurityConfig.java
```

Available endpoints:

```text
GET    /api/stores
GET    /api/stores/{id}
POST   /api/stores
PUT    /api/stores/{id}
DELETE /api/stores/{id}
```

Store type values must use these enum names:

```text
SMALL
MEDIUM
LARGE
WAREHOUSE_STORE
```

To test the Store API, start PostgreSQL first:

```bash
sudo service postgresql start
```

Then run the backend:

```bash
cd ~/DepotIQ/backend
mvn spring-boot:run
```

In a second WSL terminal, get all stores:

```bash
curl http://localhost:8080/api/stores
```

Create a store:

```bash
curl -X POST http://localhost:8080/api/stores \
  -H "Content-Type: application/json" \
  -d '{
    "storeCode": "S011",
    "name": "North Side Store",
    "storeType": "SMALL",
    "region": "North",
    "hasWarehouse": false,
    "storageCapacity": 500,
    "deliveryLeadTimeDays": 2,
    "preferredHorizonDays": 3
  }'
```

Get one store by ID:

```bash
curl http://localhost:8080/api/stores/1
```

Update a store:

```bash
curl -X PUT http://localhost:8080/api/stores/1 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Updated Store Name",
    "storeType": "MEDIUM",
    "region": "North",
    "hasWarehouse": false,
    "storageCapacity": 900,
    "deliveryLeadTimeDays": 3,
    "preferredHorizonDays": 7
  }'
```

Delete a store:

```bash
curl -X DELETE http://localhost:8080/api/stores/1
```

Confirm stores directly in PostgreSQL:

```bash
PGPASSWORD=depotiq psql -h localhost -U depotiq -d depotiq -c "SELECT * FROM stores;"
```

Before opening a pull request for backend API changes, run:

```bash
cd ~/DepotIQ/backend
mvn clean test
```

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

## Run The Complete Forecast Flow

The application uses three development servers. Keep each server running in its
own WSL terminal.

Terminal 1 - ML service:

```bash
cd ~/DepotIQ/ml-service
source .venv/bin/activate
uvicorn main:app --reload --port 8000
```

Terminal 2 - Spring Boot:

```bash
cd ~/DepotIQ/backend
mvn spring-boot:run
```

Terminal 3 - React:

```bash
cd ~/DepotIQ/frontend
npm run dev
```

Open the dashboard at:

```text
http://localhost:5173
```

The first ML request loads the processed dataset and all four model artifacts,
so it can take several seconds. Later requests use an in-memory cache.

Check the trained models:

```bash
curl http://localhost:8000/recommendations/models
```

Preview recommendations generated by FastAPI:

```bash
curl "http://localhost:8000/recommendations?store_code=S001&limit=5"
```

Import matched ML results into PostgreSQL:

```bash
curl -X POST http://localhost:8080/api/ml/sync
```

The sync response reports:

- how many ML rows were received;
- how many forecasts were stored;
- how many shipment recommendations were stored; and
- how many rows were skipped because their store or product is not present in
  the demo database.

The sync is idempotent. Running it again updates the same store, product, date,
and horizon records instead of creating duplicates.

After syncing, verify the saved results:

```bash
curl http://localhost:8080/api/forecasts
curl http://localhost:8080/api/recommendations
```

The recommendation `confidenceLower` and `confidenceUpper` values are
validation-MAE-based expected ranges. They are useful context for operators,
but they are not formal statistical confidence intervals.

## Service Configuration

Local defaults work without environment variables. These variables can
override them:

```text
DEPOTIQ_DB_URL
DEPOTIQ_DB_USERNAME
DEPOTIQ_DB_PASSWORD
DEPOTIQ_ML_SERVICE_URL
DEPOTIQ_CORS_ALLOWED_ORIGINS
VITE_API_BASE_URL
VITE_API_PROXY_TARGET
```

`VITE_API_PROXY_TARGET` defaults to `http://localhost:8080` and is used by the
Vite development server. `VITE_API_BASE_URL` is useful when the built frontend
and backend are hosted on different origins.

## Verification

Run the ML tests:

```bash
cd ~/DepotIQ/ml-service
source .venv/bin/activate
pytest -q
```

Run the backend tests:

```bash
cd ~/DepotIQ/backend
mvn clean test
```

Build the frontend and check dependencies:

```bash
cd ~/DepotIQ/frontend
npm audit --omit=dev
npm run build
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
