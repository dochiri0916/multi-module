package com.dochiri.sample.hexagonal.application.port.in;

import com.dochiri.sample.hexagonal.domain.user.User;

public interface RegisterUserUseCase {

    User register(Command command);

    record Command(String name, String email, Long departmentId) {
    }
}
