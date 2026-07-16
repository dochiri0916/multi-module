package com.dochiri.errorhandling.global.error;

import com.example.member.adapter.in.web.MemberErrorCodeMappingProvider;
import com.example.member.adapter.in.web.MemberErrorMessageProvider;
import com.example.member.application.exception.MemberApplicationErrorCode;
import com.example.member.application.exception.MemberNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
@Import({
        GlobalExceptionHandlerWebMvcTest.UserController.class,
        MemberErrorCodeMappingProvider.class,
        MemberErrorMessageProvider.class
})
class GlobalExceptionHandlerWebMvcTest {

    private static final String REFRESH_TOKEN_PATTERN_FIELD_ERROR_PATH =
            "$.fieldErrors[?(@.field == 'refreshToken' && @.messageCode == 'Pattern')]";

    private static final String USERS_PATH = "/users";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApiErrorContractValidator contractValidator;

    @Test
    @DisplayName("요청 본문 검증에 실패하면 필드 오류를 반환한다")
    void returnsFieldErrorsForInvalidRequestBody() throws Exception {
        // given
        String requestBody = """
                {"email":"abc","name":""}
                """;

        // when
        MvcResult result = mockMvc.perform(post(USERS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("/problems/validation-failed"))
                .andExpect(jsonPath("$.title").value("요청 검증 실패"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("요청 값이 올바르지 않습니다."))
                .andExpect(jsonPath("$.instance").value(USERS_PATH))
                .andExpect(jsonPath("$.code").value("GLOBAL.VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'email' && @.messageCode == 'Email')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'name' && @.messageCode == 'NotBlank')]").exists())
                .andReturn();

        // then
        assertThat(result.getResponse().getContentAsString()).doesNotContain("rejectedValue");
    }

    @Test
    @DisplayName("요청 파라미터 검증에 실패해도 거부된 원본 값을 반환하지 않는다")
    void returnsFieldErrorsForInvalidRequestParameter() throws Exception {
        // given
        String invalidAge = "0";

        // when
        MvcResult result = mockMvc.perform(get("/users/search").param("age", invalidAge))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("요청 검증 실패"))
                .andExpect(jsonPath("$.code").value("GLOBAL.VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("age"))
                .andExpect(jsonPath("$.fieldErrors[0].rejectedValue").doesNotExist())
                .andExpect(jsonPath("$.fieldErrors[0].messageCode").value("Min"))
                .andReturn();

        // then
        assertThat(result.getResponse().getContentAsString()).doesNotContain("rejectedValue");
    }

    @Test
    @DisplayName("비밀번호 검증 실패 응답과 로그에 비밀번호 원문을 노출하지 않는다")
    void doesNotExposePasswordInValidationResponseOrLog(CapturedOutput output) throws Exception {
        // given
        String password = "plain-secret-password";
        String requestBody = "{\"email\":\"user@example.com\",\"name\":\"사용자\",\"password\":\""
                + password
                + "\"}";

        // when
        MvcResult result = mockMvc.perform(post(USERS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'password' && @.messageCode == 'Size')]").exists())
                .andExpect(jsonPath("$..rejectedValue").doesNotExist())
                .andReturn();

        // then
        assertThat(result.getResponse().getContentAsString()).doesNotContain(password);
        assertThat(output.getAll()).doesNotContain(password);
    }

    @Test
    @DisplayName("토큰 검증 실패 응답과 로그에 토큰 원문을 노출하지 않는다")
    void doesNotExposeTokenInValidationResponseOrLog(CapturedOutput output) throws Exception {
        // given
        String refreshToken = "raw-refresh-token";

        // when
        MvcResult result = mockMvc.perform(get("/tokens/verify").param("refreshToken", refreshToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(REFRESH_TOKEN_PATTERN_FIELD_ERROR_PATH).exists())
                .andExpect(jsonPath("$..rejectedValue").doesNotExist())
                .andReturn();

        // then
        assertThat(result.getResponse().getContentAsString()).doesNotContain(refreshToken);
        assertThat(output.getAll()).doesNotContain(refreshToken);
    }

    @Test
    @DisplayName("Web Adapter가 plain Application 예외를 HTTP 오류 응답으로 변환한다")
    void mapsPlainApplicationExceptionToHttpError() throws Exception {
        // given
        String missingMemberId = "missing-member-id";

        // when
        MvcResult result = mockMvc.perform(get("/members/{memberId}", missingMemberId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("/problems/not-found"))
                .andExpect(jsonPath("$.title").value("회원 조회 실패"))
                .andExpect(jsonPath("$.detail").value("회원을 찾을 수 없습니다."))
                .andExpect(jsonPath("$.code").value("MEMBER.NOT_FOUND"))
                .andExpect(jsonPath("$.memberId").value(missingMemberId))
                .andReturn();

        // then
        assertThat(result.getResolvedException()).isInstanceOfSatisfying(
                MemberNotFoundException.class,
                exception -> {
                    assertThat(exception.code()).isEqualTo(MemberApplicationErrorCode.MEMBER_NOT_FOUND);
                    assertThat(exception.memberIdentifier()).isEqualTo(missingMemberId);
                }
        );
        assertThat(MemberNotFoundException.class.getSuperclass()).isEqualTo(RuntimeException.class);
    }

    @Test
    @DisplayName("자동 설정은 전역 및 Context provider 계약을 시작 시점에 검증한다")
    void validatesGlobalAndContextProvidersAtStartup() {
        // given
        ApiErrorCode globalCode = ApiErrorCode.from(GlobalErrorCode.VALIDATION_ERROR);
        ApiErrorCode memberCode = ApiErrorCode.from(MemberApplicationErrorCode.MEMBER_NOT_FOUND);

        // when
        var validatedCodes = contractValidator.validate();

        // then
        assertThat(validatedCodes).contains(globalCode, memberCode);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class WebMvcTestApplication {
    }

    @RestController
    static class UserController {

        @PostMapping(USERS_PATH)
        String createUser(@Valid @RequestBody CreateUserRequest request) {
            return "created";
        }

        @GetMapping("/users/search")
        String searchUsers(@RequestParam("age") @Min(1) int age) {
            return "ok";
        }

        @GetMapping("/tokens/verify")
        String verifyRefreshToken(
                @RequestParam("refreshToken")
                @Pattern(regexp = "^[A-F0-9]{32}$") String refreshToken
        ) {
            return "ok";
        }

        @GetMapping("/members/{memberId}")
        String getMember(@PathVariable("memberId") String memberId) {
            throw MemberNotFoundException.memberNotFound(memberId);
        }
    }

    record CreateUserRequest(
            @Email String email,
            @NotBlank String name,
            @Size(min = 32) String password
    ) {
    }

}
