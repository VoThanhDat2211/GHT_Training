package com.multimodule.booking.application.port.input.query;

import com.multimodule.booking.application.dto.response.BookingResponse;

import java.util.UUID;

public interface GetBookingByIdQuery {
    BookingResponse getBookingById(UUID id);
}
