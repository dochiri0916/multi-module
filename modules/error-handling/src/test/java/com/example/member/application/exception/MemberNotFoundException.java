package com.example.member.application.exception;

import static java.util.Objects.requireNonNull;

public final class MemberNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final MemberApplicationErrorCode errorCode;
    private final String missingMemberIdentifier;

    private MemberNotFoundException(
            MemberApplicationErrorCode errorCode,
            String missingMemberIdentifier
    ) {
        super(requireNonNull(errorCode).name());
        this.errorCode = requireNonNull(errorCode);
        this.missingMemberIdentifier = requireNonNull(missingMemberIdentifier);
    }

    public static MemberNotFoundException memberNotFound(String memberIdentifier) {
        return new MemberNotFoundException(MemberApplicationErrorCode.MEMBER_NOT_FOUND, memberIdentifier);
    }

    public MemberApplicationErrorCode code() {
        return errorCode;
    }

    public String memberIdentifier() {
        return missingMemberIdentifier;
    }
}
