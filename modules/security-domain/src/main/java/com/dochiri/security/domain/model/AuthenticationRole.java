package com.dochiri.security.domain.model;

import com.dochiri.security.domain.exception.InvalidAuthenticationRoleException;

public record AuthenticationRole(String value) {

    public AuthenticationRole {
        if (value == null) {
            throw InvalidAuthenticationRoleException.required();
        }
        value = value.strip();
        if (value.startsWith("ROLE_")) {
            value = value.substring("ROLE_".length()).strip();
        }
        if (value.isBlank()) {
            throw InvalidAuthenticationRoleException.blank();
        }
    }

    public String grantedAuthority() {
        return "ROLE_" + value;
    }
}
