# Deutsche Bank Review Checklist

The submitted implementation should make these design points visible:

- Java is responsible for transactional/API integration and Kafka -> S3 delivery.
- S3 files are immutable NDJSON batches.
- Snowflake external stage uses a storage integration; no static AWS credentials are committed.
- LANDING preserves the source payload as `VARIANT`.
- `COPY INTO` provides file-level ingestion tracking.
- A LANDING stream exposes only newly loaded rows.
- A task performs typed `MERGE` into RAW.
- RAW keeps typed source events and audit metadata.
- Independent streams support DIM, FACT and BRIDGE consumers.
- PREPARED contains dimensional structures.
- CURATED exposes a business-friendly view and an incremental aggregate.
- Security roles and a masking-policy pattern are included.
- Validation covers task history, copy history, duplicates and reconciliation.
- X-Small warehouse + auto suspend controls trial-account cost.
