package com.dochiri.sample.hexagonal.adapter.in.web.dto;

import com.dochiri.sample.hexagonal.domain.department.Department;

public record DepartmentResponse(
        Long id,
        String name,
        String description
) {

    public static DepartmentResponse from(Department department) {
        return new DepartmentResponse(department.id(), department.name(), department.description());
    }
}
