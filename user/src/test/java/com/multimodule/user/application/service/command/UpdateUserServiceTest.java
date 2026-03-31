package com.multimodule.user.application.service.command;

import com.multimodule.user.application.dto.command.UpdateUserCommand;
import com.multimodule.user.application.mapper.UserDataMapper;
import com.multimodule.user.application.port.output.UserRepository;
import com.multimodule.user.domain.entity.User;
import com.multimodule.user.domain.exception.UserConflictException;
import com.multimodule.user.domain.valueobject.UserId;
import com.multimodule.user.domain.valueobject.UserStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UpdateUserServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UpdateUserService updateUserService =
            new UpdateUserService(userRepository, new UserDataMapper());

    @Test
    void shouldUpdateUserProfile() {
        UUID userId = UUID.randomUUID();
        User user = User.restore(
                UserId.of(userId),
                "johnny",
                "john@mail.com",
                "John Doe",
                "0123",
                UserStatus.ACTIVE,
                false,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().minusDays(1),
                null
        );
        UpdateUserCommand command = new UpdateUserCommand("johnny2", "john2@mail.com", "John Doe 2", "0456");
        when(userRepository.findById(UserId.of(userId))).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailExcludingId(command.email(), user.getId())).thenReturn(false);
        when(userRepository.existsByUsernameExcludingId(command.username(), user.getId())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = updateUserService.updateUser(userId, command);

        verify(userRepository).save(any(User.class));
        assertEquals("johnny2", response.username());
        assertEquals("john2@mail.com", response.email());
    }

    @Test
    void shouldRejectDuplicateUsername() {
        UUID userId = UUID.randomUUID();
        User user = User.create("johnny", "john@mail.com", "John Doe", null);
        UpdateUserCommand command = new UpdateUserCommand("existing-user", "john2@mail.com", "John Doe 2", null);
        when(userRepository.findById(any(UserId.class))).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailExcludingId(command.email(), user.getId())).thenReturn(false);
        when(userRepository.existsByUsernameExcludingId(command.username(), user.getId())).thenReturn(true);

        assertThrows(UserConflictException.class, () -> updateUserService.updateUser(userId, command));
        verify(userRepository, never()).save(any(User.class));
    }
}
