package com.example.member.adapter.in.web;

import com.dochiri.errorhandling.global.error.ApiErrorCode;
import com.dochiri.errorhandling.global.error.ApiErrorMapping;
import com.dochiri.errorhandling.global.error.ErrorCodeMappingProvider;
import com.dochiri.errorhandling.global.error.MappedApiError;
import com.dochiri.errorhandling.global.error.ProblemType;
import com.example.member.application.exception.MemberApplicationErrorCode;
import com.example.member.application.exception.MemberNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public final class MemberErrorCodeMappingProvider implements ErrorCodeMappingProvider {

    private static final Map<ApiErrorCode, ApiErrorMapping> ERROR_CODE_MAPPINGS = Map.of(
            ApiErrorCode.from(MemberApplicationErrorCode.MEMBER_NOT_FOUND),
            new ApiErrorMapping(HttpStatus.NOT_FOUND, ProblemType.NOT_FOUND)
    );

    @Override
    public Map<ApiErrorCode, ApiErrorMapping> errorCodeMappings() {
        return Map.copyOf(ERROR_CODE_MAPPINGS);
    }

    @Override
    public Optional<MappedApiError> resolve(RuntimeException exception) {
        if (exception instanceof MemberNotFoundException memberNotFoundException) {
            return resolve(memberNotFoundException.code())
                    .map(mappedError -> mappedError.withProperties(Map.of(
                            "memberId",
                            memberNotFoundException.memberIdentifier()
                    )));
        }
        return Optional.empty();
    }
}
