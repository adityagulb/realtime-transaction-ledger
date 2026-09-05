# Real-Time Transaction Ledger & Analytics Pipeline

Enterprise-style transaction processing and analytics case study built using **Java 17, Spring Boot, PostgreSQL, Kafka, Amazon S3 and Snowflake**.

The solution demonstrates reliable transaction processing, asynchronous event delivery, durable analytical ingestion, layered Snowflake data engineering, security, governance, reconciliation and failure recovery.

---

## Architecture

```text
Client
  |
  | REST
  v
Transaction Processor
  |
  v
PostgreSQL
  |
  +-- ledger_transaction
  +-- outbox_event
  +-- rejected_transaction
  |
  v
DB Poller
  |
  | FOR UPDATE SKIP LOCKED
  v
Kafka
  |
  | transaction-events
  v
Transaction Analytics Worker
  |
  +-- Retry / DLT
  +-- Event batching
  +-- NDJSON serialization
  |
  v
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
  +------> GC Views
  |
  +------> HC Views
```

For the detailed architecture, reliability model, Snowflake layers, security, governance and recovery strategy, see [`ARCHITECTURE.md`](ARCHITECTURE.md).

---

# Repository Structure

```text
realtime-transaction-ledger/
│
├── banking-transaction-processor/
│   └── REST -> PostgreSQL -> Transactional Outbox -> Kafka
│
├── transaction-analytics-worker/
│   └── Kafka -> Retry/DLT -> Batch -> NDJSON -> Amazon S3
│
├── snowflake-data-pipeline/
│   └── S3 -> Landing -> Raw -> Prepared -> Curated
│
├── ARCHITECTURE.md
└── README.md
```

The solution separates responsibilities between transactional processing, event integration and analytical data engineering.

---

# 1. Banking Transaction Processor

`banking-transaction-processor` is the synchronous transaction-processing service.

### Responsibilities

* exposes transaction REST APIs
* validates incoming transactions
* applies business validation using a Chain of Responsibility
* persists ledger transactions
* provides duplicate/idempotency protection
* writes ledger and outbox records atomically
* audits rejected transactions
* polls unpublished outbox records
* publishes transaction events to Kafka
* exposes health and operational metrics

### Transactional Boundary

```text
POST /api/v1/transactions
          |
          v
    Validation Chain
          |
    +-----+------+
    |            |
 Rejected       Valid
    |            |
    v            v
Audit       @Transactional
                 |
          +------+------+
          |             |
          v             v
       Ledger         Outbox
```

The ledger transaction and corresponding outbox event are committed in the **same PostgreSQL transaction**.

This avoids the database/Kafka dual-write problem.

---

# 2. Transactional Outbox

A scheduled DB poller reads unpublished outbox records using:

```sql
FOR UPDATE SKIP LOCKED
```

This allows multiple poller instances to process different records concurrently without competing for the same row.

```text
PostgreSQL
     |
     v
outbox_event
     |
     v
DB Poller
     |
     v
Kafka
```

Outbox-to-Kafka delivery is **at-least-once**.

Therefore duplicate event delivery is considered a normal failure scenario and downstream components are designed to tolerate replay.

---

# 3. Transaction Analytics Worker

`transaction-analytics-worker` consumes transaction events from Kafka.

### Responsibilities

* Kafka consumption
* event deserialization
* retry handling
* Dead Letter Topic handling
* event batching
* NDJSON serialization
* Amazon S3 upload

```text
Kafka
  |
  v
Analytics Worker
  |
  +-- Deserialize
  +-- Retry
  +-- DLT
  +-- Batch
  |
  v
NDJSON
  |
  v
Amazon S3
```

The worker intentionally does **not** directly write analytical records into Snowflake.

Amazon S3 acts as the durable integration boundary between real-time event processing and the analytical data platform.

---

# 4. Amazon S3 Integration Boundary

Events are grouped into NDJSON files before upload.

Example:

```text
banking-transactions/
    year=2026/
        month=09/
            day=05/
                hour=13/
                    transactions-<timestamp>-<uuid>.json
```

Batching is controlled by:

* maximum record count
* flush interval

Using S3 provides:

* durable replay
* decoupling from Snowflake availability
* bulk-oriented analytical ingestion
* source-file auditability
* easier failure recovery
* independent scaling of application and analytical workloads

Kafka processing should only be considered successfully persisted after the corresponding batch has been durably written to S3.

---

# 5. Snowflake Data Pipeline

`snowflake-data-pipeline` owns analytical ingestion and transformation after the S3 boundary.

```text
Amazon S3
     |
     v
External Stage
     |
     v
COPY INTO
     |
     v
Landing
     |
     v
Raw
     |
     v
Prepared
     |
     v
Curated
```

Once data enters Snowflake, transformations are primarily executed using **Snowflake-native SQL, Streams, Tasks and MERGE operations** rather than moving data back into Java.

---

# 6. Snowflake Data Layers

## Landing

The Landing layer is the ingestion boundary.

Typical characteristics:

* source-oriented data
* minimal transformation
* semi-structured `VARIANT` payload
* source filename
* source row number
* ingestion timestamp

Its purpose is to preserve the source representation for traceability, troubleshooting and replay.

---

## Raw

The Raw layer converts source events into typed analytical records.

Responsibilities include:

* JSON extraction
* data-type conversion
* technical validation
* event deduplication
* historical event retention
* ingestion metadata

`MERGE` and event identifiers make processing replay-safe and idempotent.

---

## Prepared

The Prepared layer contains reusable enterprise data models.

Typical structures include:

```text
DIM_ACCOUNT
FACT_TRANSACTION
BRIDGE_TRANSACTION_ACCOUNT
```

This layer separates business modeling from source-system structures.

---

## Curated

The Curated layer exposes business-oriented data products.

Potential consumers include:

* reporting
* Business Intelligence
* analytics
* data science
* downstream applications
* controlled data sharing

Curated models are designed around consumption requirements rather than ingestion structures.

---

# 7. Snowflake Streams and Tasks

Incremental processing is implemented using Snowflake-native capabilities.

```text
Landing
   |
Landing Stream
   |
Landing -> Raw Task
   |
   v
Raw
   |
Raw Streams
   |
Raw -> Prepared Tasks
   |
   v
Prepared
   |
Prepared Stream
   |
Prepared -> Curated Task
   |
   v
Curated
```

A **Stream** identifies what data changed.

A **Task** determines when processing runs and executes the required SQL or `MERGE`.

This keeps set-based data transformation close to the data.

---

# 8. Security and Governance

The Snowflake design follows least-privilege access using RBAC.

Access can be controlled at multiple levels:

```text
User
  |
  v
Role
  |
  +-- Database
  +-- Schema
  +-- Table / View
  +-- Row Access Policy
  +-- Masking Policy
```

Example roles include:

```text
BANKING_PIPELINE_ROLE
BANKING_ANALYST_ROLE
BANKING_HIGH_CLEARANCE_ROLE
```

Row access policies can restrict which records a consumer can access.

Masking policies protect sensitive column values based on authorization.

---

# 9. GC and HC Consumption

Sensitive information remains protected in the underlying/core model.

```text
Prepared / Core Data
         |
         v
      Curated
         |
    +----+----+
    |         |
    v         v
   GC        HC
  View      View
```

### GC — General Consumption

Used for standard analytical consumption.

Sensitive attributes remain masked, protected or encrypted according to policy.

### HC — High Clearance

Restricted to explicitly authorized roles.

Authorized users can access sensitive values where business and governance policies permit it.

The design can combine:

* RBAC
* secure views
* masking policies
* row access policies
* controlled encryption/decryption

to protect PII throughout the consumption model.

---

# 10. Metadata and Data Governance

Technical and business metadata can be maintained in an enterprise catalog such as Collibra or another metadata repository.

Examples include:

* business definitions
* dataset ownership
* source systems
* schemas and columns
* PII classification
* security classification
* refresh frequency
* SLA
* retention
* quality rules
* lineage

This separates governance metadata from physical pipeline execution.

---

# 11. Reconciliation and Data Quality

The pipeline is designed so that records can be reconciled across every stage.

Example:

```text
Kafka events produced          10,000
        |
        v
S3 records                     10,000
        |
        v
Landing                        10,000
        |
        v
Raw accepted                    9,998
Raw rejected                        2
        |
        v
Prepared                        9,998
        |
        v
Curated                         9,998
```

Quality checks include:

* mandatory fields
* duplicate event IDs
* invalid amounts
* invalid currencies
* invalid data types
* referential integrity
* source-to-target reconciliation

Rejected data remains traceable for investigation and controlled reprocessing.

---

# 12. End-to-End Lineage

A curated transaction can be traced back through the complete processing chain.

```text
Curated
   |
Prepared
   |
Raw
   |
Landing
   |
S3 File
   |
Kafka Event
   |
Outbox Event
   |
Ledger Transaction
```

Important lineage identifiers include:

* `EVENT_ID`
* `TRANSACTION_ID`
* source filename
* source row number
* ingestion timestamp
* processing timestamp

---

# 13. Failure and Recovery Model

The design isolates failures at each architectural boundary.

### PostgreSQL failure

Ledger and outbox remain atomic; no partial transaction is committed.

### Kafka / DB Poller failure

Unpublished outbox records remain available for retry.

### Analytics Worker failure

Kafka retains unprocessed events.

### Poison event

Retry is attempted before the event is routed to the DLT.

### S3 failure

The batch is not considered durably persisted until upload succeeds.

### Snowflake failure

Source files remain safely available in S3 and can be processed after recovery.

### Transformation failure

Snowflake Task history, Streams and idempotent `MERGE` processing support controlled recovery.

---

# 14. Technology Stack

### Application

* Java 17
* Spring Boot
* Spring Data JPA
* Spring Kafka
* Spring JDBC
* Maven

### Transactional Data

* PostgreSQL
* Flyway

### Messaging

* Apache Kafka

### Cloud / Integration

* Amazon S3
* AWS SDK
* Terraform

### Analytics

* Snowflake
* External Stages
* `COPY INTO`
* Streams
* Tasks
* `MERGE`
* `VARIANT`

### Security / Governance

* Snowflake RBAC
* Row Access Policies
* Masking Policies
* GC / HC consumption model
* enterprise metadata/catalog integration

### Testing

* JUnit 5
* Mockito
* Spring Boot Test
* Testcontainers

---

# 15. Key Design Principles

The architecture demonstrates several enterprise design principles:

1. **Transactional consistency**
   Ledger and outbox are committed atomically.

2. **Reliable asynchronous delivery**
   Kafka decouples transactional processing from downstream analytics.

3. **At-least-once processing**
   Duplicate delivery is expected and handled through idempotent design.

4. **Durable analytics boundary**
   S3 provides replayability and isolates Snowflake availability from Kafka processing.

5. **Separation of responsibilities**
   Java handles transactional and integration concerns; Snowflake handles analytical transformation and modeling.

6. **Incremental data engineering**
   Streams and Tasks process changes rather than repeatedly rebuilding entire datasets.

7. **Layered analytical modeling**
   Landing, Raw, Prepared and Curated have clearly separated responsibilities.

8. **Enterprise security**
   RBAC, row-level controls, masking and differentiated GC/HC consumption protect sensitive data.

9. **Auditability and lineage**
   Transactions can be traced from business consumption back to their transactional source.

10. **Recoverability**
    Durable boundaries, retries, DLT handling and idempotent processing support controlled replay.

11. **Governance**
    Ownership, metadata, classification, lineage and quality rules are treated as part of the architecture rather than afterthoughts.

---

# Design Summary

```text
Transaction Processing
        |
        v
PostgreSQL + Transactional Outbox
        |
        v
Kafka
        |
        v
Event Integration
        |
        v
Amazon S3
        |
        v
Snowflake Data Platform
        |
        +-- Landing
        +-- Raw
        +-- Prepared
        +-- Curated
                |
                +-- GC
                +-- HC
```

The solution combines **Java transactional engineering, event-driven architecture and enterprise Snowflake data engineering** while maintaining clear boundaries for consistency, scalability, security, governance, auditability and recovery.
