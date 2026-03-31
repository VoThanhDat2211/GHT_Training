package com.multimodule.user.application.service.command;

import com.multimodule.user.application.dto.response.UserResponse;
import com.multimodule.user.application.mapper.UserDataMapper;
import com.multimodule.user.application.port.input.command.BlockUserUseCase;
import com.multimodule.user.application.port.output.UserRepository;
import com.multimodule.user.domain.entity.User;
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
public class BlockUserService implements BlockUserUseCase {

    private final UserRepository userRepository;
    private final UserDataMapper userDataMapper;

    @Override
    @Transactional
    public UserResponse blockUser(UUID userId) {
        User user = userRepository.findById(UserId.of(userId))
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));
        user.block();
        User savedUser = userRepository.save(user);
        log.info("User blocked with id: {}", userId);
        return userDataMapper.userToUserResponse(savedUser);
    }
}
