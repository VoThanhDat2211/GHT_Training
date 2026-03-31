create table if not exists users (
    id uuid primary key,
    username varchar(50) not null unique,
    email varchar(100) not null unique,
    full_name varchar(100) not null,
    phone_number varchar(20),
    status varchar(20) not null,
    is_deleted boolean not null default false,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted_at timestamp
);
