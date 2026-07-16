package com.dochiri.security.adapter.in.web.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@ConditionalOnMissingBean(AccessDeniedHandler.class)
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final SecurityErrorResponsePort errorResponsePort;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException exception
    ) throws IOException {
        errorResponsePort.write(SecurityErrorCode.ACCESS_DENIED, request, response);
    }
}
