package com.multimodule.user.adapter.in.web.mapper;

import com.multimodule.user.adapter.in.web.dto.request.CreateUserRequest;
import com.multimodule.user.adapter.in.web.dto.request.UpdateUserRequest;
import com.multimodule.user.adapter.in.web.dto.response.UserRestResponse;
import com.multimodule.user.application.dto.command.CreateUserCommand;
import com.multimodule.user.application.dto.command.UpdateUserCommand;
import com.multimodule.user.application.dto.response.UserResponse;
import org.springframework.stereotype.Component;

@Component
public class UserWebMapper {

    public CreateUserCommand toCreateCommand(CreateUserRequest request) {
        return new CreateUserCommand(
                request.username(),
                request.email(),
                request.fullName(),
                request.phoneNumber()
        );
    }

    public UpdateUserCommand toUpdateCommand(UpdateUserRequest request) {
        return new UpdateUserCommand(
                request.username(),
                request.email(),
                request.fullName(),
                request.phoneNumber()
        );
    }

    public UserRestResponse toRestResponse(UserResponse response) {
        return new UserRestResponse(
                response.id(),
                response.username(),
                response.email(),
                response.fullName(),
                response.phoneNumber(),
                response.status().name(),
                response.createdAt(),
                response.updatedAt()
        );
    }
}
