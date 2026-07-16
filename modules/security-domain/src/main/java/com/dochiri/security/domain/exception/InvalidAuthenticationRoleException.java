package com.dochiri.security.domain.exception;

public final class InvalidAuthenticationRoleException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final SecurityDomainErrorCode errorCode;

    private InvalidAuthenticationRoleException(SecurityDomainErrorCode errorCode) {
        super(errorCode.name());
        this.errorCode = errorCode;
    }

    public static InvalidAuthenticationRoleException required() {
        return new InvalidAuthenticationRoleException(SecurityDomainErrorCode.AUTHENTICATION_ROLE_REQUIRED);
    }

    public static InvalidAuthenticationRoleException blank() {
        return new InvalidAuthenticationRoleException(SecurityDomainErrorCode.AUTHENTICATION_ROLE_BLANK);
    }

    public SecurityDomainErrorCode code() {
        return errorCode;
    }
}
