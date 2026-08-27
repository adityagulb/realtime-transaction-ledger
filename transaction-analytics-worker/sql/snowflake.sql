create table if not exists TRANSACTION_ANALYTICS (
 EVENT_ID varchar primary key, LEDGER_ID varchar not null, TRANSACTION_ID varchar not null,
 AMOUNT number(19,4) not null, CURRENCY varchar(3) not null, SETTLEMENT_DATE date not null,
 STATUS varchar(30) not null, OCCURRED_AT timestamp_tz not null
);
