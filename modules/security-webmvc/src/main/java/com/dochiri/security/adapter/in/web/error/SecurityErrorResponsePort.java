package com.dochiri.security.adapter.in.web.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@FunctionalInterface
public interface SecurityErrorResponsePort {

    void write(
            SecurityErrorCode errorCode,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException;
}
