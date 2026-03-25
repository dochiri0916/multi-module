package com.dochiri.sample.hexagonal.application.port.out;

import com.dochiri.sample.hexagonal.domain.user.User;

import java.util.List;
import java.util.Optional;

public interface UserQueryPort {

    Optional<User> findById(Long userId);

    boolean existsByEmail(String email);

    List<User> findAllByDepartmentId(Long departmentId);
}
