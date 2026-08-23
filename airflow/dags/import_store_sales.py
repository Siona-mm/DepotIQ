"""Import queued store CSV files through DepotIQ's validated backend API."""

from __future__ import annotations

import base64
import os
import shutil
import uuid
from datetime import datetime
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen

from airflow import DAG
from airflow.operators.python import PythonOperator


INCOMING_DIRECTORY = Path("/opt/airflow/imports/incoming")
PROCESSED_DIRECTORY = Path("/opt/airflow/imports/processed")
FAILED_DIRECTORY = Path("/opt/airflow/imports/failed")


def multipart_payload(file_path: Path) -> tuple[bytes, str]:
    boundary = f"----DepotIQ{uuid.uuid4().hex}"
    content = file_path.read_bytes()
    body = b"".join(
        [
            f"--{boundary}\r\n".encode(),
            (
                "Content-Disposition: form-data; name=\"file\"; "
                f"filename=\"{file_path.name}\"\r\n"
            ).encode(),
            b"Content-Type: text/csv\r\n\r\n",
            content,
            f"\r\n--{boundary}--\r\n".encode(),
        ]
    )
    return body, boundary


def archive(file_path: Path, destination: Path) -> None:
    destination.mkdir(parents=True, exist_ok=True)
    stamped_name = f"{datetime.now().strftime('%Y%m%dT%H%M%S')}_{file_path.name}"
    shutil.move(str(file_path), destination / stamped_name)


def import_waiting_files() -> None:
    INCOMING_DIRECTORY.mkdir(parents=True, exist_ok=True)
    backend_url = os.environ["DEPOTIQ_BACKEND_URL"].rstrip("/")
    credentials = (
        f"{os.environ['DEPOTIQ_IMPORT_USERNAME']}:"
        f"{os.environ['DEPOTIQ_IMPORT_PASSWORD']}"
    )
    authorization = base64.b64encode(credentials.encode()).decode()

    for file_path in sorted(INCOMING_DIRECTORY.glob("*.csv")):
        body, boundary = multipart_payload(file_path)
        request = Request(
            f"{backend_url}/api/imports/sales-records",
            data=body,
            method="POST",
            headers={
                "Authorization": f"Basic {authorization}",
                "Content-Type": f"multipart/form-data; boundary={boundary}",
                "Content-Length": str(len(body)),
            },
        )
        try:
            with urlopen(request, timeout=120) as response:
                if response.status < 200 or response.status >= 300:
                    raise RuntimeError(f"DepotIQ import returned HTTP {response.status}")
            archive(file_path, PROCESSED_DIRECTORY)
        except (HTTPError, URLError, RuntimeError) as error:
            archive(file_path, FAILED_DIRECTORY)
            raise RuntimeError(f"Import failed for {file_path.name}: {error}") from error


with DAG(
    dag_id="import_store_sales",
    description="Import queued CSV store sales files into DepotIQ.",
    start_date=datetime(2026, 1, 1),
    schedule="*/5 * * * *",
    catchup=False,
    tags=["depotiq", "imports"],
) as dag:
    PythonOperator(task_id="import_waiting_files", python_callable=import_waiting_files)
