package com.multimodule.booking.application.mapper;

import com.multimodule.booking.application.dto.response.BookingItemResponse;
import com.multimodule.booking.application.dto.response.BookingResponse;
import com.multimodule.booking.domain.entity.Booking;
import com.multimodule.booking.domain.entity.BookingItem;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class BookingDataMapper {

    public BookingResponse bookingToBookingResponse(Booking booking) {
        return new BookingResponse(
                (UUID) booking.getId().getValue(),
                booking.getUserId(),
                booking.getBookingCode(),
                booking.getStatus(),
                booking.getTotalAmount(),
                booking.getCurrency(),
                booking.getBookedAt(),
                booking.getConfirmedAt(),
                booking.getCancelledAt(),
                booking.getExpiresAt(),
                booking.getCancelReason(),
                booking.getCreatedAt(),
                booking.getUpdatedAt(),
                booking.getItems().stream()
                        .map(this::bookingItemToBookingItemResponse)
                        .toList()
        );
    }

    public BookingItemResponse bookingItemToBookingItemResponse(BookingItem bookingItem) {
        return new BookingItemResponse(
                bookingItem.getId(),
                (UUID) bookingItem.getBookingId().getValue(),
                bookingItem.getItemType(),
                bookingItem.getItemName(),
                bookingItem.getQuantity(),
                bookingItem.getUnitPrice(),
                bookingItem.getTotalPrice(),
                bookingItem.getCreatedAt(),
                bookingItem.getUpdatedAt()
        );
    }
}
