package com.multimodule.booking.application.port.output;

import com.multimodule.booking.domain.entity.Booking;
import com.multimodule.booking.domain.entity.BookingItem;
import com.multimodule.booking.domain.valueobject.BookingId;

import java.util.Optional;
import java.util.UUID;

public interface BookingQueryRepository {
    Optional<Booking> findById(BookingId id);
}
