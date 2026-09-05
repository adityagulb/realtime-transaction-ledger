# Snowflake Data Pipeline — Banking Transaction Processor

This folder is designed to be added as the **third top-level component** in the existing repository:

```text
banking-transaction-processor/
├── transaction-api/              # Existing: REST -> PostgreSQL -> Kafka
├── transaction-ingestion/        # Existing/modified: Kafka -> S3
└── snowflake-data-pipeline/      # This folder
```

## Target architecture

```text
REST API
   |
   v
Spring Boot
   |
   v
PostgreSQL
   |
   v
Kafka
   |
   v
Java Kafka Consumer
   |
   | writes NDJSON files
   v
Amazon S3
   |
   | Snowflake external stage + COPY INTO
   v
BANKING_LANDING
   |
   | Stream + Task
   v
BANKING_RAW
   |
   | Independent Streams + Tasks
   v
BANKING_PREPARED
   |         |          |
   |         |          +--> BRIDGE_TRANSACTION_ACCOUNT
   |         +-------------> DIM_ACCOUNT
   +-----------------------> FACT_TRANSACTION
   |
   | Stream + Task / Views
   v
BANKING_CURATED
   |
   +--> V_TRANSACTION_HISTORY
   +--> DAILY_TRANSACTION_SUMMARY
```

## Why the layers are separate Snowflake databases

The implementation follows the hierarchy:

```text
Database -> Schema -> Table/View -> Column
```

The logical data-engineering layers are therefore separate databases:

- `BANKING_LANDING` — temporary/semi-structured ingestion, payload retained as `VARIANT`.
- `BANKING_RAW` — typed, auditable transaction-event data.
- `BANKING_PREPARED` — fact, dimension and bridge modelling.
- `BANKING_CURATED` — business-consumption views/tables.

## Data contract from Kafka -> S3

The Java consumer should write **newline-delimited JSON (NDJSON)**. One Kafka transaction event becomes one JSON line.

Example:

```json
{"eventId":"evt-1001","transactionId":"txn-1001","eventType":"DEPOSIT","accountId":"ACC-001","counterpartyAccountId":null,"amount":1250.50,"currency":"INR","transactionTimestamp":"2026-09-02T09:30:00Z","eventVersion":1,"source":"BANKING_API"}
```

Required fields for the supplied SQL:

- `eventId`
- `transactionId`
- `eventType`
- `accountId`
- `amount`
- `currency`
- `transactionTimestamp`

Optional fields:

- `counterpartyAccountId`
- `eventVersion`
- `source`

If your existing Kafka payload uses different field names, only the JSON-path mapping in
`sql/03_raw/02_landing_to_raw_task.sql` needs to be adjusted.

## S3 naming convention

Recommended prefix:

```text
s3://<bucket>/banking-transactions/year=YYYY/month=MM/day=DD/hour=HH/
```

Example:

```text
s3://my-bank-demo/banking-transactions/year=2026/month=09/day=02/hour=15/transactions-000001.json
```

Your Java application may buffer a fixed number of Kafka messages before creating each NDJSON file.

## Execution order

Run the scripts in this order:

1. `sql/00_bootstrap/01_warehouse_databases_schemas.sql`
2. `sql/01_storage/01_storage_integration.sql`
3. Complete the AWS IAM trust step described below.
4. `sql/01_storage/02_file_format_stage.sql`
5. `sql/02_landing/01_landing_objects.sql`
6. `sql/03_raw/01_raw_objects.sql`
7. `sql/04_prepared/01_prepared_objects.sql`
8. `sql/05_curated/01_curated_objects.sql`
9. `sql/06_security/01_roles_and_grants.sql`
10. `sql/07_tasks/01_task_graph.sql`
11. `sql/07_tasks/02_resume_task_graph.sql`
12. Upload a sample NDJSON file to the configured S3 prefix.
13. Use `sql/08_validation/01_validation_queries.sql` to verify the flow.

## AWS storage integration — important one-time step

Edit this placeholder in `sql/01_storage/01_storage_integration.sql`:

```text
<YOUR_AWS_ROLE_ARN>
<YOUR_S3_BUCKET>
```

After creating the Snowflake storage integration, run:

```sql
DESC INTEGRATION BANKING_S3_INT;
```

Snowflake returns values including:

- `STORAGE_AWS_IAM_USER_ARN`
- `STORAGE_AWS_EXTERNAL_ID`

Update the trust policy of `<YOUR_AWS_ROLE_ARN>` in AWS so that the Snowflake IAM user can assume the role using the external ID.

Then create the stage using `sql/01_storage/02_file_format_stage.sql`.

No AWS keys or secrets should be committed to this repository.

## Task graph

The root task polls S3 using `COPY INTO` every minute.

```text
LOAD_LANDING_TASK
       |
       v
LANDING_TO_RAW_TASK
       |
       v
RAW_TO_DIM_ACCOUNT_TASK
       |
       v
RAW_TO_FACT_TRANSACTION_TASK
       |
       v
RAW_TO_BRIDGE_TASK
       |
       v
PREPARED_TO_CURATED_TASK
```

`SYSTEM$STREAM_HAS_DATA(...)` prevents downstream tasks from running when there is no new data.

Three independent streams are created on the RAW transaction-event table because a Snowflake stream represents a consumer offset. Independent consumers should not share a single stream.

## Idempotency

There are two layers of protection:

1. `COPY INTO` tracks loaded files and does not reload an already loaded file unless explicitly forced.
2. `LANDING_TO_RAW_TASK` uses `MERGE` by `EVENT_ID` and keeps the highest/latest event version.

This makes replaying Kafka messages or S3 files safer.

## Testing an update

Upload `sample-data/transaction_batch_001.json`, let the task graph process it, and inspect the results.

Then upload `sample-data/transaction_batch_002_update.json`.

The second file contains a later event for `txn-1002`. The prepared fact table is updated to represent the latest state for that transaction while RAW retains individual source events.

## Free-trial cost controls

The warehouse is deliberately created as `XSMALL` with:

```text
AUTO_SUSPEND = 60
AUTO_RESUME  = TRUE
```

For manual testing you can suspend the task graph when finished:

```sql
ALTER TASK BANKING_LANDING.PIPELINE.LOAD_LANDING_TASK SUSPEND;
```

## Production improvements that are intentionally documented rather than overbuilt

For a larger production implementation, consider:

- Snowpipe / event notifications instead of scheduled `COPY INTO`.
- Dynamic Tables for selected transformations.
- Dead-letter/reject S3 prefixes.
- Schema registry and event-version compatibility rules.
- Snowflake resource monitors.
- Stronger data-quality framework.
- Tag-based masking policies for PII.
- Terraform for Snowflake + AWS resources.
- Observability into task history, copy history and reconciliation metrics.
