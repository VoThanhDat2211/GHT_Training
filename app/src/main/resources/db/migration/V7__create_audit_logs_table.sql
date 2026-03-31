create table if not exists audit_logs (
    id uuid primary key,
    aggregate_type varchar(50) not null,
    aggregate_id uuid not null,
    action varchar(50) not null,
    actor varchar(100),
    metadata jsonb,
    created_at timestamptz not null
);
