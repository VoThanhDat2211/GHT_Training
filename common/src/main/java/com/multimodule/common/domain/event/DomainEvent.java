package com.multimodule.common.domain.event;

public interface DomainEvent<T> {
    T getEntity();
}
