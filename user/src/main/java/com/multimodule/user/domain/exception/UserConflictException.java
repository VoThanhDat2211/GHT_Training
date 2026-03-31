package com.multimodule.user.domain.exception;

public class UserConflictException extends UserDomainException {

    public UserConflictException(String message) {
        super(message);
    }
}
