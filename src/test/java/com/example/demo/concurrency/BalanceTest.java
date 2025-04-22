package com.example.demo.concurrency;

import com.example.demo.domain.balance.BalanceCommand;
import com.example.demo.domain.balance.BalanceRepository;
import com.example.demo.domain.balance.BalanceService;
import com.example.demo.domain.balance.Balance;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class BalanceTest {
    @Autowired
    private BalanceService balanceService;

    @Autowired
    private BalanceRepository balanceRepository;

    private final Long USER_ID = 1L;

    @BeforeEach
    public void setUp() {
        balanceRepository.deleteAll();
        Balance balance = Balance.create(USER_ID, 100L);
        balanceRepository.save(balance);
    }

    @DisplayName("동시성 문제가 발생되지 않을때는 정상적으로 충전되고, 동시에 충전 요청했을때 낙관적락 예외가 발생된다.")
    @Test
    public void charge() throws InterruptedException {
        int threadCount = 2;
        long amountPerCharge = 10L;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger optimisticLockExceptions = new AtomicInteger(0);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.execute(() -> {
                try {
                    balanceService.charge(BalanceCommand.Charge.of(USER_ID, amountPerCharge));
                    successCount.incrementAndGet();
                } catch (ObjectOptimisticLockingFailureException e) {
                    optimisticLockExceptions.incrementAndGet();
                    System.out.println("낙관적 락 충돌 예외 발생: " + e.getMessage());
                } catch (Exception e) {
                    System.out.println("기타 예외 발생: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        Balance result = balanceRepository.findByUserId(USER_ID).orElseThrow();
        long expectedAmount = successCount.get() * amountPerCharge;

        System.out.println("최종 잔액: " + result.getAmount());
        System.out.println(" 성공한 스레드 수: " + successCount.get());
        System.out.println("낙관적 락 충돌 횟수: " + optimisticLockExceptions.get());

        assertThat(result.getAmount()).isEqualTo(100L+expectedAmount);
    }

    @DisplayName("동시성 문제가 발생되지 않을때는 정상적으로 사용되고, 동시에 잔액사용 요청시 낙관적락 충돌 예외 발생가 발생된다.")
    @Test
    public void use() throws InterruptedException {
        int threadCount = 3;
        long amountPerUse = 10L;

        // 초기 잔액: 충분히 크게 설정
        Balance initial = Balance.create(USER_ID, 200L);
        balanceRepository.deleteAll();
        balanceRepository.save(initial);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger optimisticLockExceptions = new AtomicInteger(0);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.execute(() -> {
                try {
                    balanceService.use(BalanceCommand.Use.of(USER_ID, amountPerUse));
                    successCount.incrementAndGet();
                } catch (ObjectOptimisticLockingFailureException e) {
                    optimisticLockExceptions.incrementAndGet();
                    System.out.println("낙관적 락 충돌 예외 발생: " + e.getMessage());
                } catch (Exception e) {
                    System.out.println("기타 예외 발생: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        Balance result = balanceRepository.findByUserId(USER_ID).orElseThrow();
        long expectedAmount = 200L - (successCount.get() * amountPerUse);

        System.out.println("최종 잔액: " + result.getAmount());
        System.out.println("성공한 스레드 수: " + successCount.get());
        System.out.println("낙관적 락 충돌 횟수: " + optimisticLockExceptions.get());

        assertThat(result.getAmount()).isEqualTo(expectedAmount);
    }
}
