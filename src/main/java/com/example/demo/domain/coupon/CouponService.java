package com.example.demo.domain.coupon;

import com.example.demo.infra.coupon.CouponConsumerCommand;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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

    public void issue(CouponCommand.Issue command) {
        rabbitTemplate.convertAndSend(
                EXCHANGE_COUPON,
                ROUTE_COUPON_ISSUE,
                CouponConsumerCommand.Issue.of(command.getUserId(), command.getCouponId())
        );
        log.info("쿠폰 발급 메시지 전송 완료: userId={}, couponId={}", command.getUserId(), command.getCouponId());
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
