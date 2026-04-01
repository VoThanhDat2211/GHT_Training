package com.multimodule.booking.application.dto.response;

import com.multimodule.booking.domain.valueobject.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record BookingResponse(
        UUID id,
        UUID userId,
        String bookingCode,
        BookingStatus status,
        BigDecimal totalAmount,
        String currency,
        LocalDateTime bookedAt,
        LocalDateTime confirmedAt,
        LocalDateTime cancelledAt,
        LocalDateTime expiresAt,
        String cancelReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<BookingItemResponse> items
) {
}

