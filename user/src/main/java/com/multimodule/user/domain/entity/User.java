package com.multimodule.user.domain.entity;

import com.multimodule.user.domain.exception.UserDomainException;
import com.multimodule.user.domain.valueobject.UserId;
import com.multimodule.user.domain.valueobject.UserStatus;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class User implements Serializable {

    private final UserId id;
    private String username;
    private String email;
    private String fullName;
    private String phoneNumber;
    private UserStatus status;
    private boolean deleted;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    private User(
            UserId id,
            String username,
            String email,
            String fullName,
            String phoneNumber,
            UserStatus status,
            boolean deleted,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime deletedAt
    ) {
        this.id = Objects.requireNonNull(id, "User id is required");
        this.username = normalize(username);
        this.email = normalizeEmail(email);
        this.fullName = normalize(fullName);
        this.phoneNumber = normalizeNullable(phoneNumber);
        this.status = Objects.requireNonNull(status, "User status is required");
        this.deleted = deleted;
        this.createdAt = Objects.requireNonNull(createdAt, "Created time is required");
        this.updatedAt = Objects.requireNonNull(updatedAt, "Updated time is required");
        this.deletedAt = deletedAt;
        validateState();
    }

    public static User create(String username, String email, String fullName, String phoneNumber) {
        LocalDateTime now = LocalDateTime.now();
        return new User(
                UserId.of(UUID.randomUUID()),
                username,
                email,
                fullName,
                phoneNumber,
                UserStatus.ACTIVE,
                false,
                now,
                now,
                null
        );
    }

    public static User restore(
            UserId id,
            String username,
            String email,
            String fullName,
            String phoneNumber,
            UserStatus status,
            boolean deleted,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime deletedAt
    ) {
        return new User(id, username, email, fullName, phoneNumber, status, deleted, createdAt, updatedAt, deletedAt);
    }

    public void updateProfile(String username, String email, String fullName, String phoneNumber) {
        ensureNotDeleted("Cannot update a deleted user.");
        this.username = normalize(username);
        this.email = normalizeEmail(email);
        this.fullName = normalize(fullName);
        this.phoneNumber = normalizeNullable(phoneNumber);
        touch();
        validateState();
    }

    public void block() {
        ensureNotDeleted("Cannot block a deleted user.");
        this.status = UserStatus.BLOCKED;
        touch();
    }

    public void activate() {
        ensureNotDeleted("Cannot activate a deleted user.");
        this.status = UserStatus.ACTIVE;
        touch();
    }

    public void softDelete() {
        ensureNotDeleted("User is already deleted.");
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
        touch();
    }

    public UserId getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public UserStatus getStatus() {
        return status;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    private void ensureNotDeleted(String message) {
        if (deleted) {
            throw new UserDomainException(message);
        }
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    private void validateState() {
        if (username == null || username.isBlank()) {
            throw new UserDomainException("Username cannot be empty.");
        }
        if (fullName == null || fullName.isBlank()) {
            throw new UserDomainException("Full name cannot be empty.");
        }
        if (email == null || !email.contains("@")) {
            throw new UserDomainException("User email is invalid.");
        }
        if (deleted && deletedAt == null) {
            throw new UserDomainException("Deleted user must have deletedAt.");
        }
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private static String normalizeNullable(String value) {
        String normalized = normalize(value);
        return normalized == null || normalized.isBlank() ? null : normalized;
    }

    private static String normalizeEmail(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : normalized.toLowerCase();
    }
}
