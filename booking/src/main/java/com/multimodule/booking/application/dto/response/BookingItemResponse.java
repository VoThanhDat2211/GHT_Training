package com.multimodule.booking.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record BookingItemResponse(
        UUID id,
        UUID bookingId,
        String itemType,
        String itemName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
