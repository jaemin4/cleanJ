package com.example.demo.domain.coupon;

import com.example.demo.infra.coupon.CouponConsumerCommand;
import com.example.demo.infra.coupon.CouponScheduler;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.example.demo.support.constants.RabbitmqConstant.EXCHANGE_COUPON;
import static com.example.demo.support.constants.RabbitmqConstant.ROUTE_COUPON_ISSUE;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final RabbitTemplate rabbitTemplate;
    private final RedisTemplate<String,Object> redisTemplate;
    private final RedissonClient redissonClient;

    public void issue(CouponCommand.Issue command) {
        Long couponId = command.getCouponId();
        String redisKey = CouponScheduler.COUPON_ISSUE_KEY;

        Double score = redisTemplate.opsForZSet().score(redisKey, couponId);
        if (score == null) {
            RLock lock = redissonClient.getLock("lock:coupon:init:" + couponId);
            boolean isLocked = false;

            try {
                isLocked = lock.tryLock(3, 1, TimeUnit.SECONDS);
                if (isLocked) {
                    score = redisTemplate.opsForZSet().score(redisKey, couponId);
                    if (score == null) {
                        Coupon coupon = couponRepository.findByCouponId(couponId)
                                .orElseThrow(() -> new RuntimeException("coupon not found"));
                        redisTemplate.opsForZSet().add(redisKey, couponId, (double) coupon.getQuantity());
                        log.info("[CACHE INIT] 쿠폰 ZSET 초기화: couponId={}, quantity={}", couponId, coupon.getQuantity());
                        score = (double) coupon.getQuantity();
                    }
                } else {
                    Thread.sleep(100);
                    score = redisTemplate.opsForZSet().score(redisKey, couponId);
                    if (score == null) {
                        throw new IllegalStateException("쿠폰 캐시 초기화 실패");
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("락 대기 중 인터럽트 발생", e);
            } finally {
                if (isLocked && lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }

        if (score < 1) {
            throw new IllegalStateException("쿠폰 재고 부족");
        }

        Double remain = redisTemplate.opsForZSet().incrementScore(redisKey, couponId, -1);
        if (remain == null || remain < 0) {
            redisTemplate.opsForZSet().incrementScore(redisKey, couponId, 1);
            throw new IllegalStateException("쿠폰 재고 부족 (차감 실패)");
        }

        rabbitTemplate.convertAndSend(
                EXCHANGE_COUPON,
                ROUTE_COUPON_ISSUE,
                CouponConsumerCommand.Issue.of(command.getUserId(), couponId)
        );

        log.info("쿠폰 발급 메시지 전송 완료: userId={}, couponId={}", command.getUserId(), couponId);
    }


    @Transactional
    public void use(CouponCommand.Use command) {
        final long userId = command.getUserId();
        final long userCouponId = command.getUserCouponId();

        UserCoupon userCoupon = findUserCoupon(userId,userCouponId);
        userCoupon.markAsUsed();

        userCouponRepository.save(userCoupon);
    }

    public long calculateDiscountedAmount(long originalPrice, Long couponId) {
        if (couponId == null) {
            return originalPrice;
        }

        double discountRate = findCoupon(couponId).getDiscountRate();

        long discountedPrice = (long) (originalPrice - (originalPrice * discountRate * 0.01));

        return Math.max(discountedPrice, 0L);
    }

    public List<CouponInfo.GetAllQuantity> findAllQuantity() {
        List<Coupon> list = couponRepository.findAll();

        return list.stream()
                .map(coupon -> CouponInfo.GetAllQuantity.of(coupon.getId(), coupon.getQuantity()))
                .toList();
    }

    private Coupon findCoupon(Long couponId) {
        return couponRepository.findById(couponId).orElseThrow(
                () -> new RuntimeException("coupon could not be found")
        );
    }

    private UserCoupon findUserCoupon(Long userId, Long userCouponId) {
        log.info("userId : {}, userCouponId : {}", userId, userCouponId);
        UserCoupon userCoupon = userCouponRepository.findByCouponId(userCouponId)
                .orElseThrow(() -> new RuntimeException("쿠폰을 찾을 수 없습니다."));

        if (!userCoupon.getUserId().equals(userId)) {
            throw new RuntimeException("해당 유저의 쿠폰이 아닙니다.");
        }
        return userCoupon;
    }



}
