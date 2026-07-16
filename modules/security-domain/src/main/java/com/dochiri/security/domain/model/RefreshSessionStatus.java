package com.dochiri.security.domain.model;

public enum RefreshSessionStatus {

    ACTIVE,
    REVOKED;

    public boolean isRevoked() {
        return this == REVOKED;
    }
}
