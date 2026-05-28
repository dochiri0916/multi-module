package com.dochiri.errorhandling;

public record FieldErrorDetail(
        String field,
        Object rejectedValue,
        String reason,
        String messageCode
) {
}
