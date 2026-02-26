package com.dochiri.commerceapi.presentation.coupon.response;

import com.dochiri.commerceapi.application.coupon.command.RegisterCouponService.Output;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

public record RegisterCouponResponse(
        Long id,
        String code,
        BigDecimal discountAmount,
        ZonedDateTime expiresAt
) {
    public static RegisterCouponResponse from(Output output) {
        var coupon = output.coupon();
        return new RegisterCouponResponse(
                coupon.getId(),
                coupon.getCode(),
                coupon.getDiscountAmount(),
                coupon.getExpiresAt()
        );
    }
}