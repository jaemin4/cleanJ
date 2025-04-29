package com.example.demo.infra.coupon;

import com.example.demo.domain.coupon.Coupon;
import com.example.demo.domain.coupon.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CouponRepositoryImpl implements CouponRepository {

    private final CouponJpaRepository couponJpaRepository;

    @Override
    public Coupon save(Coupon coupon) {
        return couponJpaRepository.save(coupon);
    }

    @Override
    public Optional<Coupon> findById(long id) {
        return couponJpaRepository.findById(id);
    }

    @Override
    public Optional<Coupon> findByCouponIdForLock(Long couponId) {
        return couponJpaRepository.findByCouponIdForLock(couponId);
    }

    @Override
    public Optional<Coupon> findByCouponId(Long couponId) {
        return couponJpaRepository.findById(couponId);
    }
}
