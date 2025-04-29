package com.example.demo.infra.comm.lock;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RedissonLockAspect {

    private final RedissonClient redissonClient;

    @Around("@annotation(com.example.demo.infra.comm.lock.RedissonLock)")
    public Object redissonLock(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RedissonLock annotation = method.getAnnotation(RedissonLock.class);

        String lockKey = method.getName() +
                CustomSpringElParser.getDynamicValue(signature.getParameterNames(), joinPoint.getArgs(), annotation.value());

        RLock lock = redissonClient.getLock(lockKey);

        boolean lockable = false;
        try {
            lockable = lock.tryLock(annotation.waitTime(), annotation.leaseTime(), TimeUnit.MILLISECONDS);
            if (!lockable) {
                log.warn("Lock 획득 실패: {}", lockKey);
                throw new RuntimeException("락 획득 실패");
            }

            log.info("Lock 획득 성공: {}", lockKey);
            return joinPoint.proceed(); //
        } catch (InterruptedException e) {
            log.error("Lock 처리 중 인터럽트", e);
            throw e;
        } finally {
            if (lockable && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("Lock 해제 완료: {}", lockKey);
            }
        }
    }
}
