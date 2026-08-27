create table ledger_transaction (id uuid primary key, transaction_id varchar(100) not null, amount numeric(19,4) not null check(amount>0), currency varchar(3) not null, settlement_date date not null, status varchar(30) not null, created_at timestamptz not null);
create index idx_ledger_tx_created on ledger_transaction(transaction_id,created_at);
create table deduplication_key (transaction_id varchar(100) primary key, expires_at timestamptz not null);
create index idx_dedup_expires on deduplication_key(expires_at);
create table outbox_event (id uuid primary key, aggregate_id uuid not null, event_type varchar(100) not null, payload text not null, status varchar(30) not null, retry_count integer not null default 0, created_at timestamptz not null, published_at timestamptz null);
create index idx_outbox_status_created on outbox_event(status,created_at);
create table rejected_transaction (id uuid primary key, transaction_id varchar(100), error_code varchar(100) not null, error_message varchar(500) not null, payload text not null, created_at timestamptz not null);
