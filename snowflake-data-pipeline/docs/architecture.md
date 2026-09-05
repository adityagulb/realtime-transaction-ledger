# Architecture and Design Rationale

## Responsibility split

### Java / Spring

The application tier owns:

- REST transaction processing.
- PostgreSQL transactional persistence.
- Outbox/idempotency logic.
- Kafka publishing/consumption.
- File batching.
- S3 upload.
- Retry around external integration.

### Snowflake

Once the file exists in S3, Snowflake owns:

- ingestion,
- change tracking,
- incremental processing,
- type conversion,
- dimensional modelling,
- business-facing views/aggregates,
- role-based access,
- operational reconciliation.

This prevents unnecessary extraction of Snowflake data back into Java merely to transform it.

## Layer semantics

### LANDING

Purpose: preserve the source event with minimal transformation.

Primary storage:

```text
PAYLOAD VARIANT
SOURCE_FILE
SOURCE_ROW_NUMBER
LOAD_TS
```

### RAW

Purpose: produce typed, queryable, auditable source-domain records.

`MERGE` is used to make ingestion idempotent by `EVENT_ID`.

### PREPARED

Purpose: build analytic structures.

Included in this example:

- `DIM_ACCOUNT`
- `FACT_TRANSACTION`
- `BRIDGE_TRANSACTION_ACCOUNT`

### CURATED

Purpose: expose business-specific data products.

Included:

- `V_TRANSACTION_HISTORY`
- `DAILY_TRANSACTION_SUMMARY`

## Why multiple RAW streams?

Snowflake streams have consumer-offset semantics. Once a stream is consumed by a committed DML statement, its offset advances.

The dimension, fact and bridge loads are logically separate consumers, therefore this demo gives each one an independent stream:

```text
RAW_DIM_ACCOUNT_STREAM
RAW_FACT_TRANSACTION_STREAM
RAW_BRIDGE_STREAM
```

This avoids one task consuming changes that another task still needs.

## PII

The sample event intentionally avoids real customer PII.

A production implementation should classify PII columns and apply:

- masking policies,
- row access policies where appropriate,
- least-privilege roles,
- secure views for controlled data sharing.

A sample masking policy is included in the security scripts for demonstration.
