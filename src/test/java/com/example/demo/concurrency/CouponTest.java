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
    public void 쿠폰_100명_발급_테스트_동시성_실패_확인() throws InterruptedException {
        // todo 초기 쿠폰 등록 (100개 수량)
        Coupon coupon = Coupon.create("테스트 쿠폰", 100, 100L);
        Coupon saved = couponRepository.save(coupon);
        Long couponId = saved.getId();

        // todo  유저 수 100명 및 스레드 15개 설정
        int userCount = 100;
        int threadPerUser = 5;
        int totalThread = userCount * threadPerUser;

        ExecutorService executorService = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(totalThread);

        for (long userId = 1; userId <= userCount; userId++) {
            final long finalUserId = userId;
            for (int i = 0; i < threadPerUser; i++) {
                executorService.execute(() -> {
                    try {
                        couponService.issue(CouponCommand.Issue.of(finalUserId,couponId));
                    } catch (Exception e) {
                        System.out.println("Exception: " + e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                });
            }
        }

        latch.await();

        // todo 동시성 문제가 있을 경우 quantity가 0이 아닐 수 있음
        Long quantity = couponRepository.findById(couponId).get().getQuantity();

        // todo quantity == 0 이라고 가정하면 실패해야 함 (즉 실제로는 실패하게 만들어야 성공)
        boolean failedAsExpected = quantity != 0;

        // todo  // 기대: 동시성 실패 → 수량이 0이 아님 → 테스트 성공
        System.out.println("남은 수량: " + quantity);
        assertThat(failedAsExpected).isTrue();
    }






}
