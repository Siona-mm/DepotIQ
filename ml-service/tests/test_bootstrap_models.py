import json

from scripts.bootstrap_models import REQUIRED_MODELS, artifacts_are_current, write_manifest


def test_artifacts_are_current_requires_data_models_and_matching_manifest(tmp_path):
    models_dir = tmp_path / "models"
    models_dir.mkdir()
    synthetic_data = tmp_path / "synthetic.csv"
    processed_data = tmp_path / "processed.csv"
    manifest_path = models_dir / ".training-manifest.json"
    fingerprint = "current-training-code"

    assert not artifacts_are_current(
        fingerprint,
        models_dir,
        synthetic_data,
        processed_data,
        manifest_path,
    )

    synthetic_data.touch()
    processed_data.touch()
    for filename in REQUIRED_MODELS:
        (models_dir / filename).touch()
    write_manifest(fingerprint, manifest_path)

    assert artifacts_are_current(
        fingerprint,
        models_dir,
        synthetic_data,
        processed_data,
        manifest_path,
    )


def test_artifacts_are_current_rejects_stale_manifest(tmp_path):
    models_dir = tmp_path / "models"
    models_dir.mkdir()
    synthetic_data = tmp_path / "synthetic.csv"
    processed_data = tmp_path / "processed.csv"
    manifest_path = models_dir / ".training-manifest.json"
    synthetic_data.touch()
    processed_data.touch()
    for filename in REQUIRED_MODELS:
        (models_dir / filename).touch()
    manifest_path.write_text(json.dumps({"fingerprint": "old"}), encoding="utf-8")

    assert not artifacts_are_current(
        "new",
        models_dir,
        synthetic_data,
        processed_data,
        manifest_path,
    )
