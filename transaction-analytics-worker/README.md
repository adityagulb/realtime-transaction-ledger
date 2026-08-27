# Transaction Analytics Worker

Kafka consumer for VP Case Study 1. It receives transaction events, retries transient failures, publishes exhausted failures to `<topic>.DLT`, and writes analytics records through an `AnalyticsSink` abstraction.

## Sinks
- `ANALYTICS_SINK=log` (default): local/demo mode, no Snowflake required.
- `SPRING_PROFILES_ACTIVE=snowflake`: enables the JDBC Snowflake sink and datasource. Configure `SNOWFLAKE_URL`, `SNOWFLAKE_USER`, and `SNOWFLAKE_PASSWORD`.

The Snowflake writer uses `MERGE` keyed by `EVENT_ID`, making repeated Kafka delivery idempotent.

## Run locally
1. Copy .env.example file to .env file and change the password for Postgress as your wish,  in .env file
2. Start Snowflake, Kafka and Application by running : `docker compose up -d`


## Kafka retry / DLQ
Spring Kafka `DefaultErrorHandler` uses exponential backoff. After retries are exhausted, the record is sent to `<original-topic>.DLT`.

## SQL
Run `sql/snowflake.sql` once in Snowflake.

## Terraform
`terraform/` provisions an AWS MSK Serverless cluster in existing private subnets. The application will need the appropriate IAM Kafka permissions when using MSK IAM authentication.

## Tests
`mvn test` covers event parsing/forwarding and idempotent Snowflake MERGE behavior.
