package com.multimodule.user.application.port.input.query;

import com.multimodule.user.application.dto.response.UserResponse;

import java.util.UUID;

public interface GetUserByIdQuery {

    UserResponse getUserById(UUID userId);
}
