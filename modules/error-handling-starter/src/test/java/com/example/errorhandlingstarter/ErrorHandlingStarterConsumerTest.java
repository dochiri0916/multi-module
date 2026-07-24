package com.example.errorhandlingstarter;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import({
        ErrorHandlingStarterConsumerTest.ProbeController.class,
        ErrorHandlingStarterConsumerTest.ProbeExceptionHandler.class
})
class ErrorHandlingStarterConsumerTest {

    private static final String INTERNAL_MESSAGE = "internal-secret-message";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("오류 처리 starter 하나로 검증 오류를 표준 ProblemDetail로 반환한다")
    void configuresStandardValidationProblemDetail() throws Exception {
        // given
        String requestBody = "{\"name\":\"\"}";

        // when
        MvcResult result = mockMvc.perform(post("/probe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.instance").value("/probe"))
                .andExpect(jsonPath("$.code").doesNotExist())
                .andExpect(jsonPath("$.traceId").doesNotExist())
                .andExpect(jsonPath("$.fieldErrors").doesNotExist())
                .andReturn();

        // then
        assertThat(ClassUtils.isPresent(
                "org.hibernate.validator.HibernateValidator",
                Thread.currentThread().getContextClassLoader()
        )).isTrue();
        assertThat(result.getResponse().getContentAsString()).doesNotContain(INTERNAL_MESSAGE);
    }

    @Test
    @DisplayName("미처리 예외는 내부 정보를 숨긴 표준 ProblemDetail로 반환한다")
    void hidesInternalDetailsForUnhandledException() throws Exception {
        // given
        String expectedDetail = "일시적인 오류가 발생했습니다.";

        // when
        MvcResult result = mockMvc.perform(get("/failure"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").doesNotExist())
                .andExpect(jsonPath("$.title").value("서버 오류"))
                .andExpect(jsonPath("$.status").value(HttpStatus.INTERNAL_SERVER_ERROR.value()))
                .andExpect(jsonPath("$.detail").value(expectedDetail))
                .andExpect(jsonPath("$.instance").value("/failure"))
                .andExpect(jsonPath("$.code").doesNotExist())
                .andExpect(jsonPath("$.traceId").doesNotExist())
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString(INTERNAL_MESSAGE)
                )))
                .andReturn();

        // then
        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(result.getResponse().getContentAsString())
                .contains(expectedDetail)
                .doesNotContain(INTERNAL_MESSAGE);
    }

    @Test
    @DisplayName("소비 Context의 구체적인 예외 처리가 공통 fallback보다 우선한다")
    void prefersConsumerContextExceptionHandler() throws Exception {
        // given
        String expectedType = "/problems/duplicate-resource";

        // when
        MvcResult result = mockMvc.perform(get("/business-failure"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(expectedType))
                .andExpect(jsonPath("$.title").value("리소스 중복"))
                .andExpect(jsonPath("$.detail").value("이미 존재하는 리소스입니다."))
                .andExpect(jsonPath("$.instance").value("/business-failure"))
                .andReturn();

        // then
        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(result.getResponse().getContentAsString()).contains(expectedType);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class ConsumerApplication {
    }

    @RestController
    static class ProbeController {

        @PostMapping("/probe")
        String probe(@Valid @RequestBody ProbeRequest request) {
            return "ok";
        }

        @GetMapping("/failure")
        String failure() {
            throw new IllegalStateException(INTERNAL_MESSAGE);
        }

        @GetMapping("/business-failure")
        String businessFailure() {
            throw new DuplicateResourceException();
        }
    }

    @RestControllerAdvice
    @Order(Ordered.HIGHEST_PRECEDENCE)
    static class ProbeExceptionHandler {

        @ExceptionHandler(DuplicateResourceException.class)
        ProblemDetail handleDuplicateResource() {
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                    HttpStatus.CONFLICT,
                    "이미 존재하는 리소스입니다."
            );
            problem.setType(URI.create("/problems/duplicate-resource"));
            problem.setTitle("리소스 중복");
            return problem;
        }
    }

    static final class DuplicateResourceException extends RuntimeException {
    }

    record ProbeRequest(
            @NotBlank(message = INTERNAL_MESSAGE) String name
    ) {
    }
}
