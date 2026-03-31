create table if not exists booking_items (
    id uuid primary key,
    booking_id uuid not null,
    item_type varchar(30) not null,
    item_name varchar(100) not null,
    quantity int not null,
    unit_price numeric(12,2) not null,
    total_price numeric(12,2) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint fk_booking_items_booking
        foreign key (booking_id) references bookings(id) on delete cascade
);
