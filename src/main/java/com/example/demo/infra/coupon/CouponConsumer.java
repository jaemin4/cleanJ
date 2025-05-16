package com.example.demo.infra.coupon;

import com.example.demo.domain.coupon.Coupon;
import com.example.demo.domain.coupon.CouponRepository;
import com.example.demo.domain.coupon.UserCoupon;
import com.example.demo.domain.coupon.UserCouponRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import static com.example.demo.support.constants.RabbitmqConstant.QUEUE_COUPON_ISSUE;
import static com.example.demo.support.constants.RabbitmqConstant.QUEUE_COUPON_ISSUE_DLQ;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("consumer")
public class CouponConsumer {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;


    @RabbitListener(queues = QUEUE_COUPON_ISSUE, concurrency = "1")
    public void issue(CouponConsumerCommand.Issue command) {
        try {

            Long couponId = command.getCouponId();
            Long userId = command.getUserId();

            Coupon coupon = couponRepository.findByCouponId(couponId)
                    .orElseThrow(() -> new RuntimeException("coupon not found: id=" + couponId));

            coupon.use();
            couponRepository.save(coupon);

            userCouponRepository.save(UserCoupon.issue(couponId, userId));
            log.info("쿠폰 발급 성공: couponId={}, userId={}", couponId, userId);

        } catch (Exception e) {
            log.error("쿠폰 발급 실패: couponId={}, userId={}, error={}", command.getCouponId(), command.getUserId(), e.getMessage(), e);
            throw new RuntimeException("쿠폰 발급 실패");
        }
    }


}
