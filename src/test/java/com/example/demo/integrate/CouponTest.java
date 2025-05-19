package com.example.demo.integrate;

import com.example.demo.domain.coupon.*;
import com.example.demo.domain.coupon.CouponService;
import com.example.demo.infra.coupon.CouponConsumerCommand;
import com.example.demo.infra.coupon.CouponScheduler;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.example.demo.support.constants.RabbitmqConstant.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
public class CouponTest {

    @Autowired
    private CouponService couponService;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private CouponScheduler couponScheduler;

    private static final String REDIS_KEY = CouponScheduler.COUPON_ISSUE_KEY;

    private Long couponId;

    @BeforeEach
    void setUp() {
        redisTemplate.delete(REDIS_KEY);
        Coupon coupon = Coupon.create("통합테스트쿠폰", 20, 5L);
        couponId = couponRepository.save(coupon).getId();
    }

    @Test
    @DisplayName("Redis에 캐시가 없을 경우 DB조회 후 캐시 저장 후 발급")
    void issue_withNoCache_thenInitAndIssue() {
        couponScheduler.initCoupon();

        couponService.issue(CouponCommand.Issue.of(1L, couponId));

        Double remain = redisTemplate.opsForZSet().score(REDIS_KEY, couponId);
        assertThat(remain).isEqualTo(4.0);
    }

    @Test
    @DisplayName("Redis 캐시가 없고, DB 재고도 0일 경우 예외")
    void issue_withNoCache_andNoStock_thenFail() {
        Coupon coupon = couponRepository.findById(couponId).orElseThrow();
        for (int i = 0; i < 5; i++) {
            coupon.use();
        }
        couponRepository.save(coupon);

        assertThatThrownBy(() -> couponService.issue(CouponCommand.Issue.of(2L, couponId)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("쿠폰 재고 부족");
    }

    @Test
    @DisplayName(" Redis에 캐시가 있고 재고가 남은 경우 정상 발급")
    void issue_withCachedStock_thenSuccess() {
        couponScheduler.initCoupon();

        couponService.issue(CouponCommand.Issue.of(3L, couponId));
        Double remain = redisTemplate.opsForZSet().score(REDIS_KEY, couponId);

        assertThat(remain).isEqualTo(4.0);
    }

    @Test
    @DisplayName(" Redis 재고 0일 경우 예외")
    void issue_withCacheStockZero_thenFail() {
        redisTemplate.opsForZSet().add(REDIS_KEY, couponId, 0);

        assertThatThrownBy(() -> couponService.issue(CouponCommand.Issue.of(4L, couponId)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("쿠폰 재고 부족");
    }

    @Test
    void 쿠폰_DLQ_테스트() throws InterruptedException {
        Long invalidCouponId = 999L;
        Long userId = 1L;

        CouponConsumerCommand.Issue command = CouponConsumerCommand.Issue.of(invalidCouponId, userId);
        rabbitTemplate.convertAndSend(EXCHANGE_COUPON, ROUTE_COUPON_ISSUE, command);

        Thread.sleep(12000);

        Message dlqMessage = rabbitTemplate.receive(QUEUE_COUPON_ISSUE_DLQ);
        AssertionsForClassTypes.assertThat(dlqMessage).isNotNull();
    }
}
