package com.multimodule.user.application.port.input.command;

import com.multimodule.user.application.dto.response.UserResponse;

import java.util.UUID;

public interface ActivateUserUseCase {

    UserResponse activateUser(UUID userId);
}
