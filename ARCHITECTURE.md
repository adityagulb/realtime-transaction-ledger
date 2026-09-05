# Architecture

## 1. End-to-End Architecture

```mermaid
flowchart LR

    C[Client] -->|POST /api/v1/transactions| API[banking-transaction-processor]

    API --> V[Validation Chain]

    V -->|valid| DB[(PostgreSQL)]
    V -->|rejected| AUDIT[(rejected_transaction)]

    DB --> TX[ledger_transaction]
    DB --> OB[outbox_event]

    POLL[DB Poller] -->|FOR UPDATE SKIP LOCKED| OB
    POLL --> K[Kafka<br/>transaction-events]

    K --> W[transaction-analytics-worker]

    W -->|retry exhausted| DLT[transaction-events.DLT]

    W --> BATCH[NDJSON File Batching]
    BATCH --> S3[(Amazon S3)]

    S3 --> STAGE[Snowflake External Stage]

    STAGE -->|COPY INTO| LANDING[(Landing DB)]

    LANDING -->|Stream + Task| RAW[(Raw DB)]

    RAW -->|Streams + Tasks / MERGE| PREPARED[(Prepared DB)]

    PREPARED -->|Streams + Tasks / MERGE| CURATED[(Curated DB)]

    CURATED --> GC[GC View<br/>General Consumption]
    CURATED --> HC[HC View<br/>High Clearance]
