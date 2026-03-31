create table if not exists tickets (
    id uuid primary key,
    booking_id uuid not null,
    user_id uuid not null,
    ticket_code varchar(30) not null unique,
    ticket_type varchar(30) not null,
    status varchar(20) not null,
    issued_at timestamptz not null,
    used_at timestamptz,
    cancelled_at timestamptz,
    seat_number varchar(20),
    qr_code varchar(255),
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint fk_tickets_booking
        foreign key (booking_id) references bookings(id),
    constraint fk_tickets_user
        foreign key (user_id) references users(id)
);
