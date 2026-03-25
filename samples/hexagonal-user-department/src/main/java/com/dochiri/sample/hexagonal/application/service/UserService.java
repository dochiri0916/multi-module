package com.dochiri.sample.hexagonal.application.service;

import com.dochiri.errorhandling.BaseException;
import com.dochiri.sample.hexagonal.application.port.in.GetUserUseCase;
import com.dochiri.sample.hexagonal.application.port.in.ListDepartmentUsersUseCase;
import com.dochiri.sample.hexagonal.application.port.in.RegisterUserUseCase;
import com.dochiri.sample.hexagonal.application.port.out.DepartmentQueryPort;
import com.dochiri.sample.hexagonal.application.port.out.UserCommandPort;
import com.dochiri.sample.hexagonal.application.port.out.UserQueryPort;
import com.dochiri.sample.hexagonal.domain.user.User;
import com.dochiri.sample.hexagonal.support.error.SampleErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class UserService implements RegisterUserUseCase, GetUserUseCase, ListDepartmentUsersUseCase {

    private final UserCommandPort userCommandPort;
    private final UserQueryPort userQueryPort;
    private final DepartmentQueryPort departmentQueryPort;

    public UserService(
            UserCommandPort userCommandPort,
            UserQueryPort userQueryPort,
            DepartmentQueryPort departmentQueryPort
    ) {
        this.userCommandPort = userCommandPort;
        this.userQueryPort = userQueryPort;
        this.departmentQueryPort = departmentQueryPort;
    }

    @Override
    @Transactional
    public User register(Command command) {
        Long departmentId = command.departmentId();
        String email = command.email().trim().toLowerCase();

        departmentQueryPort.findById(departmentId)
                .orElseThrow(() -> BaseException.of(
                        SampleErrorCode.DEPARTMENT_NOT_FOUND,
                        "departmentId", departmentId
                ));

        if (userQueryPort.existsByEmail(email)) {
            throw BaseException.of(SampleErrorCode.DUPLICATE_USER_EMAIL, "email", email);
        }

        User user = User.register(command.name(), email, departmentId);
        return userCommandPort.save(user);
    }

    @Override
    public User get(Long userId) {
        return userQueryPort.findById(userId)
                .orElseThrow(() -> BaseException.of(SampleErrorCode.USER_NOT_FOUND, "userId", userId));
    }

    @Override
    public List<User> listByDepartmentId(Long departmentId) {
        departmentQueryPort.findById(departmentId)
                .orElseThrow(() -> BaseException.of(
                        SampleErrorCode.DEPARTMENT_NOT_FOUND,
                        "departmentId", departmentId
                ));

        return userQueryPort.findAllByDepartmentId(departmentId);
    }
}
