package com.multimodule.user.application.port.input.command;

import com.multimodule.user.application.dto.command.UpdateUserCommand;
import com.multimodule.user.application.dto.response.UserResponse;

import java.util.UUID;

public interface UpdateUserUseCase {

    UserResponse updateUser(UUID userId, UpdateUserCommand command);
}
