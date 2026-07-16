package com.dochiri.security.domain.model;

import com.dochiri.security.domain.exception.InvalidAuthenticationRoleException;

public record AuthenticationRole(String value) {

    private static final String ROLE_PREFIX = "ROLE_";

    public AuthenticationRole {
        if (value == null) {
            throw InvalidAuthenticationRoleException.required();
        }
        value = value.strip();
        if (value.startsWith(ROLE_PREFIX)) {
            value = value.substring(ROLE_PREFIX.length()).strip();
        }
        if (value.isBlank()) {
            throw InvalidAuthenticationRoleException.blank();
        }
    }

    public String grantedAuthority() {
        return ROLE_PREFIX + value;
    }
}
