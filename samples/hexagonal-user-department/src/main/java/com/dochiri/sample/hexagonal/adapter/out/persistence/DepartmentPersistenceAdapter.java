package com.dochiri.sample.hexagonal.adapter.out.persistence;

import com.dochiri.sample.hexagonal.adapter.out.persistence.entity.DepartmentJpaEntity;
import com.dochiri.sample.hexagonal.adapter.out.persistence.repository.DepartmentJpaRepository;
import com.dochiri.sample.hexagonal.application.port.out.DepartmentCommandPort;
import com.dochiri.sample.hexagonal.application.port.out.DepartmentQueryPort;
import com.dochiri.sample.hexagonal.domain.department.Department;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class DepartmentPersistenceAdapter implements DepartmentCommandPort, DepartmentQueryPort {

    private final DepartmentJpaRepository departmentJpaRepository;

    public DepartmentPersistenceAdapter(DepartmentJpaRepository departmentJpaRepository) {
        this.departmentJpaRepository = departmentJpaRepository;
    }

    @Override
    public Department save(Department department) {
        DepartmentJpaEntity departmentJpaEntity = DepartmentJpaEntity.from(department);
        return departmentJpaRepository.save(departmentJpaEntity).toDomain();
    }

    @Override
    public Optional<Department> findById(Long departmentId) {
        return departmentJpaRepository.findById(departmentId)
                .map(DepartmentJpaEntity::toDomain);
    }
}
