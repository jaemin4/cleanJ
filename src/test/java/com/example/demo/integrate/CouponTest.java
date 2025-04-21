package com.example.demo.integrate;



import com.example.demo.domain.coupon.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
public class CouponTest {

    @Autowired
    private CouponService couponService;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private UserCouponRepository userCouponRepository;

    @Test
    @DisplayName("쿠폰 발급 성공")
    void testIssueCouponSuccess() {
        final long couponId = 1L;
        final long userId = 10L;

        couponService.issue(CouponCommand.Issue.of(userId, couponId));

        UserCoupon userCoupon = userCouponRepository.findByCouponIdAndUserId(couponId, userId)
                .orElseThrow(() -> new RuntimeException("UserCoupon not found"));

        assertThat(userCoupon.getUserId()).isEqualTo(userId);
        assertThat(userCoupon.getCouponId()).isEqualTo(couponId);

        Coupon updatedCoupon = couponRepository.findById(couponId).orElseThrow();
        assertThat(updatedCoupon.getQuantity()).isEqualTo(1);
    }

    @Test
    @DisplayName("중복 발급 시 예외 발생")
    void testIssueDuplicateCouponFail() {
        final long couponId = 1L;
        final long userId = 8L;

        couponService.issue(CouponCommand.Issue.of(userId, couponId));

        assertThatThrownBy(() ->
                couponService.issue(CouponCommand.Issue.of(userId, couponId))
        ).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("중복 발급");
    }

    @Test
    @DisplayName("쿠폰 수량이 0이면 예외 발생")
    void testCouponQuantityEmptyFail() {
        // couponId = 2L은 quantity가 0인 상태라고 가정
        final long couponId = 1L;
        final long userId = 12L;

        assertThatThrownBy(() ->
                couponService.issue(CouponCommand.Issue.of(userId, couponId))
        ).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("쿠폰 수량이 부족합니다");
    }

    @Test
    @DisplayName("쿠폰 사용 성공")
    void testUseCouponSuccess() {
        final long couponId = 2L;
        final long userId = 19;

        // 발급 먼저
        couponService.issue(CouponCommand.Issue.of(userId, couponId));
        UserCoupon userCoupon = userCouponRepository.findByCouponIdAndUserId(couponId, userId)
                .orElseThrow();

        // 사용
        couponService.use(CouponCommand.Use.of(userCoupon.getCouponId(),userId));

        UserCoupon used = userCouponRepository.findByCouponIdAndUserId(userCoupon.getCouponId(), userId)
                .orElseThrow();

        assertThat(used.isUsed()).isTrue();
    }


}
