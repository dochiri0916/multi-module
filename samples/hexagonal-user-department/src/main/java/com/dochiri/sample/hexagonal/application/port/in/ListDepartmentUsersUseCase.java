package com.dochiri.sample.hexagonal.application.port.in;

import com.dochiri.sample.hexagonal.domain.user.User;

import java.util.List;

public interface ListDepartmentUsersUseCase {

    List<User> listByDepartmentId(Long departmentId);
}
