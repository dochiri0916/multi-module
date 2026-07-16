package com.dochiri.security.domain.exception;

public final class InvalidAuthenticationSubjectException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final SecurityDomainErrorCode errorCode;

    private InvalidAuthenticationSubjectException(SecurityDomainErrorCode errorCode) {
        super(errorCode.name());
        this.errorCode = errorCode;
    }

    public static InvalidAuthenticationSubjectException required() {
        return new InvalidAuthenticationSubjectException(SecurityDomainErrorCode.AUTHENTICATION_SUBJECT_REQUIRED);
    }

    public static InvalidAuthenticationSubjectException blank() {
        return new InvalidAuthenticationSubjectException(SecurityDomainErrorCode.AUTHENTICATION_SUBJECT_BLANK);
    }

    public SecurityDomainErrorCode code() {
        return errorCode;
    }
}
