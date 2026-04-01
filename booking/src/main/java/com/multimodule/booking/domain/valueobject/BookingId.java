package com.multimodule.booking.domain.valueobject;

import com.multimodule.common.domain.valueobject.BaseId;

import java.util.UUID;

public class BookingId extends BaseId<UUID> {
    public BookingId(UUID value) {
        super(value);
    }

    public static BookingId of(UUID value) {
        return new BookingId(value);
    }
}
