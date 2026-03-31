package com.multimodule.user.domain.event;

import com.multimodule.common.domain.event.DomainEvent;
import com.multimodule.user.domain.entity.User;

import java.io.Serializable;
import java.time.LocalDateTime;

public class UserCreatedEvent implements DomainEvent<User>, Serializable {

    private final User user;
    private final LocalDateTime occurredAt;

    public UserCreatedEvent(User user) {
        this.user = user;
        this.occurredAt = LocalDateTime.now();
    }

    @Override
    public User getEntity() {
        return user;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
}
