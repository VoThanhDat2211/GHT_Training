package com.multimodule.booking.domain.exception;

public class BookingNotFoundException extends BookingDomainException{
    public BookingNotFoundException(String message) {
        super(message);
    }
}
