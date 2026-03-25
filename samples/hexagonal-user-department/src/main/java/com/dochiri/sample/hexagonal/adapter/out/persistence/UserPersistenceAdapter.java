package com.dochiri.sample.hexagonal.adapter.out.persistence;

import com.dochiri.sample.hexagonal.adapter.out.persistence.entity.UserJpaEntity;
import com.dochiri.sample.hexagonal.adapter.out.persistence.repository.UserJpaRepository;
import com.dochiri.sample.hexagonal.application.port.out.UserCommandPort;
import com.dochiri.sample.hexagonal.application.port.out.UserQueryPort;
import com.dochiri.sample.hexagonal.domain.user.User;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class UserPersistenceAdapter implements UserCommandPort, UserQueryPort {

    private final UserJpaRepository userJpaRepository;

    public UserPersistenceAdapter(UserJpaRepository userJpaRepository) {
        this.userJpaRepository = userJpaRepository;
    }

    @Override
    public User save(User user) {
        UserJpaEntity userJpaEntity = UserJpaEntity.from(user);
        return userJpaRepository.save(userJpaEntity).toDomain();
    }

    @Override
    public Optional<User> findById(Long userId) {
        return userJpaRepository.findById(userId)
                .map(UserJpaEntity::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userJpaRepository.existsByEmail(email);
    }

    @Override
    public List<User> findAllByDepartmentId(Long departmentId) {
        return userJpaRepository.findAllByDepartmentIdOrderByIdAsc(departmentId).stream()
                .map(UserJpaEntity::toDomain)
                .toList();
    }
}
