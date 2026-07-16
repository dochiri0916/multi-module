package com.dochiri.errorhandling;

public record ApiErrorMessage(String title, String detail) {

    public ApiErrorMessage {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title은 필수입니다.");
        }
        if (detail == null || detail.isBlank()) {
            throw new IllegalArgumentException("detail은 필수입니다.");
        }
    }
}
