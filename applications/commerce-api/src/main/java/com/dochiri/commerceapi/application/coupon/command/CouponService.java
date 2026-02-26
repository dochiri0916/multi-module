package com.dochiri.commerceapi.application.coupon.command;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

import com.dochiri.commerceapi.domain.coupon.Coupon;
import com.dochiri.commerceapi.domain.coupon.CouponRepository;
import com.dochiri.commerceapi.domain.coupon.CouponStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CouponService {

    private final CouponRepository couponRepository;

    public CouponService(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    @Transactional
    public Coupon create(String code, BigDecimal discountAmount, ZonedDateTime expiresAt) {
        couponRepository.findByCode(code).ifPresent(coupon -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "coupon code already exists");
        });

        Coupon coupon = new Coupon(code, discountAmount, expiresAt);
        return couponRepository.save(coupon);
    }

    @Transactional(readOnly = true)
    public Coupon get(Long couponId) {
        return couponRepository.findById(couponId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "coupon not found"));
    }

    @Transactional
    public Coupon use(Long couponId) {
        Coupon coupon = get(couponId);

        if (coupon.getStatus() == CouponStatus.EXPIRED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "coupon is expired");
        }
        if (coupon.getStatus() == CouponStatus.USED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "coupon is already used");
        }

        try {
            coupon.use(ZonedDateTime.now());
            return coupon;
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }
}
