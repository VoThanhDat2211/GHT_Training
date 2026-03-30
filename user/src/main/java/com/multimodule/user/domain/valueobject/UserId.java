package com.multimodule.user.domain.valueobject;

import com.multimodule.common.domain.valueobject.BaseId;

import java.util.UUID;

public class UserId extends BaseId<UUID> {

    public UserId(UUID value) {
        super(value);
    }

    public static UserId of(UUID value) {
        return new UserId(value);
    }
}
