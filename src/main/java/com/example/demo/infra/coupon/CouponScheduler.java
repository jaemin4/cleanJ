package com.example.demo.infra.coupon;

import com.example.demo.domain.coupon.CouponInfo;
import com.example.demo.domain.coupon.CouponService;
import com.example.demo.support.util.Utils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.example.demo.support.constants.RabbitmqConstant.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponScheduler {

    private final CouponService couponService;
    private final RedisTemplate<String,Object> redisTemplate;
    private final RedissonClient redissonClient;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    public static final String COUPON_ISSUE_KEY = "coupon:issue";
    private final CouponConsumer couponConsumer;

    @PostConstruct
    public void initCoupon() {
        RLock lock = redissonClient.getLock("lock:coupon:issue:init");
        boolean isLocked = false;

        try{
            isLocked = lock.tryLock(0,3, TimeUnit.SECONDS);
            if (!isLocked) {
                log.info("쿠폰 발급 락 획득 실패");
                return;
            }
            List<CouponInfo.GetAllQuantity> list = couponService.findAllQuantity();
            log.info("쿠폰 스케쥴링 데이터 : {}", Utils.toJson(list));

            for(CouponInfo.GetAllQuantity coupon : list){
                redisTemplate.opsForZSet().add(COUPON_ISSUE_KEY, coupon.getCouponId(), coupon.getQuantity());
            }

        }catch (Exception e){
            log.error("쿠폰 등록 실패 : {}", e.getMessage());
        }finally {
            if(isLocked && lock.isHeldByCurrentThread()){
                lock.unlock();
            }
        }
    }


    @Scheduled(fixedRate = 10_000)
    public void retryCouponIssueFromDlq() {
        log.info("[DLQ Retry] 쿠폰 발급 DLQ 메시지 재처리 시작");
        int retryLimit = 50;
        for (int i = 0; i < retryLimit; i++) {
            Message message = rabbitTemplate.receive(QUEUE_COUPON_ISSUE_DLQ);
            if (message == null) break;

            try {
                CouponConsumerCommand.Issue command = objectMapper.readValue(message.getBody(), CouponConsumerCommand.Issue.class);
                couponConsumer.issue(command);
                log.info("[DLQ Retry] 재처리 성공: userId={}, couponId={}", command.getUserId(), command.getCouponId());

            } catch (Exception e) {
                log.error("[DLQ Retry] 재처리 실패: {}", e.getMessage(), e);

                rabbitTemplate.send(EXCHANGE_COUPON, ROUTE_COUPON_ISSUE_DLQ, message);
            }
        }

        log.info("[DLQ Retry] 쿠폰 발급 DLQ 메시지 재처리 종료");
    }



}
