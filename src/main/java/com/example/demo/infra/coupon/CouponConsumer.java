package com.example.demo.infra.coupon;

import com.example.demo.domain.coupon.Coupon;
import com.example.demo.domain.coupon.CouponRepository;
import com.example.demo.domain.coupon.UserCoupon;
import com.example.demo.domain.coupon.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;
import static com.example.demo.support.constants.RabbitmqConstant.QUEUE_COUPON_ISSUE;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("consumer")
public class CouponConsumer {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final RedissonClient redissonClient;

    @RabbitListener(queues = QUEUE_COUPON_ISSUE, concurrency = "1")
    public void issue(CouponConsumerCommand.Issue command) {
        String lockKey = "lock:coupon:issue:" + command.getCouponId();
        RLock lock = redissonClient.getLock(lockKey);
        boolean isLocked = false;

        try {
            isLocked = lock.tryLock(5, 3, TimeUnit.SECONDS);
            if (!isLocked) {
                throw new IllegalStateException("쿠폰 발급 락 획득 실패: couponId=" + command.getCouponId());
            }

            Long couponId = command.getCouponId();
            Long userId = command.getUserId();

            Coupon coupon = couponRepository.findByCouponId(couponId)
                    .orElseThrow(() -> new RuntimeException("coupon not found"));

            coupon.use();
            couponRepository.save(coupon);

            try {
                userCouponRepository.save(UserCoupon.issue(couponId, userId));
                log.info("쿠폰발급 성공: couponId={}, userId={}", couponId, userId);
            } catch (DataIntegrityViolationException e) {
                throw new IllegalStateException("중복 발급 불가. couponId=" + couponId);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("락 대기 중 인터럽트 발생", e);
        } catch (Exception e) {
            log.error("쿠폰 발급 실패: {}", e.getMessage());
        } finally {
            if (isLocked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }


}
