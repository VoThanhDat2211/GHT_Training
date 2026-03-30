package com.multimodule.user.application.service.command;

import com.multimodule.user.application.port.input.command.DeleteUserUseCase;
import com.multimodule.user.application.port.output.UserRepository;
import com.multimodule.user.domain.entity.User;
import com.multimodule.user.domain.exception.UserDomainException;
import com.multimodule.user.domain.valueobject.UserId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeleteUserService implements DeleteUserUseCase {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public void deleteUser(UUID userId) {
        User user = userRepository.findById(UserId.of(userId))
                .orElseThrow(() -> new UserDomainException("User not found with id: " + userId));
        user.delete();
        userRepository.save(user);
        log.info("User deleted with id: {}", userId);
    }
}
