package com.multimodule.user.application.service.command;

import com.multimodule.user.application.dto.command.UpdateUserCommand;
import com.multimodule.user.application.dto.response.UserResponse;
import com.multimodule.user.application.mapper.UserDataMapper;
import com.multimodule.user.application.port.input.command.UpdateUserUseCase;
import com.multimodule.user.application.port.output.UserRepository;
import com.multimodule.user.domain.entity.User;
import com.multimodule.user.domain.exception.UserConflictException;
import com.multimodule.user.domain.exception.UserNotFoundException;
import com.multimodule.user.domain.valueobject.UserId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateUserService implements UpdateUserUseCase {

    private final UserRepository userRepository;
    private final UserDataMapper userDataMapper;

    @Override
    @Transactional
    public UserResponse updateUser(UUID userId, UpdateUserCommand command) {
        User user = userRepository.findById(UserId.of(userId))
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        ensureUniqueEmail(command.email(), user.getId());
        ensureUniqueUsername(command.username(), user.getId());

        user.updateProfile(command.username(), command.email(), command.fullName(), command.phoneNumber());
        User savedUser = userRepository.save(user);
        log.info("User updated with id: {}", userId);
        return userDataMapper.userToUserResponse(savedUser);
    }

    private void ensureUniqueEmail(String email, UserId userId) {
        if (userRepository.existsByEmailExcludingId(email, userId)) {
            throw new UserConflictException("Email already exists: " + email);
        }
    }

    private void ensureUniqueUsername(String username, UserId userId) {
        if (userRepository.existsByUsernameExcludingId(username, userId)) {
            throw new UserConflictException("Username already exists: " + username);
        }
    }
}
