package com.multimodule.user.application.service.query;

import com.multimodule.user.application.dto.response.UserResponse;
import com.multimodule.user.application.mapper.UserDataMapper;
import com.multimodule.user.application.port.input.query.GetUserByIdQuery;
import com.multimodule.user.application.port.output.UserQueryRepository;
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
public class GetUserByIdService implements GetUserByIdQuery {

    private final UserQueryRepository userQueryRepository;
    private final UserDataMapper userDataMapper;

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID userId) {
        return userQueryRepository.findById(UserId.of(userId))
                .map(userDataMapper::userToUserResponse)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));
    }
}
