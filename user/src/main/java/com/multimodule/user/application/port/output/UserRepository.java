package com.multimodule.user.application.port.output;

import com.multimodule.user.domain.entity.User;
import com.multimodule.user.domain.valueobject.UserId;

import java.util.Optional;

public interface UserRepository {

    User save(User user);

    Optional<User> findById(UserId userId);

    boolean existsByEmail(String email);

    boolean existsByEmailExcludingId(String email, UserId userId);

    boolean existsByUsername(String username);

    boolean existsByUsernameExcludingId(String username, UserId userId);
}
