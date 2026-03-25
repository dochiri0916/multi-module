package com.dochiri.sample.hexagonal.application.port.out;

import com.dochiri.sample.hexagonal.domain.department.Department;

public interface DepartmentCommandPort {

    Department save(Department department);
}
