# Local Setup Guide

## Prerequisites

- Java 17 through 21 and Maven 3.8+
- Node.js and npm
- Python 3.10+ for the ML service
- PostgreSQL

Verify the Java and Maven installation:

```powershell
java -version
mvn -version
```

## Database

Create a PostgreSQL user and database named `depotiq`. The default local credentials are:

```text
Database: depotiq
Username: depotiq
Password: depotiq
```

If `psql` is installed, run these commands as a PostgreSQL administrator:

```sql
CREATE USER depotiq WITH PASSWORD 'depotiq';
CREATE DATABASE depotiq OWNER depotiq;
```

If the user already exists, reset the password instead:

```sql
ALTER USER depotiq WITH PASSWORD 'depotiq';
```

When the backend starts, Flyway creates the schema and loads the built-in demo dataset automatically.

## Start the application

Open separate PowerShell terminals.

### Backend

```powershell
cd C:\Users\Lenovo\DepotIQ\backend
mvn spring-boot:run
```

The API runs at `http://localhost:8080`.

### Frontend

```powershell
cd C:\Users\Lenovo\DepotIQ\frontend
npm.cmd install
npm.cmd run dev
```

Use `npm.cmd` if PowerShell blocks `npm.ps1` due to its execution policy. Open `http://localhost:5173` when Vite is ready.

### ML service (optional for browsing seeded data)

```powershell
cd C:\Users\Lenovo\DepotIQ\ml-service
.\.venv\Scripts\Activate.ps1
uvicorn main:app --reload --port 8000
```

The ML service runs at `http://localhost:8000`. It is required for live ML synchronization; the seeded forecasts and recommendations are available without it.

## Local test accounts

| User | Password | Access |
|---|---|---|
| `admin` | `admin123` | Full access |
| `manager` | `manager123` | Operational workflow without imports or settings changes |
| `viewer` | `viewer123` | Read-only operational access |

Passwords can be overridden through `DEPOTIQ_ADMIN_PASSWORD`, `DEPOTIQ_MANAGER_PASSWORD`, and `DEPOTIQ_VIEWER_PASSWORD`.

## Environment variables

| Variable | Default |
|---|---|
| `DEPOTIQ_DB_URL` | `jdbc:postgresql://localhost:5432/depotiq` |
| `DEPOTIQ_DB_USERNAME` | `depotiq` |
| `DEPOTIQ_DB_PASSWORD` | `depotiq` |
| `DEPOTIQ_ML_SERVICE_URL` | `http://localhost:8000` |
| `DEPOTIQ_CORS_ALLOWED_ORIGINS` | local frontend origins |
| `VITE_API_BASE_URL` | empty (uses the Vite API proxy) |

## Verify before a pull request

```powershell
cd C:\Users\Lenovo\DepotIQ\backend
mvn test

cd C:\Users\Lenovo\DepotIQ\frontend
npm.cmd run build
```
