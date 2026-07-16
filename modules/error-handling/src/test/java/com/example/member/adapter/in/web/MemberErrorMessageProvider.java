package com.example.member.adapter.in.web;

import com.dochiri.errorhandling.ApiErrorCode;
import com.dochiri.errorhandling.ApiErrorMessage;
import com.dochiri.errorhandling.ApiErrorMessageProvider;
import com.example.member.application.exception.MemberApplicationErrorCode;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public final class MemberErrorMessageProvider implements ApiErrorMessageProvider {

    private static final Map<ApiErrorCode, ApiErrorMessage> ERROR_MESSAGES = Map.of(
            ApiErrorCode.from(MemberApplicationErrorCode.MEMBER_NOT_FOUND),
            new ApiErrorMessage("회원 조회 실패", "회원을 찾을 수 없습니다.")
    );

    @Override
    public Map<ApiErrorCode, ApiErrorMessage> errorMessages() {
        return Map.copyOf(ERROR_MESSAGES);
    }
}
