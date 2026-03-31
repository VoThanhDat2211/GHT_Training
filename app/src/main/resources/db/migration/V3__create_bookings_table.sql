create table if not exists bookings (
    id uuid primary key,
    user_id uuid not null,
    booking_code varchar(30) not null unique,
    status varchar(20) not null,
    total_amount numeric(12,2) not null,
    currency varchar(10) not null default 'VND',
    booked_at timestamptz not null,
    confirmed_at timestamptz,
    cancelled_at timestamptz,
    expires_at timestamptz,
    cancel_reason varchar(255),
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint fk_bookings_user
        foreign key (user_id) references users(id)
);
