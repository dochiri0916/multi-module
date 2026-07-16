package com.dochiri.security.application.port.in;

public record CleanupRefreshSessionsResult(
        int deletedCount,
        boolean moreMayRemain
) {
}
