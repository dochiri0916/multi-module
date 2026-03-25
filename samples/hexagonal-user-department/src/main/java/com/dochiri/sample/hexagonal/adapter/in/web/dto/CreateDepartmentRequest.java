package com.dochiri.sample.hexagonal.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDepartmentRequest(
        @NotBlank(message = "부서명은 필수입니다.")
        @Size(max = 120, message = "부서명은 120자를 넘길 수 없습니다.")
        String name,

        @Size(max = 500, message = "설명은 500자를 넘길 수 없습니다.")
        String description
) {
}
