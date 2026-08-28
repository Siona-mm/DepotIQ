"""Generate synthetic data and train every model required by the API."""

from __future__ import annotations

import argparse
import hashlib
import json
import subprocess
import sys
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[2]
MODELS_DIR = PROJECT_ROOT / "ml-service" / "models"
SYNTHETIC_DATA = PROJECT_ROOT / "data" / "synthetic" / "depotiq_synthetic_retail.csv"
PROCESSED_DATA = PROJECT_ROOT / "data" / "processed" / "depotiq_multiday_targets.csv"
MANIFEST_PATH = MODELS_DIR / ".training-manifest.json"

REQUIRED_MODELS = (
    "synthetic_honest_3_day_model.joblib",
    "demand_model_7_day.joblib",
    "large_store_14_day_demand_model.joblib",
    "demand_model_30_day.joblib",
)

PIPELINE_SCRIPTS = (
    "ml-service/scripts/generate_synthetic_retail_data.py",
    "ml-service/notebooks/08_create_multiday_targets.py",
    "ml-service/notebooks/09_train_3_day_demand_model.py",
    "ml-service/notebooks/12_train_7_day_demand_model.py",
    "ml-service/notebooks/11_train_14_day_large_store_model.py",
    "ml-service/notebooks/13_train_30_day_demand_model.py",
)

FINGERPRINT_INPUTS = PIPELINE_SCRIPTS + ("ml-service/requirements.txt",)


def training_fingerprint(project_root: Path = PROJECT_ROOT) -> str:
    digest = hashlib.sha256()
    for relative_path in FINGERPRINT_INPUTS:
        path = project_root / relative_path
        digest.update(relative_path.encode("utf-8"))
        digest.update(path.read_bytes())
    return digest.hexdigest()


def artifacts_are_current(
    fingerprint: str,
    models_dir: Path = MODELS_DIR,
    synthetic_data: Path = SYNTHETIC_DATA,
    processed_data: Path = PROCESSED_DATA,
    manifest_path: Path = MANIFEST_PATH,
) -> bool:
    required_paths = [models_dir / filename for filename in REQUIRED_MODELS]
    if not synthetic_data.is_file() or not processed_data.is_file():
        return False
    if not all(path.is_file() for path in required_paths):
        return False
    try:
        manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    except (FileNotFoundError, json.JSONDecodeError, OSError):
        return False
    return manifest.get("fingerprint") == fingerprint


def run_pipeline(project_root: Path = PROJECT_ROOT) -> None:
    for relative_path in PIPELINE_SCRIPTS:
        print(f"\n=== Running {relative_path} ===", flush=True)
        subprocess.run(
            [sys.executable, str(project_root / relative_path)],
            cwd=project_root,
            check=True,
        )


def write_manifest(fingerprint: str, manifest_path: Path = MANIFEST_PATH) -> None:
    manifest_path.parent.mkdir(parents=True, exist_ok=True)
    temporary_path = manifest_path.with_suffix(".tmp")
    temporary_path.write_text(
        json.dumps(
            {
                "fingerprint": fingerprint,
                "models": list(REQUIRED_MODELS),
            },
            indent=2,
        )
        + "\n",
        encoding="utf-8",
    )
    temporary_path.replace(manifest_path)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--force",
        action="store_true",
        help="Regenerate the dataset and retrain every model.",
    )
    args = parser.parse_args()

    fingerprint = training_fingerprint()
    if not args.force and artifacts_are_current(fingerprint):
        print("ML training artifacts are current; skipping training.")
        return

    print("Preparing DepotIQ ML artifacts. First-run training may take a few minutes.")
    run_pipeline()

    if not all((MODELS_DIR / filename).is_file() for filename in REQUIRED_MODELS):
        raise RuntimeError("Training completed without producing every required model artifact")
    if not SYNTHETIC_DATA.is_file() or not PROCESSED_DATA.is_file():
        raise RuntimeError("Training completed without producing the required datasets")

    write_manifest(fingerprint)
    print("\nAll DepotIQ ML models are trained and ready.")


if __name__ == "__main__":
    main()
