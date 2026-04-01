package com.multimodule.booking.domain.entity;

import com.multimodule.booking.domain.exception.BookingDomainException;
import com.multimodule.booking.domain.valueobject.BookingId;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class BookingItem implements Serializable {

    private final UUID id;
    private final BookingId bookingId;
    private final String itemType;
    private final String itemName;
    private final int quantity;
    private final BigDecimal unitPrice;
    private final BigDecimal totalPrice;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private BookingItem(
            UUID id,
            BookingId bookingId,
            String itemType,
            String itemName,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal totalPrice,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "Booking item id is required");
        this.bookingId = Objects.requireNonNull(bookingId, "Booking id is required");
        this.itemType = requireText(itemType, "Item type is required");
        this.itemName = requireText(itemName, "Item name is required");
        this.quantity = requirePositive(quantity, "Quantity must be greater than zero");
        this.unitPrice = requireNonNegative(unitPrice, "Unit price must be greater than or equal to zero");
        this.totalPrice = requireNonNegative(totalPrice, "Total price must be greater than or equal to zero");
        this.createdAt = Objects.requireNonNull(createdAt, "Created at is required");
        this.updatedAt = Objects.requireNonNull(updatedAt, "Updated at is required");

        validatePriceConsistency();
        validateTimeConsistency();
    }

    public static BookingItem create(
            UUID id,
            BookingId bookingId,
            String itemType,
            String itemName,
            int quantity,
            BigDecimal unitPrice,
            LocalDateTime createdAt
    ) {
        return new BookingItem(
                id,
                bookingId,
                itemType,
                itemName,
                quantity,
                unitPrice,
                unitPrice.multiply(BigDecimal.valueOf(quantity)),
                createdAt,
                createdAt
        );
    }

    public static BookingItem restore(
            UUID id,
            BookingId bookingId,
            String itemType,
            String itemName,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal totalPrice,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        return new BookingItem(
                id,
                bookingId,
                itemType,
                itemName,
                quantity,
                unitPrice,
                totalPrice,
                createdAt,
                updatedAt
        );
    }

    public UUID getId() {
        return id;
    }

    public BookingId getBookingId() {
        return bookingId;
    }

    public String getItemType() {
        return itemType;
    }

    public String getItemName() {
        return itemName;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    private void validatePriceConsistency() {
        BigDecimal expectedTotalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
        if (expectedTotalPrice.compareTo(totalPrice) != 0) {
            throw new BookingDomainException("Total price must equal quantity multiplied by unit price");
        }
    }

    private void validateTimeConsistency() {
        if (createdAt.isAfter(updatedAt)) {
            throw new BookingDomainException("Updated at must be after or equal to createdAt");
        }
    }

    private static String requireText(String value, String message) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new BookingDomainException(message);
        }
        return trimmed;
    }

    private static int requirePositive(int value, String message) {
        if (value <= 0) {
            throw new BookingDomainException(message);
        }
        return value;
    }

    private static BigDecimal requireNonNegative(BigDecimal value, String message) {
        BigDecimal amount = Objects.requireNonNull(value, "Amount is required");
        if (amount.signum() < 0) {
            throw new BookingDomainException(message);
        }
        return amount;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
