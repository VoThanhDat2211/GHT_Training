package com.multimodule.user.adapter.in.web.mapper;

import com.multimodule.user.adapter.in.web.dto.request.CreateUserRequest;
import com.multimodule.user.adapter.in.web.dto.response.UserRestResponse;
import com.multimodule.user.application.dto.command.CreateUserCommand;
import com.multimodule.user.application.dto.response.UserResponse;
import org.springframework.stereotype.Component;

@Component
public class UserWebMapper {

    public CreateUserCommand toCommand(CreateUserRequest request) {
        return new CreateUserCommand(request.name(), request.email());
    }

    public UserRestResponse toRestResponse(UserResponse response) {
        return new UserRestResponse(
                response.id(),
                response.name(),
                response.email(),
                response.status().name(),
                response.createdAt()
        );
    }
}
