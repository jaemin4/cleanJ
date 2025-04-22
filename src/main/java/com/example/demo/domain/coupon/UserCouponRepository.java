package com.example.demo.domain.coupon;

import java.util.Optional;

public interface UserCouponRepository {
    UserCoupon save(UserCoupon coupon);
    Optional<UserCoupon> findById(long id);
    Optional<UserCoupon> findByCouponIdAndUserId(Long couponId, Long userId);
    Optional<UserCoupon> findByCouponId(Long couponId);
    long count();
}
