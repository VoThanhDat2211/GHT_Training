package com.multimodule.common.domain.event;

import java.time.LocalDateTime;

public interface DomainEvent {

    String getEventType();

    LocalDateTime getOccurredAt();
}
