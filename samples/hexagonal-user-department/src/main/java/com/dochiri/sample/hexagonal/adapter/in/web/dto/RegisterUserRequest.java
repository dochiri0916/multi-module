package com.dochiri.sample.hexagonal.adapter.in.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record RegisterUserRequest(
        @NotBlank(message = "사용자 이름은 필수입니다.")
        @Size(max = 100, message = "사용자 이름은 100자를 넘길 수 없습니다.")
        String name,

        @NotBlank(message = "사용자 이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이어야 합니다.")
        @Size(max = 150, message = "사용자 이메일은 150자를 넘길 수 없습니다.")
        String email,

        @NotNull(message = "departmentId는 필수입니다.")
        @Positive(message = "departmentId는 양수여야 합니다.")
        Long departmentId
) {
}
