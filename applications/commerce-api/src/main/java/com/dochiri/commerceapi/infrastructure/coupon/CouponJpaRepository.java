package com.dochiri.commerceapi.infrastructure.coupon;

import com.dochiri.commerceapi.domain.coupon.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CouponJpaRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByCode(String code);

}