package com.multimodule.user.domain.entity;

import com.multimodule.user.domain.exception.UserDomainException;
import com.multimodule.user.domain.valueobject.UserId;
import com.multimodule.user.domain.valueobject.UserStatus;

import java.time.LocalDateTime;

public class User {

    private UserId id;
    private String name;
    private String email;
    private UserStatus status;
    private LocalDateTime createdAt;

    private User() {}

    public User(String name, String email) {
        this.id = UserId.of(java.util.UUID.randomUUID());
        this.name = name;
        this.email = email;
        this.status = UserStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
        this.validate();
    }

    public static User create(String name, String email) {
        User user = new User();
        user.id = UserId.of(java.util.UUID.randomUUID());
        user.name = name;
        user.email = email;
        user.status = UserStatus.ACTIVE;
        user.createdAt = LocalDateTime.now();
        user.validate();
        return user;
    }

    public void deactivate() {
        if (this.status == UserStatus.DELETED) {
            throw new UserDomainException("Cannot deactivate a deleted user.");
        }
        this.status = UserStatus.INACTIVE;
    }

    public void delete() {
        this.status = UserStatus.DELETED;
    }

    private void validate() {
        if (name == null || name.isBlank()) {
            throw new UserDomainException("User name cannot be empty.");
        }
        if (email == null || !email.contains("@")) {
            throw new UserDomainException("User email is invalid.");
        }
    }

    public UserId getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public UserStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
