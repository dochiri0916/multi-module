package com.dochiri.sample.hexagonal.application.port.in;

import com.dochiri.sample.hexagonal.domain.department.Department;

public interface GetDepartmentUseCase {

    Department get(Long departmentId);
}
