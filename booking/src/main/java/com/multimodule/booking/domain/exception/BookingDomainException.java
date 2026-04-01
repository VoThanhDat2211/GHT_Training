package com.multimodule.booking.domain.exception;

import com.multimodule.common.exception.DomainException;

public class BookingDomainException extends DomainException {
    public BookingDomainException(String message) {
        super(message);
    }
}
