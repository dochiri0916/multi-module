package com.dochiri.commerceapi.presentation.coupon;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

import com.dochiri.commerceapi.application.coupon.command.CouponService;
import com.dochiri.commerceapi.application.coupon.command.RegisterCouponService;
import com.dochiri.commerceapi.domain.coupon.Coupon;
import com.dochiri.commerceapi.domain.coupon.CouponStatus;
import com.dochiri.commerceapi.presentation.coupon.request.RegisterCouponRequest;
import com.dochiri.commerceapi.presentation.coupon.response.RegisterCouponResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;
    private final RegisterCouponService registerCouponService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<RegisterCouponResponse> create(@Valid @RequestBody RegisterCouponRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(RegisterCouponResponse
                        .from(registerCouponService.execute(
                                        request.toInput()
                                )
                        )
                );
    }

    @GetMapping("/{couponId}")
    public CouponResponse get(@PathVariable Long couponId) {
        return CouponResponse.from(couponService.get(couponId));
    }

    @PostMapping("/{couponId}/use")
    public CouponResponse use(@PathVariable Long couponId) {
        return CouponResponse.from(couponService.use(couponId));
    }

    public record CreateCouponRequest(String code, BigDecimal discountAmount, ZonedDateTime expiresAt) {
    }

    public record CouponResponse(
            Long id,
            String code,
            BigDecimal discountAmount,
            CouponStatus status,
            ZonedDateTime expiresAt,
            ZonedDateTime usedAt
    ) {
        static CouponResponse from(Coupon coupon) {
            return new CouponResponse(
                    coupon.getId(),
                    coupon.getCode(),
                    coupon.getDiscountAmount(),
                    coupon.getStatus(),
                    coupon.getExpiresAt(),
                    coupon.getUsedAt()
            );
        }
    }
}
