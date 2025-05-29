package com.example.demo.domain.coupon;

import com.example.demo.infra.coupon.CouponConsumerCommand;
import com.example.demo.infra.coupon.CouponScheduler;
import com.example.demo.support.util.Utils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import java.util.Collections;
import java.util.List;

import static com.example.demo.support.constants.RabbitmqConstant.EXCHANGE_COUPON;
import static com.example.demo.support.constants.RabbitmqConstant.ROUTE_COUPON_ISSUE;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final RabbitTemplate rabbitTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;


    public void issue(CouponCommand.Issue command) {
        Long couponId = command.getCouponId();
        String redisKey = CouponScheduler.COUPON_ISSUE_KEY;

        Long result = tryIssueCouponAtomic(couponId);

        if (result == null || result == -1) {
            Coupon coupon = couponRepository.findByCouponId(couponId)
                    .orElseThrow(() -> new RuntimeException("coupon not found"));

            redisTemplate.opsForZSet().add(redisKey, couponId.toString(), coupon.getQuantity());
            log.info("쿠폰 캐시 재초기화: couponId={}, quantity={}", couponId, coupon.getQuantity());

            result = tryIssueCouponAtomic(couponId);

            if (result == null || result <= 0) {
                throw new IllegalStateException("쿠폰 발급 실패 (초기화 후에도 재고 부족)");
            }
        }

        if (result == 0) {
            throw new IllegalStateException("쿠폰 재고 부족");
        }

        kafkaTemplate.send("coupon.issue", String.valueOf(command.getCouponId()), CouponConsumerCommand.Issue.of(command.getUserId(), couponId));
        log.info("쿠폰 발급 이벤트 발행 완료: {}", Utils.toJson(CouponConsumerCommand.Issue.of(command.getUserId(), couponId)));

//        rabbitTemplate.convertAndSend(
//                EXCHANGE_COUPON,
//                ROUTE_COUPON_ISSUE,
//                CouponConsumerCommand.Issue.of(command.getUserId(), couponId)
//        );
//        log.info("쿠폰 발급 메시지 전송 완료: userId={}, couponId={}, remain={}", command.getUserId(), couponId, result);
    }

    private Long tryIssueCouponAtomic(Long couponId) {
        String redisKey = CouponScheduler.COUPON_ISSUE_KEY;

        String script =
                "local key = KEYS[1]\n" +
                        "local cid = tostring(ARGV[1])\n" +
                        "local stock = redis.call('ZSCORE', key, cid)\n" +
                        "if (not stock) then return -1 end\n" +
                        "if (tonumber(stock) < 1) then return 0 end\n" +
                        "local newStock = redis.call('ZINCRBY', key, -1, cid)\n" +
                        "if (tonumber(newStock) < 0) then redis.call('ZINCRBY', key, 1, cid) return 0 end\n" +
                        "return tonumber(newStock)";

        return redisTemplate.execute(
                new DefaultRedisScript<>(script, Long.class),
                Collections.singletonList(redisKey),
                couponId.toString()
        );
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
