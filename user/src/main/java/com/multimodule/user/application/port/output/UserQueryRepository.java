package com.multimodule.user.application.port.output;

import com.multimodule.user.domain.entity.User;
import com.multimodule.user.domain.valueobject.UserId;

import java.util.Optional;

// Output Port dành riêng cho read — impl sẽ check Redis trước, miss mới query PostgreSQL
public interface UserQueryRepository {

    Optional<User> findById(UserId userId);
}
