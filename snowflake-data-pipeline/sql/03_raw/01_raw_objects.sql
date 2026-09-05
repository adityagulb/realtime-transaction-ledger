USE DATABASE BANKING_RAW;
USE SCHEMA CORE;

CREATE TABLE IF NOT EXISTS TRANSACTION_EVENT_RAW (
    EVENT_ID                    VARCHAR         NOT NULL,
    TRANSACTION_ID              VARCHAR         NOT NULL,
    EVENT_TYPE                  VARCHAR         NOT NULL,
    ACCOUNT_ID                  VARCHAR         NOT NULL,
    COUNTERPARTY_ACCOUNT_ID     VARCHAR,
    AMOUNT                      NUMBER(18,2)    NOT NULL,
    CURRENCY                    VARCHAR(3)      NOT NULL,
    TRANSACTION_TS              TIMESTAMP_TZ    NOT NULL,
    EVENT_VERSION               NUMBER          NOT NULL DEFAULT 1,
    SOURCE_SYSTEM               VARCHAR,
    SOURCE_FILE                 VARCHAR,
    SOURCE_ROW_NUMBER           NUMBER,
    ORIGINAL_PAYLOAD            VARIANT,
    RAW_CREATED_TS              TIMESTAMP_TZ    NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    RAW_UPDATED_TS              TIMESTAMP_TZ    NOT NULL DEFAULT CURRENT_TIMESTAMP()
);

-- EVENT_ID is the logical business key used by the MERGE.
-- Standard-table PK/unique constraints in Snowflake are informational rather than enforced,
-- so idempotency is implemented explicitly in the MERGE instead of relying on a constraint.

USE SCHEMA BANKING_RAW.CDC;

-- Each downstream logical consumer gets an independent stream.
CREATE OR REPLACE STREAM RAW_DIM_ACCOUNT_STREAM
  ON TABLE BANKING_RAW.CORE.TRANSACTION_EVENT_RAW;

CREATE OR REPLACE STREAM RAW_FACT_TRANSACTION_STREAM
  ON TABLE BANKING_RAW.CORE.TRANSACTION_EVENT_RAW;

CREATE OR REPLACE STREAM RAW_BRIDGE_STREAM
  ON TABLE BANKING_RAW.CORE.TRANSACTION_EVENT_RAW;
