package com.dochiri.sample.hexagonal.application.port.out;

import com.dochiri.sample.hexagonal.domain.user.User;

public interface UserCommandPort {

    User save(User user);
}
