package com.dochiri.commerceapi.infrastructure.user;

import com.dochiri.commerceapi.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserJpaRepository extends JpaRepository<User, Long> {

}