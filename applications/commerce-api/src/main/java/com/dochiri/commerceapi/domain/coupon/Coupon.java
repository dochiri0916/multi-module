package com.dochiri.commerceapi.domain.coupon;

import com.dochiri.jpa.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

import static java.util.Objects.requireNonNull;

@Getter
@Table(name = "coupons")
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Coupon extends BaseEntity {

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal discountAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CouponStatus status;

    @Column(nullable = false)
    private ZonedDateTime expiresAt;

    private ZonedDateTime usedAt;

    public static Coupon register(String code, BigDecimal discountAmount, ZonedDateTime expiresAt) {
        Coupon coupon = new Coupon();
        coupon.code = requireNonNull(code);
        coupon.discountAmount = requireNonNull(discountAmount);
        coupon.expiresAt = requireNonNull(expiresAt);
        coupon.usedAt = expiresAt;
        return coupon;
    }

    public boolean isExpiredAt(ZonedDateTime now) {
        return expiresAt.isBefore(now);
    }

    public void use(ZonedDateTime now) {
        if (status != CouponStatus.ISSUED) {
            throw new IllegalStateException("coupon is not in issued status");
        }
        if (isExpiredAt(now)) {
            throw new IllegalStateException("coupon is expired");
        }

        this.status = CouponStatus.USED;
        this.usedAt = now;
    }

}