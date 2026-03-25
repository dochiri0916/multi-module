package com.dochiri.sample.hexagonal.domain.user;

import java.util.Locale;

public record User(
        Long id,
        String name,
        String email,
        Long departmentId
) {

    public User {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("user name must not be blank");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("user email must not be blank");
        }
        if (departmentId == null || departmentId < 1L) {
            throw new IllegalArgumentException("departmentId must be positive");
        }

        name = name.trim();
        email = email.trim().toLowerCase(Locale.ROOT);
    }

    public static User register(String name, String email, Long departmentId) {
        return new User(null, name, email, departmentId);
    }
}
