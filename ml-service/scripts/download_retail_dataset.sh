#!/usr/bin/env bash
set -euo pipefail

DATASET_SLUG="anirudhchauhan/retail-store-inventory-forecasting-dataset"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
RAW_DIR="$REPO_ROOT/data/raw"
EXPECTED_CSV="$RAW_DIR/retail_store_inventory.csv"
ZIP_PATH="$RAW_DIR/retail-store-inventory-forecasting-dataset.zip"
ENV_FILE="$REPO_ROOT/.env"

if [[ -f "$ENV_FILE" ]]; then
  set -a
  # shellcheck source=/dev/null
  source "$ENV_FILE"
  set +a
fi

if [[ -z "${KAGGLE_KEY:-}" && -n "${KAGGLE_API_TOKEN:-}" ]]; then
  export KAGGLE_KEY="$KAGGLE_API_TOKEN"
fi

if ! command -v kaggle >/dev/null 2>&1; then
  echo "Missing Kaggle CLI."
  echo
  echo "Install it with one of:"
  echo "  python -m pip install kaggle"
  echo "  pipx install kaggle"
  echo
  echo "Then authenticate with either:"
  echo "  ~/.kaggle/kaggle.json"
  echo "or environment variables:"
  echo "  KAGGLE_USERNAME=... KAGGLE_KEY=..."
  echo "You can also put those variables in a repo-root .env file."
  exit 1
fi

mkdir -p "$RAW_DIR"

echo "Downloading Kaggle dataset: $DATASET_SLUG"
kaggle datasets download \
  --dataset "$DATASET_SLUG" \
  --path "$RAW_DIR" \
  --force

if [[ ! -f "$ZIP_PATH" ]]; then
  found_zip="$(find "$RAW_DIR" -maxdepth 1 -type f -name "*.zip" | head -n 1)"
  if [[ -z "$found_zip" ]]; then
    echo "Download completed, but no zip file was found in $RAW_DIR"
    exit 1
  fi
  ZIP_PATH="$found_zip"
fi

echo "Unzipping $(basename "$ZIP_PATH") into data/raw/"
unzip -o "$ZIP_PATH" -d "$RAW_DIR"

if [[ ! -f "$EXPECTED_CSV" ]]; then
  csv_count=0
  csv_candidate=""
  while IFS= read -r csv_file; do
    csv_count=$((csv_count + 1))
    csv_candidate="$csv_file"
  done < <(find "$RAW_DIR" -maxdepth 1 -type f -name "*.csv")

  if [[ "$csv_count" -eq 1 ]]; then
    mv "$csv_candidate" "$EXPECTED_CSV"
  fi
fi

if [[ ! -f "$EXPECTED_CSV" ]]; then
  echo "Expected CSV not found at $EXPECTED_CSV"
  echo "CSV files currently in data/raw/:"
  find "$RAW_DIR" -maxdepth 1 -type f -name "*.csv" -print
  exit 1
fi

echo
echo "Dataset ready:"
echo "  $EXPECTED_CSV"

if git -C "$REPO_ROOT" check-ignore -q "data/raw/retail_store_inventory.csv"; then
  echo "Confirmed: data/raw/retail_store_inventory.csv is ignored by Git."
else
  echo "Warning: data/raw/retail_store_inventory.csv is not ignored by Git."
  echo "Do not commit the dataset."
fi
