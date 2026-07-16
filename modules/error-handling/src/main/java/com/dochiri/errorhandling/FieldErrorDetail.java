package com.dochiri.errorhandling;

public record FieldErrorDetail(
        String field,
        String reason,
        String messageCode
) {
}
