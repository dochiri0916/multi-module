package com.dochiri.commerceapi.presentation.coupon.request;

import com.dochiri.commerceapi.application.coupon.command.RegisterCouponService;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

public record RegisterCouponRequest(
        String code,
        BigDecimal discountAmount,
        ZonedDateTime expiresAt
) {
    public RegisterCouponService.Input toInput() {
        return new RegisterCouponService.Input(code, discountAmount, expiresAt);
    }
}