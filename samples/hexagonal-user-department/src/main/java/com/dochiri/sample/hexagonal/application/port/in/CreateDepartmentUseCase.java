package com.dochiri.sample.hexagonal.application.port.in;

import com.dochiri.sample.hexagonal.domain.department.Department;

public interface CreateDepartmentUseCase {

    Department create(Command command);

    record Command(String name, String description) {
    }
}
