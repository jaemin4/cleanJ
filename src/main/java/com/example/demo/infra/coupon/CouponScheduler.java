package com.example.demo.infra.coupon;

import com.example.demo.domain.coupon.CouponInfo;
import com.example.demo.domain.coupon.CouponService;
import com.example.demo.support.util.Utils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponScheduler {

    private final CouponService couponService;
    private final RedisTemplate<String,Object> redisTemplate;
    private final RedissonClient redissonClient;
    public static final String COUPON_ISSUE_KEY = "coupon:issue";

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




}
