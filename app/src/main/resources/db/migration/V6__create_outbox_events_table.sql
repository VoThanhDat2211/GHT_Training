create table if not exists outbox_events (
    id uuid primary key,
    aggregate_type varchar(50) not null,
    aggregate_id uuid not null,
    event_type varchar(100) not null,
    payload jsonb not null,
    status varchar(20) not null,
    created_at timestamptz not null,
    published_at timestamptz
);
