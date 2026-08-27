# Architecture

```mermaid
flowchart LR
  C[Client] -->|POST /api/v1/transactions| API[banking-transaction-processor]
  API --> V[Validation chain]
  V -->|valid| DB[(PostgreSQL)]
  V -->|rejected| AUDIT[(rejected_transaction)]
  DB --> TX[ledger_transaction]
  DB --> OB[outbox_event]
  POLL[DB Poller] -->|FOR UPDATE SKIP LOCKED| OB
  POLL --> K[Kafka transaction-events]
  K --> W[transaction-analytics-worker]
  W -->|retry exhausted| DLT[transaction-events.DLT]
  W --> S[(Snowflake)]
```

## Consistency and delivery
- Ledger + outbox are one ACID transaction.
- Polling is horizontally safe through PostgreSQL row locking with `SKIP LOCKED`.
- Outbox -> Kafka is at-least-once.
- Snowflake `MERGE` by `EVENT_ID` makes downstream handling idempotent.
- Rejected requests are retained in a separate audit table with stable error codes.
