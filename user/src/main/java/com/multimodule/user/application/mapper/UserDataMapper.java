package com.multimodule.user.application.mapper;

import com.multimodule.user.application.dto.response.UserResponse;
import com.multimodule.user.domain.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserDataMapper {

    public UserResponse userToUserResponse(User user) {
        return new UserResponse(
                user.getId().getValue(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getPhoneNumber(),
                user.getStatus(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
