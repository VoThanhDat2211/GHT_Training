package com.multimodule.booking.domain.entity;

import com.multimodule.booking.domain.exception.BookingDomainException;
import com.multimodule.booking.domain.valueobject.BookingId;
import com.multimodule.booking.domain.valueobject.BookingStatus;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Booking implements Serializable {

    private final UUID userId;
    private final BookingId id;
    private final String bookingCode;
    private BookingStatus status;
    private final BigDecimal totalAmount;
    private final String currency;
    private final LocalDateTime bookedAt;
    private final List<BookingItem> items;
    private LocalDateTime confirmedAt;
    private LocalDateTime cancelledAt;
    private final LocalDateTime expiresAt;
    private String cancelReason;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Booking(
            UUID userId,
            BookingId id,
            String bookingCode,
            BookingStatus status,
            BigDecimal totalAmount,
            String currency,
            LocalDateTime bookedAt,
            List<BookingItem> items,
            LocalDateTime confirmedAt,
            LocalDateTime cancelledAt,
            LocalDateTime expiresAt,
            String cancelReason,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.userId = Objects.requireNonNull(userId, "User id is required");
        this.id = Objects.requireNonNull(id, "Booking id is required");
        this.bookingCode = requireText(bookingCode, "Booking code is required");
        this.status = Objects.requireNonNull(status, "Booking status is required");
        this.totalAmount = requireNonNegative(totalAmount, "Total amount must be greater than or equal to zero");
        this.currency = requireText(currency, "Currency is required");
        this.bookedAt = Objects.requireNonNull(bookedAt, "Booked at is required");
        this.items = List.copyOf(requireNonEmptyItems(items));
        this.confirmedAt = confirmedAt;
        this.cancelledAt = cancelledAt;
        this.expiresAt = Objects.requireNonNull(expiresAt, "Expires at is required");
        this.cancelReason = trimToNull(cancelReason);
        this.createdAt = Objects.requireNonNull(createdAt, "Created at is required");
        this.updatedAt = Objects.requireNonNull(updatedAt, "Updated at is required");

        validateItemsBelongToBooking();
        validateTotalAmountConsistency();
        validateTimeConsistency();
        validateStatusConsistency();
    }

    public static Booking create(
            BookingId id,
            UUID userId,
            String bookingCode,
            BigDecimal totalAmount,
            String currency,
            LocalDateTime bookedAt,
            List<BookingItem> items,
            LocalDateTime expiresAt,
            LocalDateTime createdAt
    ) {
        return new Booking(
                userId,
                id,
                bookingCode,
                BookingStatus.PENDING,
                totalAmount,
                currency,
                bookedAt,
                items,
                null,
                null,
                expiresAt,
                null,
                createdAt,
                createdAt
        );
    }

    public static Booking restore(
            UUID userId,
            BookingId id,
            String bookingCode,
            BookingStatus status,
            BigDecimal totalAmount,
            String currency,
            LocalDateTime bookedAt,
            List<BookingItem> items,
            LocalDateTime confirmedAt,
            LocalDateTime cancelledAt,
            LocalDateTime expiresAt,
            String cancelReason,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        return new Booking(
                userId,
                id,
                bookingCode,
                status,
                totalAmount,
                currency,
                bookedAt,
                items,
                confirmedAt,
                cancelledAt,
                expiresAt,
                cancelReason,
                createdAt,
                updatedAt
        );
    }

    public void confirm(LocalDateTime confirmedAt) {
        requirePending("Only pending booking can be confirmed");

        LocalDateTime confirmationTime = Objects.requireNonNull(confirmedAt, "Confirmed at is required");
        if (confirmationTime.isAfter(expiresAt)) {
            throw new BookingDomainException("Expired booking cannot be confirmed");
        }

        this.status = BookingStatus.CONFIRMED;
        this.confirmedAt = confirmationTime;
        this.cancelledAt = null;
        this.cancelReason = null;
        this.updatedAt = confirmationTime;
    }

    public void cancel(String cancelReason, LocalDateTime cancelledAt) {
        if (status == BookingStatus.CANCELLED) {
            throw new BookingDomainException("Booking is already cancelled");
        }
        if (status == BookingStatus.EXPIRED) {
            throw new BookingDomainException("Expired booking cannot be cancelled");
        }

        LocalDateTime cancellationTime = Objects.requireNonNull(cancelledAt, "Cancelled at is required");
        this.status = BookingStatus.CANCELLED;
        this.cancelledAt = cancellationTime;
        this.cancelReason = requireText(cancelReason, "Cancel reason is required");
        this.updatedAt = cancellationTime;
    }

    public void expire(LocalDateTime expiredAt) {
        requirePending("Only pending booking can be expired");

        LocalDateTime expirationTime = Objects.requireNonNull(expiredAt, "Expired at is required");
        if (expirationTime.isBefore(expiresAt)) {
            throw new BookingDomainException("Booking cannot be expired before expiresAt");
        }

        this.status = BookingStatus.EXPIRED;
        this.updatedAt = expirationTime;
    }

    public boolean isExpiredAt(LocalDateTime referenceTime) {
        return !expiresAt.isAfter(Objects.requireNonNull(referenceTime, "Reference time is required"));
    }

    public UUID getUserId() {
        return userId;
    }

    public BookingId getId() {
        return id;
    }

    public String getBookingCode() {
        return bookingCode;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public LocalDateTime getBookedAt() {
        return bookedAt;
    }

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public List<BookingItem> getItems() {
        return items;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    private void requirePending(String message) {
        if (status != BookingStatus.PENDING) {
            throw new BookingDomainException(message);
        }
    }

    private void validateTimeConsistency() {
        if (expiresAt.isBefore(bookedAt)) {
            throw new BookingDomainException("Expires at must be after or equal to bookedAt");
        }
        if (createdAt.isAfter(updatedAt)) {
            throw new BookingDomainException("Updated at must be after or equal to createdAt");
        }
        if (confirmedAt != null && confirmedAt.isBefore(bookedAt)) {
            throw new BookingDomainException("Confirmed at must be after or equal to bookedAt");
        }
        if (cancelledAt != null && cancelledAt.isBefore(bookedAt)) {
            throw new BookingDomainException("Cancelled at must be after or equal to bookedAt");
        }
    }

    private void validateStatusConsistency() {
        if (status == BookingStatus.PENDING && (confirmedAt != null || cancelledAt != null || cancelReason != null)) {
            throw new BookingDomainException("Pending booking must not have confirmation or cancellation information");
        }
        if (status == BookingStatus.CONFIRMED && confirmedAt == null) {
            throw new BookingDomainException("Confirmed booking must have confirmedAt");
        }
        if (status == BookingStatus.CANCELLED) {
            if (cancelledAt == null) {
                throw new BookingDomainException("Cancelled booking must have cancelledAt");
            }
            if (cancelReason == null) {
                throw new BookingDomainException("Cancelled booking must have cancelReason");
            }
        }
        if (status == BookingStatus.EXPIRED && confirmedAt != null) {
            throw new BookingDomainException("Expired booking must not have confirmedAt");
        }
    }

    private void validateItemsBelongToBooking() {
        for (BookingItem item : items) {
            if (!id.equals(item.getBookingId())) {
                throw new BookingDomainException("All booking items must belong to the same booking");
            }
        }
    }

    private void validateTotalAmountConsistency() {
        BigDecimal totalItemAmount = items.stream()
                .map(BookingItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalAmount.compareTo(totalItemAmount) != 0) {
            throw new BookingDomainException("Total amount must equal the sum of booking item total prices");
        }
    }

    private static String requireText(String value, String message) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new BookingDomainException(message);
        }
        return trimmed;
    }

    private static BigDecimal requireNonNegative(BigDecimal value, String message) {
        BigDecimal amount = Objects.requireNonNull(value, "Total amount is required");
        if (amount.signum() < 0) {
            throw new BookingDomainException(message);
        }
        return amount;
    }

    private static List<BookingItem> requireNonEmptyItems(List<BookingItem> items) {
        List<BookingItem> bookingItems = Objects.requireNonNull(items, "Booking items are required");
        if (bookingItems.isEmpty()) {
            throw new BookingDomainException("Booking must contain at least one item");
        }
        return bookingItems;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
