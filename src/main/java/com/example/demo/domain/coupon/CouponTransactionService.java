package com.example.demo.domain.coupon;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CouponTransactionService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;

    @Transactional
    public void issueWithTransaction(CouponCommand.Issue command) {
        Long couponId = command.getCouponId();
        Long userId = command.getUserId();

        Coupon coupon = couponRepository.findByCouponId(couponId)
                .orElseThrow(() -> new RuntimeException("coupon not found"));

        coupon.use();
        couponRepository.save(coupon);

        try {
            userCouponRepository.save(UserCoupon.issue(couponId, userId));
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("중복 발급 불가. couponId=" + couponId);
        }
    }
}
