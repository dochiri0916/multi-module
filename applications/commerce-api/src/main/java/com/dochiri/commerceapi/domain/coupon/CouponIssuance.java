package com.dochiri.commerceapi.domain.coupon;

import com.dochiri.jpa.common.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Table(name = "coupon_issuances")
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponIssuance extends BaseEntity {



}