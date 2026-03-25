package com.dochiri.sample.hexagonal.application.service;

import com.dochiri.errorhandling.BaseException;
import com.dochiri.sample.hexagonal.application.port.in.CreateDepartmentUseCase;
import com.dochiri.sample.hexagonal.application.port.in.GetDepartmentUseCase;
import com.dochiri.sample.hexagonal.application.port.out.DepartmentCommandPort;
import com.dochiri.sample.hexagonal.application.port.out.DepartmentQueryPort;
import com.dochiri.sample.hexagonal.domain.department.Department;
import com.dochiri.sample.hexagonal.support.error.SampleErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DepartmentService implements CreateDepartmentUseCase, GetDepartmentUseCase {

    private final DepartmentCommandPort departmentCommandPort;
    private final DepartmentQueryPort departmentQueryPort;

    public DepartmentService(
            DepartmentCommandPort departmentCommandPort,
            DepartmentQueryPort departmentQueryPort
    ) {
        this.departmentCommandPort = departmentCommandPort;
        this.departmentQueryPort = departmentQueryPort;
    }

    @Override
    @Transactional
    public Department create(Command command) {
        Department department = Department.create(command.name(), command.description());
        return departmentCommandPort.save(department);
    }

    @Override
    public Department get(Long departmentId) {
        return departmentQueryPort.findById(departmentId)
                .orElseThrow(() -> BaseException.of(
                        SampleErrorCode.DEPARTMENT_NOT_FOUND,
                        "departmentId", departmentId
                ));
    }
}
