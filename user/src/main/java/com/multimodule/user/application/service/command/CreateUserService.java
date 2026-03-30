package com.multimodule.user.application.service.command;

import com.multimodule.user.application.dto.command.CreateUserCommand;
import com.multimodule.user.application.dto.response.UserResponse;
import com.multimodule.user.application.mapper.UserDataMapper;
import com.multimodule.user.application.port.input.command.CreateUserUseCase;
import com.multimodule.user.application.port.output.UserMessagePublisher;
import com.multimodule.user.application.port.output.UserRepository;
import com.multimodule.user.domain.entity.User;
import com.multimodule.user.domain.event.UserCreatedEvent;
import com.multimodule.user.domain.exception.UserDomainException;
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
        if (userRepository.existsByEmail(command.email())) {
            throw new UserDomainException("Email already exists: " + command.email());
        }

        User user = User.create(command.name(), command.email());
        User savedUser = userRepository.save(user);

        userMessagePublisher.publishUserCreatedEvent(new UserCreatedEvent(savedUser));
        log.info("User created with id: {}", savedUser.getId().getValue());

        return userDataMapper.userToUserResponse(savedUser);
    }
}
