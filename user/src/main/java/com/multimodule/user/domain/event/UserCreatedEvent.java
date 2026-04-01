package com.multimodule.user.domain.event;

import com.multimodule.common.domain.event.DomainEvent;
import com.multimodule.user.domain.entity.User;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

public class UserCreatedEvent implements DomainEvent, Serializable {

    private final UUID userId;
    private final String username;
    private final String email;
    private final String fullName;
    private final String phoneNumber;
    private final LocalDateTime occurredAt;

    private UserCreatedEvent(
            UUID userId,
            String username,
            String email,
            String fullName,
            String phoneNumber,
            LocalDateTime occurredAt
    ) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.occurredAt = occurredAt;
    }

    public static UserCreatedEvent from(User user) {
        return new UserCreatedEvent(
                user.getId().getValue(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getPhoneNumber(),
                LocalDateTime.now()
        );
    }

    public UUID getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    @Override
    public String getEventType() {
        return "UserCreated";
    }

    @Override
    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
}
