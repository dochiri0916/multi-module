package com.dochiri.sample.hexagonal.adapter.out.persistence.repository;

import com.dochiri.sample.hexagonal.adapter.out.persistence.entity.DepartmentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentJpaRepository extends JpaRepository<DepartmentJpaEntity, Long> {
}
