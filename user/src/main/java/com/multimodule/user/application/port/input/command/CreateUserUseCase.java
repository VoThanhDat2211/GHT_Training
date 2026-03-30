package com.multimodule.user.application.port.input.command;

import com.multimodule.user.application.dto.command.CreateUserCommand;
import com.multimodule.user.application.dto.response.UserResponse;

public interface CreateUserUseCase {

    UserResponse createUser(CreateUserCommand command);
}
