package com.multimodule.user.application.dto.response;

import com.multimodule.user.domain.valueobject.UserStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String name,
        String email,
        UserStatus status,
        LocalDateTime createdAt
) {}
