package com.dochiri.errorhandling;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import({
        GlobalExceptionHandlerWebMvcTest.TestGlobalExceptionHandler.class,
        GlobalExceptionHandlerWebMvcTest.UserController.class
})
class GlobalExceptionHandlerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void request_body_validation_실패_시_fieldErrors를_반환한다() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"abc","name":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("/errors/validation-error"))
                .andExpect(jsonPath("$.title").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("요청 값이 올바르지 않습니다."))
                .andExpect(jsonPath("$.instance").value("/users"))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'email' && @.messageCode == 'Email')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'name' && @.messageCode == 'NotBlank')]").exists());
    }

    @Test
    void request_param_validation_실패_시_fieldErrors를_반환한다() throws Exception {
        mockMvc.perform(get("/users/search").param("age", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("age"))
                .andExpect(jsonPath("$.fieldErrors[0].rejectedValue").value(0))
                .andExpect(jsonPath("$.fieldErrors[0].messageCode").value("Min"));
    }

    @SpringBootApplication
    static class TestApplication {
    }

    @RestControllerAdvice
    static class TestGlobalExceptionHandler extends GlobalExceptionHandler {
    }

    @RestController
    static class UserController {

        @PostMapping("/users")
        String createUser(@Valid @RequestBody CreateUserRequest request) {
            return "created";
        }

        @GetMapping("/users/search")
        String searchUsers(@RequestParam("age") @Min(1) int age) {
            return "ok";
        }
    }

    record CreateUserRequest(
            @Email String email,
            @NotBlank String name
    ) {
    }

}
