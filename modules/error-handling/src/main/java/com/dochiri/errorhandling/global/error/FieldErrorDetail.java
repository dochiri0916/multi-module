package com.dochiri.errorhandling.global.error;

public record FieldErrorDetail(
        String field,
        String reason,
        String messageCode
) {
}
