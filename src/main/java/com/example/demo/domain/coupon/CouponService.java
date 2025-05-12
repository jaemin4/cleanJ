package com.example.demo.domain.coupon;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final RedissonClient redissonClient;
    private final CouponTransactionService couponTransactionService;

    public void issue(CouponCommand.Issue command) {
        String lockName = "lock:coupon:" + command.getCouponId();
        RLock lock = redissonClient.getLock(lockName);

        boolean isLocked = false;
        try {
            isLocked = lock.tryLock(5, 3, TimeUnit.SECONDS);
            if (!isLocked) {
                throw new IllegalStateException("락 획득 실패. 동시 요청 과다");
            }

            couponTransactionService.issueWithTransaction(command);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("락 대기 중 인터럽트 발생", e);
        } finally {
            if (isLocked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
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
