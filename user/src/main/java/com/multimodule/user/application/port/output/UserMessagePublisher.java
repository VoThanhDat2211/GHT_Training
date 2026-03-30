package com.multimodule.user.application.port.output;

import com.multimodule.user.domain.event.UserCreatedEvent;

public interface UserMessagePublisher {

    void publishUserCreatedEvent(UserCreatedEvent event);
}
