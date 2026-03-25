package com.dochiri.sample.hexagonal.adapter.in.web;

import com.dochiri.sample.hexagonal.adapter.in.web.dto.CreateDepartmentRequest;
import com.dochiri.sample.hexagonal.adapter.in.web.dto.DepartmentResponse;
import com.dochiri.sample.hexagonal.adapter.in.web.dto.UserResponse;
import com.dochiri.sample.hexagonal.application.port.in.CreateDepartmentUseCase;
import com.dochiri.sample.hexagonal.application.port.in.GetDepartmentUseCase;
import com.dochiri.sample.hexagonal.application.port.in.ListDepartmentUsersUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/departments")
public class DepartmentController {

    private final CreateDepartmentUseCase createDepartmentUseCase;
    private final GetDepartmentUseCase getDepartmentUseCase;
    private final ListDepartmentUsersUseCase listDepartmentUsersUseCase;

    public DepartmentController(
            CreateDepartmentUseCase createDepartmentUseCase,
            GetDepartmentUseCase getDepartmentUseCase,
            ListDepartmentUsersUseCase listDepartmentUsersUseCase
    ) {
        this.createDepartmentUseCase = createDepartmentUseCase;
        this.getDepartmentUseCase = getDepartmentUseCase;
        this.listDepartmentUsersUseCase = listDepartmentUsersUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DepartmentResponse create(@Valid @RequestBody CreateDepartmentRequest request) {
        return DepartmentResponse.from(
                createDepartmentUseCase.create(
                        new CreateDepartmentUseCase.Command(request.name(), request.description())
                )
        );
    }

    @GetMapping("/{departmentId}")
    public DepartmentResponse get(@PathVariable("departmentId") Long departmentId) {
        return DepartmentResponse.from(getDepartmentUseCase.get(departmentId));
    }

    @GetMapping("/{departmentId}/users")
    public List<UserResponse> listUsers(@PathVariable("departmentId") Long departmentId) {
        return listDepartmentUsersUseCase.listByDepartmentId(departmentId).stream()
                .map(UserResponse::from)
                .toList();
    }
}
