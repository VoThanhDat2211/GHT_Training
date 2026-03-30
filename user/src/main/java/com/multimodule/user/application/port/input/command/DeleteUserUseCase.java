package com.multimodule.user.application.port.input.command;

import java.util.UUID;

public interface DeleteUserUseCase {

    void deleteUser(UUID userId);
}
