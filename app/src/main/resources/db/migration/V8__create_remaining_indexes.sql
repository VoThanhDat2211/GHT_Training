create index if not exists idx_bookings_user_id on bookings(user_id);
create index if not exists idx_bookings_status on bookings(status);
create index if not exists idx_bookings_booking_code on bookings(booking_code);
create index if not exists idx_bookings_expires_at on bookings(expires_at);

create index if not exists idx_booking_items_booking_id on booking_items(booking_id);

create index if not exists idx_tickets_booking_id on tickets(booking_id);
create index if not exists idx_tickets_user_id on tickets(user_id);
create index if not exists idx_tickets_ticket_code on tickets(ticket_code);
create index if not exists idx_tickets_status on tickets(status);

create index if not exists idx_outbox_status on outbox_events(status);
create index if not exists idx_outbox_aggregate_id on outbox_events(aggregate_id);
create index if not exists idx_outbox_event_type on outbox_events(event_type);
