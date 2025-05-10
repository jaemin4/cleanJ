package com.example.demo.domain.coupon;

import java.util.Optional;

public interface CouponRepository {
    Coupon save(Coupon coupon);
    Optional<Coupon> findById(long couponId);
    Optional<Coupon> findByCouponIdForLock(Long couponId);
    Optional<Coupon> findByCouponId(Long couponId);

}
