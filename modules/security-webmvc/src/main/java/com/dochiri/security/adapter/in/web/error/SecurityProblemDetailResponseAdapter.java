package com.dochiri.security.adapter.in.web.error;

import com.dochiri.errorhandling.global.error.ApiErrorCode;
import com.dochiri.errorhandling.global.error.ApiErrorMapping;
import com.dochiri.errorhandling.global.error.ApiErrorMessage;
import com.dochiri.errorhandling.global.error.ApiErrorMessageCatalog;
import com.dochiri.errorhandling.global.error.ApiExceptionMapper;
import com.dochiri.errorhandling.global.error.ApiProblemDetailFactory;
import com.dochiri.errorhandling.global.error.MappedApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.ServletWebRequest;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@ConditionalOnMissingBean(SecurityErrorResponsePort.class)
public class SecurityProblemDetailResponseAdapter implements SecurityErrorResponsePort {

    private final ObjectMapper objectMapper;
    private final ApiProblemDetailFactory problemDetailFactory;
    private final ApiExceptionMapper exceptionMapper;
    private final ApiErrorMessageCatalog messageCatalog;

    @Override
    public void write(
            SecurityErrorCode errorCode,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        ApiErrorCode code = ApiErrorCode.from(errorCode);
        ApiErrorMapping mapping = exceptionMapper.mappingFor(code);
        ApiErrorMessage message = messageCatalog.messageFor(code);
        ProblemDetail body = problemDetailFactory.create(
                MappedApiError.from(code, mapping),
                message,
                new ServletWebRequest(request)
        );
        response.setStatus(mapping.status().value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), body);
    }
}
