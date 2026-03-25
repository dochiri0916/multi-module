package com.dochiri.sample.hexagonal.adapter.in.web.dto;

import com.dochiri.sample.hexagonal.domain.user.User;

public record UserResponse(
        Long id,
        String name,
        String email,
        Long departmentId
) {

    public static UserResponse from(User user) {
        return new UserResponse(user.id(), user.name(), user.email(), user.departmentId());
    }
}
