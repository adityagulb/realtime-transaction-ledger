# Java change: Kafka -> S3 instead of Kafka -> Snowflake

The existing Kafka consumer should stop inserting directly into Snowflake.

Its responsibility becomes:

```text
Kafka record
   ->
validate/deserialize
   ->
append event JSON to current batch
   ->
when batch-size threshold is reached
   ->
write NDJSON file
   ->
upload file to S3
   ->
commit Kafka offset
```

A safe production order is:

```text
1. Consume Kafka records.
2. Build the NDJSON file.
3. Upload file successfully to S3.
4. Commit the Kafka offsets.
```

If the process fails before step 4, Kafka can replay the events.

Use a deterministic or unique S3 object key. Example:

```text
banking-transactions/year=2026/month=09/day=02/hour=15/
partition-2-offset-10500-10999.json
```

The partition/offset range makes operational debugging and replay analysis easier.

The supplied Snowflake pipeline assumes each line of the object is one complete JSON event.
