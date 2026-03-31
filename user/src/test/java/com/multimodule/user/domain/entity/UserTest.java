package com.multimodule.user.domain.entity;

import com.multimodule.user.domain.exception.UserDomainException;
import com.multimodule.user.domain.valueobject.UserStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserTest {

    @Test
    void shouldCreateUserWithNormalizedData() {
        User user = User.create("  johnny  ", "  JOHN@MAIL.COM  ", "  John Doe  ", " 0123 ");

        assertNotNull(user.getId());
        assertEquals("johnny", user.getUsername());
        assertEquals("john@mail.com", user.getEmail());
        assertEquals("John Doe", user.getFullName());
        assertEquals("0123", user.getPhoneNumber());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertTrue(!user.isDeleted());
    }

    @Test
    void shouldBlockActivateAndSoftDeleteUser() {
        User user = User.create("johnny", "john@mail.com", "John Doe", null);

        user.block();
        assertEquals(UserStatus.BLOCKED, user.getStatus());

        user.activate();
        assertEquals(UserStatus.ACTIVE, user.getStatus());

        user.softDelete();
        assertTrue(user.isDeleted());
        assertNotNull(user.getDeletedAt());
    }

    @Test
    void shouldRejectInvalidEmail() {
        assertThrows(UserDomainException.class, () -> User.create("johnny", "bad-email", "John Doe", null));
    }
}
