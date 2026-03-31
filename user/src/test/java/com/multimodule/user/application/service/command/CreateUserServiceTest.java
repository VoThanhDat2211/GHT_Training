package com.multimodule.user.application.service.command;

import com.multimodule.user.application.dto.command.CreateUserCommand;
import com.multimodule.user.application.mapper.UserDataMapper;
import com.multimodule.user.application.port.output.UserMessagePublisher;
import com.multimodule.user.application.port.output.UserRepository;
import com.multimodule.user.domain.entity.User;
import com.multimodule.user.domain.exception.UserConflictException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreateUserServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserMessagePublisher userMessagePublisher = mock(UserMessagePublisher.class);
    private final CreateUserService createUserService =
            new CreateUserService(userRepository, userMessagePublisher, new UserDataMapper());

    @Test
    void shouldCreateUserWhenUsernameAndEmailAreUnique() {
        CreateUserCommand command = new CreateUserCommand("johnny", "john@mail.com", "John Doe", "0123");
        when(userRepository.existsByEmail(command.email())).thenReturn(false);
        when(userRepository.existsByUsername(command.username())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = createUserService.createUser(command);

        verify(userRepository).save(any(User.class));
        verify(userMessagePublisher).publishUserCreatedEvent(any());
        assertEquals("johnny", response.username());
        assertEquals("john@mail.com", response.email());
    }

    @Test
    void shouldRejectDuplicateEmail() {
        CreateUserCommand command = new CreateUserCommand("johnny", "john@mail.com", "John Doe", "0123");
        when(userRepository.existsByEmail(command.email())).thenReturn(true);

        assertThrows(UserConflictException.class, () -> createUserService.createUser(command));
        verify(userRepository, never()).save(any(User.class));
    }
}
