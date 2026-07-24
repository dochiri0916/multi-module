package com.dochiri.security.adapter.in.web.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@ConditionalOnMissingBean(SecurityErrorResponsePort.class)
public class SecurityProblemDetailResponseAdapter implements SecurityErrorResponsePort {

    private final ObjectMapper objectMapper;

    @Override
    public void write(
            SecurityErrorCode errorCode,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        ProblemDetail body = switch (errorCode) {
            case AUTHENTICATION_REQUIRED -> problem(
                    HttpStatus.UNAUTHORIZED,
                    "/problems/authentication-required",
                    "인증 필요",
                    "인증이 필요합니다."
            );
            case ACCESS_DENIED -> problem(
                    HttpStatus.FORBIDDEN,
                    "/problems/access-denied",
                    "접근 거부",
                    "접근 권한이 없습니다."
            );
        };
        body.setInstance(URI.create(request.getRequestURI()));
        response.setStatus(body.getStatus());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), body);
    }

    private static ProblemDetail problem(
            final HttpStatus status,
            final String type,
            final String title,
            final String detail
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create(type));
        problem.setTitle(title);
        return problem;
    }
}
