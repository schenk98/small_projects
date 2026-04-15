#!/usr/bin/env bash
set -euo pipefail

SECRET_ID="${1:-book-recommender/kaggle}"
OUTPUT_PATH="${2:-./secrets/kaggle.json}"

mkdir -p "$(dirname "${OUTPUT_PATH}")"
aws secretsmanager get-secret-value \
  --secret-id "${SECRET_ID}" \
  --query SecretString \
  --output text > "${OUTPUT_PATH}"
chmod 600 "${OUTPUT_PATH}"

echo "Secret written to ${OUTPUT_PATH}"
