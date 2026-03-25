package com.dochiri.sample.hexagonal.adapter.out.persistence.repository;

import com.dochiri.sample.hexagonal.adapter.out.persistence.entity.UserJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserJpaRepository extends JpaRepository<UserJpaEntity, Long> {

    boolean existsByEmail(String email);

    List<UserJpaEntity> findAllByDepartmentIdOrderByIdAsc(Long departmentId);
}
