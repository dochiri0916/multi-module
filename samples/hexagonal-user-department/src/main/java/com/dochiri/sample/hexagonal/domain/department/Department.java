package com.dochiri.sample.hexagonal.domain.department;

public record Department(
        Long id,
        String name,
        String description
) {

    public Department {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("department name must not be blank");
        }

        name = name.trim();
        description = description == null ? "" : description.trim();
    }

    public static Department create(String name, String description) {
        return new Department(null, name, description);
    }
}
