package com.example.demo.infra.coupon;

import com.example.demo.domain.coupon.Coupon;
import com.example.demo.domain.coupon.CouponRepository;
import com.example.demo.domain.coupon.UserCoupon;
import com.example.demo.domain.coupon.UserCouponRepository;
import com.example.demo.support.util.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Slf4j
public class CouponKafkaConsumer {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 0),

            exclude = {org.apache.kafka.common.errors.RetriableException.class,
                       org.apache.kafka.common.errors.SerializationException.class,
                       org.springframework.dao.DataAccessException.class
            }
    )
    @KafkaListener(topics = "coupon.issue", groupId = "group_1")
    @Transactional
    public void handleCouponIssue(CouponConsumerCommand.Issue command) {
        log.info("데이터 : {}", Utils.toJson(command));



        try {
            log.info("데이터 : {}", Utils.toJson(command));

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
