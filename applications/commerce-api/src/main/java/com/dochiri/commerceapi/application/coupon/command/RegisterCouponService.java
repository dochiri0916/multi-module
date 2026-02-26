package com.dochiri.commerceapi.application.coupon.command;

import com.dochiri.commerceapi.domain.coupon.Coupon;
import com.dochiri.commerceapi.domain.coupon.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Service
@RequiredArgsConstructor
public class RegisterCouponService {

    private final CouponRepository couponRepository;

    @Transactional
    public Output execute(Input input) {
        return Output.of(
                couponRepository.save(
                        Coupon.register(
                                input.code, input.discountAmount, input.expiresAt
                        )
                )
        );
    }

    public record Input(
            String code,
            BigDecimal discountAmount,
            ZonedDateTime expiresAt
    ) {
    }

    public record Output(Coupon coupon) {
        public static Output of(Coupon coupon) {
            return new Output(coupon);
        }
    }

}