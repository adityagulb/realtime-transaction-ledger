# VP Case Study 1 - Two Repository Solution

1. `banking-transaction-processor`: API, validation, PostgreSQL ledger/audit, transactional outbox, DB poller, Kafka producer.
2. `transaction-analytics-worker`: Kafka consumer, retry/DLT, idempotent Snowflake adapter.

## End-to-end
`Client -> Processor -> PostgreSQL (ledger + outbox atomically) -> DB Poller -> Kafka -> Analytics Worker -> Snowflake`

The design intentionally uses a scheduled DB poller rather than Debezium. Delivery from outbox to Kafka is at-least-once; the downstream sink is idempotent by event ID.
