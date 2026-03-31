package com.multimodule.user.adapter.in.web.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserRestResponse(
        UUID id,
        String username,
        String email,
        String fullName,
        String phoneNumber,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
