# Banking Transaction Processor

Spring Boot / Java 17 ingestion service for VP Case Study 1.

## Responsibilities
- POST `/api/v1/transactions`
- Bean validation + Chain-of-Responsibility business validation
- Atomic deduplication claim for a configurable sliding window
- ACID write of ledger transaction + transactional outbox event
- Rejected transaction audit table with error codes
- Scheduled PostgreSQL outbox poller using `FOR UPDATE SKIP LOCKED`
- Kafka publication with at-least-once delivery semantics
- Actuator health and metrics endpoints

## Reliability model
The ledger row and outbox row are committed in the same transaction. The poller reads a locked batch, publishes to Kafka, and marks the row published only after broker acknowledgement. A crash after Kafka acknowledgement but before DB commit can cause a duplicate event; consumers must therefore be idempotent by `eventId`.

## Run locally
1. Copy .env.example file to .env file and change the password for Postgress as your wish,  in .env file
2. Start Snowflake, Kafka and Application by running : `docker compose up -d`
3. POST sample:
```json
{"transactionId":"TX-1001","amount":125.50,"currency":"USD","settlementDate":"2099-01-01"}
```

## Tests
`mvn test` includes validator/service unit tests and a PostgreSQL Testcontainers test for atomic duplicate claiming.

## Terraform
`terraform/` provisions an AWS RDS PostgreSQL instance in existing private subnets. Copy `terraform.tfvars.example`, then run `terraform init && terraform apply`.
