package com.example.demo.concurrency;

import com.example.demo.domain.coupon.*;
import com.example.demo.infra.coupon.CouponConsumerCommand;
import com.example.demo.infra.coupon.CouponScheduler;
import com.example.demo.support.constants.RabbitmqConstant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.amqp.rabbit.support.micrometer.RabbitTemplateObservation.TemplateLowCardinalityTags.ROUTING_KEY;

@SpringBootTest
public class CouponTest {

    @Autowired
    private CouponService couponService;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private UserCouponRepository userCouponRepository;

    @Autowired
    private CouponScheduler couponScheduler;

    @Test
    public void 쿠폰_200명_발급_테스트_동시성_성공_비동기() throws InterruptedException {
        Coupon coupon = Coupon.create("테스트 쿠폰", 100, 100L);
        Coupon saved = couponRepository.save(coupon);
        Long couponId = saved.getId();

        couponScheduler.initCoupon();
        Thread.sleep(300);

        int userCount = 200;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
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
        executorService.shutdown();

        // 큐 처리 완료 대기
        Thread.sleep(15_000);

        long remaining = couponRepository.findById(couponId).orElseThrow().getQuantity();
        long issuedCount = userCouponRepository.count();

        System.out.println("[Result] 남은 수량: " + remaining);
        System.out.println("[Result] 발급된 유저 수: " + issuedCount);

        assertThat(issuedCount).isEqualTo(100L);
    }


}
