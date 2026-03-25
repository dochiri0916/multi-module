package com.dochiri.sample.hexagonal.application.port.in;

import com.dochiri.sample.hexagonal.domain.user.User;

public interface GetUserUseCase {

    User get(Long userId);
}
