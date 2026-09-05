# Transaction Analytics Worker

Kafka consumer for the Real-Time Transaction Ledger & Analytics Pipeline.

The worker consumes transaction events from Kafka, applies retry/error handling, batches successfully processed events into NDJSON files, and persists those files to Amazon S3.

The worker intentionally does **not** write directly to Snowflake. S3 acts as the durable integration boundary between the real-time event-processing application and the downstream Snowflake data platform.

## Architecture

```text
Kafka
  |
  | transaction-events
  v
Transaction Analytics Worker
  |
  +-- Deserialize
  +-- Validate
  +-- Retry transient failures
  +-- DLT exhausted / poison events
  +-- Batch events
  |
  v
NDJSON Batch
  |
  v
Amazon S3
  |
  v
Snowflake External Stage
  |
  v
Landing -> Raw -> Prepared -> Curated
```

The Snowflake pipeline is maintained separately from this worker.

---

## Responsibilities

The Transaction Analytics Worker is responsible for:

- consuming transaction events from Kafka
- deserializing events into `TransactionEvent`
- handling transient processing failures
- publishing exhausted failures to `<original-topic>.DLT`
- batching transaction events
- serializing batches as NDJSON
- uploading batches to Amazon S3
- retaining failed batches for retry
- providing a clean integration boundary between Kafka and the analytics platform

The worker is **not responsible for Snowflake transformations or data modeling**.

Snowflake ingestion and transformation are handled downstream.

---

## Analytics Sink

The application uses the `AnalyticsSink` abstraction to separate Kafka consumption from the physical analytics destination.

### Logging Sink

```text
ANALYTICS_SINK=log
```

Used for local development and debugging.

Flow:

```text
Kafka -> Analytics Worker -> Log
```

### S3 Sink

```text
ANALYTICS_SINK=s3
```

Used for the analytics pipeline.

Flow:

```text
Kafka
  -> Analytics Worker
  -> Event Batch
  -> NDJSON
  -> Amazon S3
```

This allows the destination implementation to change without coupling the Kafka consumer directly to a warehouse technology.

---

## File Batching

Transaction events are grouped before being uploaded to S3.

Two conditions can trigger a flush:

1. Maximum number of records is reached.
2. Flush interval expires.

Example configuration:

```properties
BATCH_MAX_RECORDS=500
BATCH_FLUSH_INTERVAL_MS=30000
```

This prevents creation of one S3 object per Kafka event while ensuring low-volume events are not retained indefinitely in memory.

---

## NDJSON Format

Each Kafka transaction event becomes one JSON record in the output file.

Example:

```json
{"eventId":"11111111-1111-1111-1111-111111111111","ledgerId":"22222222-2222-2222-2222-222222222222","transactionId":"txn-1001","amount":100.00,"currency":"INR","settlementDate":"2026-09-05","status":"COMPLETED","occurredAt":"2026-09-05T07:30:00Z"}
{"eventId":"33333333-3333-3333-3333-333333333333","ledgerId":"44444444-4444-4444-4444-444444444444","transactionId":"txn-1002","amount":250.00,"currency":"INR","settlementDate":"2026-09-05","status":"COMPLETED","occurredAt":"2026-09-05T07:31:00Z"}
```

NDJSON is used because each event remains an independent JSON record and can be loaded efficiently into the Snowflake Landing layer.

---

## S3 Object Layout

Files are organized using date/time prefixes.

Example:

```text
banking-transactions/
    year=2026/
        month=09/
            day=05/
                hour=13/
                    transactions-<timestamp>-<uuid>.json
```

Example:

```text
s3://transaction-analytics/
banking-transactions/
year=2026/
month=09/
day=05/
hour=13/
transactions-20260905T133000Z-a1b2c3.json
```

Using unique object names makes retries and parallel workers safer and provides an auditable source-file boundary.

---

## Configuration

Example environment configuration:

```properties
# Kafka

KAFKA_BOOTSTRAP_SERVERS=localhost:9092

APP_KAFKA_TOPIC=transaction-events

APP_KAFKA_GROUP=transaction-analytics


# Analytics destination

ANALYTICS_SINK=s3


# AWS

AWS_REGION=ap-south-1

S3_BUCKET=your-transaction-bucket

S3_PREFIX=banking-transactions


# Batching

BATCH_MAX_RECORDS=500

BATCH_FLUSH_INTERVAL_MS=30000
```

Do not commit AWS credentials to the repository.

The AWS SDK credential provider chain should be used.

For local development this can use an AWS CLI profile or environment credentials.

For deployment on AWS, IAM roles should be preferred.

---

## Kafka Retry / DLT

Spring Kafka `DefaultErrorHandler` provides retry handling for processing failures.

Transient failures are retried using backoff.

After the configured retry attempts are exhausted, poison/unprocessable records can be published to:

```text
<original-topic>.DLT
```

Example:

```text
transaction-events
       |
       v
Analytics Worker
       |
       +---- success ----> Batch ----> S3
       |
       +---- failure ----> Retry
                              |
                              +---- recovered --> processing
                              |
                              +---- exhausted --> transaction-events.DLT
```

DLT records can later be inspected, corrected where appropriate, and replayed.

---

## Delivery Semantics

The upstream Transaction Processor uses the Transactional Outbox pattern.

Therefore Kafka delivery is treated as **at-least-once**.

The analytics pipeline must tolerate duplicate delivery.

The durable processing boundary for this worker is S3.

Conceptually:

```text
Kafka batch
     |
     v
Deserialize
     |
     v
Create NDJSON
     |
     v
Upload to S3
     |
     | SUCCESS
     v
Kafka acknowledgement
```

Kafka offsets should only be considered successfully processed after the corresponding data has been durably persisted to S3.

Downstream Snowflake processing also uses idempotent loading/transformation techniques to make replay safe.

---

## Failure Handling

### Kafka unavailable

The worker cannot consume new events.

Existing events remain retained by Kafka.

### Worker unavailable

Kafka retains unconsumed/uncommitted events.

Processing resumes when the worker becomes available.

### S3 unavailable

The batch must not be considered successfully persisted.

The upload is retried and Kafka acknowledgement should not occur until durable persistence succeeds.

### Poison event

Processing retries are exhausted and the event is routed to the DLT.

### Snowflake unavailable

The worker is unaffected after successfully writing the batch to S3.

Files remain durably available in S3 and Snowflake ingestion can resume independently.

This is one of the reasons S3 is used as the integration boundary instead of directly coupling the Kafka worker to Snowflake availability.

---

## Downstream Snowflake Processing

This application ends at S3.

The downstream analytical pipeline is:

```text
Amazon S3
    |
    v
Snowflake External Stage
    |
    | COPY INTO
    v
Landing
    |
    | Stream + Task + MERGE
    v
Raw
    |
    | Streams + Tasks + MERGE
    v
Prepared
    |
    | Streams + Tasks
    v
Curated
    |
    +---- GC View
    |
    +---- HC View
```

### Landing

Retains the source payload with minimal transformation, typically using `VARIANT`, together with ingestion metadata such as source file and load timestamp.

### Raw

Converts source payloads into typed columns and applies technical validation and deduplication.

### Prepared

Creates reusable enterprise models such as facts, dimensions and bridge tables.

### Curated

Provides consumption-oriented data products for reporting, analytics and downstream consumers.

GC and HC views provide differentiated access to sensitive data.

See the repository-level `ARCHITECTURE.md` and the Snowflake pipeline module for the complete data-platform design.

---

## Build

Compile the worker:

```bash
mvn clean compile
```

Run tests:

```bash
mvn clean test
```

Run locally:

```bash
mvn spring-boot:run
```

---

## Local Testing

For initial S3 testing, use a small batch size:

```properties
ANALYTICS_SINK=s3

BATCH_MAX_RECORDS=3

BATCH_FLUSH_INTERVAL_MS=30000

S3_BUCKET=<your-test-bucket>

AWS_REGION=ap-south-1
```

Produce three transaction events to:

```text
transaction-events
```

The worker should create one NDJSON batch and upload it to the configured S3 prefix.

For low-volume testing, the scheduled flush will upload a partially filled batch after the configured flush interval.

---

## Tests

Unit tests should cover:

- Kafka event deserialization
- forwarding valid events to the configured sink
- batch-size-triggered S3 upload
- scheduled flush of partial batches
- S3 bucket and object-key construction
- NDJSON serialization
- S3 upload failure behavior
- retention/retry behavior when persistence fails
- Kafka retry and DLT behavior where applicable

Run:

```bash
mvn test
```

Snowflake JDBC and direct Snowflake `MERGE` tests are intentionally not part of this worker because Snowflake processing belongs to the downstream data-pipeline component.

---

## Terraform

The existing `terraform/` module provisions the Kafka/MSK infrastructure required by the case study.

When using AWS-managed infrastructure, applications should use IAM-based authentication and least-privilege permissions.

The analytics worker additionally requires permission to write objects only to its configured S3 bucket/prefix.

---

## Design Principle

The key responsibility boundary is:

```text
Transaction Processing
        |
        v
PostgreSQL / Outbox
        |
        v
Kafka
        |
        v
Event Integration
Analytics Worker
        |
        v
Amazon S3
        |
        v
Data Platform
Snowflake
```

This keeps the Java event-processing layer decoupled from the analytical warehouse and provides a durable, replayable boundary between real-time transaction processing and downstream data engineering.
