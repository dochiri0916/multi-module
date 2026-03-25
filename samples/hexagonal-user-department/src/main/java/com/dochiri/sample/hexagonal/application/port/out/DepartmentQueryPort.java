package com.dochiri.sample.hexagonal.application.port.out;

import com.dochiri.sample.hexagonal.domain.department.Department;

import java.util.Optional;

public interface DepartmentQueryPort {

    Optional<Department> findById(Long departmentId);
}
