package com.example.demo.domain.coupon;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final Map<Long, Lock> couponLocks = new ConcurrentHashMap<>();

    public void issue(CouponCommand.Issue command) {
       final long couponId = command.getCouponId();
       final long userId = command.getUserId();

       duplicatedCoupon(couponId,userId);

       Coupon coupon = validCoupon(couponId);
       coupon.use();
       couponRepository.save(coupon);

       UserCoupon userCoupon = UserCoupon.issue(couponId,userId);
       userCouponRepository.save(userCoupon);
    }

    public void use(CouponCommand.Use command) {
        final long userId = command.getUserId();
        final long userCouponId = command.getUserCouponId();

        UserCoupon userCoupon = validUserCoupon(userId,userCouponId);
        userCoupon.markAsUsed();

        userCouponRepository.save(userCoupon);
    }

    public double getDiscountRate(CouponCommand.GetDiscountRate command) {
        Coupon coupon = validCoupon(command.getCouponId());
        return coupon.getDiscountRate();
    }

    private Coupon validCoupon(Long couponId) {
        return couponRepository.findById(couponId).orElseThrow(
                () -> new RuntimeException("coupon could not be found")
        );
    }

    private UserCoupon validUserCoupon(Long userId, Long userCouponId) {
        UserCoupon userCoupon = userCouponRepository.findByCouponId(userCouponId)
                .orElseThrow(() -> new RuntimeException("쿠폰을 찾을 수 없습니다."));

        if (!userCoupon.getUserId().equals(userId)) {
            throw new RuntimeException("해당 유저의 쿠폰이 아닙니다.");
        }
        return userCoupon;
    }

    private void duplicatedCoupon(Long couponId, Long userId) {
        userCouponRepository.findByCouponIdAndUserId(couponId,userId)
                .ifPresent(coupon -> {
                    throw new IllegalStateException("중복 발급이 불가능합니다. couponId=" + couponId);
                });
    }

}
