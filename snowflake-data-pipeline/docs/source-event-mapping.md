# Source Event Mapping

The supplied pipeline uses this canonical JSON contract:

| Kafka/S3 JSON field | RAW column |
|---|---|
| `eventId` | `EVENT_ID` |
| `transactionId` | `TRANSACTION_ID` |
| `eventType` | `EVENT_TYPE` |
| `accountId` | `ACCOUNT_ID` |
| `counterpartyAccountId` | `COUNTERPARTY_ACCOUNT_ID` |
| `amount` | `AMOUNT` |
| `currency` | `CURRENCY` |
| `transactionTimestamp` | `TRANSACTION_TS` |
| `eventVersion` | `EVENT_VERSION` |
| `source` | `SOURCE_SYSTEM` |

If the existing Java/Kafka DTO uses different property names, update only the JSON paths in:

- `sql/03_raw/02_landing_to_raw_task.sql`
- `sql/07_tasks/01_task_graph.sql`

For example, if the Java DTO emits `type` rather than `eventType`, replace:

```sql
PAYLOAD:eventType::VARCHAR
```

with:

```sql
PAYLOAD:type::VARCHAR
```
