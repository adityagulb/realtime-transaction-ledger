#!/usr/bin/env bash
set -euo pipefail

# Usage:
#   export S3_URI=s3://your-bucket/banking-transactions/year=2026/month=09/day=02/hour=15/
#   ./scripts/upload-samples.sh
#
# Requires AWS CLI credentials to already be configured locally.

: "${S3_URI:?Set S3_URI first}"

aws s3 cp sample-data/transaction_batch_001.json "${S3_URI}transaction_batch_001.json"
aws s3 cp sample-data/transaction_batch_002_update.json "${S3_URI}transaction_batch_002_update.json"

echo "Uploaded sample NDJSON files to ${S3_URI}"
