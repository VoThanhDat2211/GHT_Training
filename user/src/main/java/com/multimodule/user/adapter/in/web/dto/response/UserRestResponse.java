package com.multimodule.user.adapter.in.web.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserRestResponse(
        UUID id,
        String name,
        String email,
        String status,
        LocalDateTime createdAt
) {}
