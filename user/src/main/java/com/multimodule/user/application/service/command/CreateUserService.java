package com.multimodule.user.application.service.command;

import com.multimodule.user.application.dto.command.CreateUserCommand;
import com.multimodule.user.application.dto.response.UserResponse;
import com.multimodule.user.application.mapper.UserDataMapper;
import com.multimodule.user.application.port.input.command.CreateUserUseCase;
import com.multimodule.user.application.port.output.UserMessagePublisher;
import com.multimodule.user.application.port.output.UserRepository;
import com.multimodule.user.domain.entity.User;
import com.multimodule.user.domain.event.UserCreatedEvent;
import com.multimodule.user.domain.exception.UserConflictException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateUserService implements CreateUserUseCase {

    private final UserRepository userRepository;
    private final UserMessagePublisher userMessagePublisher;
    private final UserDataMapper userDataMapper;

    @Override
    @Transactional
    public UserResponse createUser(CreateUserCommand command) {
        String normalizedEmail = normalizeEmail(command.email());
        String normalizedUsername = normalize(command.username());

        ensureUniqueEmail(normalizedEmail);
        ensureUniqueUsername(normalizedUsername);

        User user = User.create(normalizedUsername, normalizedEmail, command.fullName(), command.phoneNumber());
        User savedUser = userRepository.save(user);

        userMessagePublisher.publishUserCreatedEvent(UserCreatedEvent.from(savedUser));
        log.info("User created with id: {}", savedUser.getId().getValue());

        return userDataMapper.userToUserResponse(savedUser);
    }

    private void ensureUniqueEmail(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new UserConflictException("Email already exists: " + email);
        }
    }

    private void ensureUniqueUsername(String username) {
        if (userRepository.existsByUsername(username)) {
            throw new UserConflictException("Username already exists: " + username);
        }
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeEmail(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : normalized.toLowerCase();
    }
}
