package com.dochiri.sample.hexagonal.adapter.in.web;

import com.dochiri.sample.hexagonal.adapter.in.web.dto.RegisterUserRequest;
import com.dochiri.sample.hexagonal.adapter.in.web.dto.UserResponse;
import com.dochiri.sample.hexagonal.application.port.in.GetUserUseCase;
import com.dochiri.sample.hexagonal.application.port.in.RegisterUserUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/users")
public class UserController {

    private final RegisterUserUseCase registerUserUseCase;
    private final GetUserUseCase getUserUseCase;

    public UserController(
            RegisterUserUseCase registerUserUseCase,
            GetUserUseCase getUserUseCase
    ) {
        this.registerUserUseCase = registerUserUseCase;
        this.getUserUseCase = getUserUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterUserRequest request) {
        return UserResponse.from(
                registerUserUseCase.register(
                        new RegisterUserUseCase.Command(request.name(), request.email(), request.departmentId())
                )
        );
    }

    @GetMapping("/{userId}")
    public UserResponse get(@PathVariable("userId") Long userId) {
        return UserResponse.from(getUserUseCase.get(userId));
    }
}
