package com.example.demo.infra.coupon;

import com.example.demo.domain.coupon.UserCoupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface UserCouponJpaRepository extends JpaRepository<UserCoupon, Long> {

    Optional<UserCoupon> findByCouponIdAndUserId(Long couponId, Long userId);

    Optional<UserCoupon> findByCouponId(Long couponId);
}
