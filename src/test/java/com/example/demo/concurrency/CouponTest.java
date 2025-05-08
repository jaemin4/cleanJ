package com.example.demo.concurrency;

import com.example.demo.domain.coupon.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
public class CouponTest {

    @Autowired
    private CouponService couponService;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private UserCouponRepository userCouponRepository;


    @Test
    public void 쿠폰_200명_발급_테스트_동시성_성공_확인() throws InterruptedException {
        Coupon coupon = Coupon.create("테스트 쿠폰", 100, 100L);
        Coupon saved = couponRepository.save(coupon);
        Long couponId = saved.getId();
        System.out.println("[Init] 쿠폰 수량 = " + coupon.getQuantity());

        int userCount = 200;
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(userCount);

        for (long userId = 1; userId <= userCount; userId++) {
            final long finalUserId = userId;
            executorService.execute(() -> {
                try {
                    couponService.issue(CouponCommand.Issue.of(finalUserId, couponId));
                    System.out.println("[SUCCESS] userId=" + finalUserId);
                } catch (Exception e) {
                    System.out.println("[FAIL] userId=" + finalUserId + " => " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        long remaining = couponRepository.findById(couponId).get().getQuantity();
        long issuedCount = userCouponRepository.count();

        System.out.println("[Result] 남은 수량: " + remaining);
        System.out.println("[Result] 발급된 유저 수: " + issuedCount);

        assertThat(issuedCount).isEqualTo(100L);
    }







}
