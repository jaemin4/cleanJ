package com.example.demo.index;

import com.example.demo.domain.payment.PaymentHistory;
import com.example.demo.infra.payment.PaymentJdbcRepository;
import jakarta.transaction.Transactional;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

@SpringBootTest
@Transactional
@Rollback(false)
public class IndexTest {

    @Autowired
    PaymentJdbcRepository paymentJdbcRepository;

    @Test
    void 벌크결제이력_저장() {
        List<PaymentHistory> list = new ArrayList<>();
        Random random = new Random();

        for (long i = 1; i <= 30_000; i++) {
            long userId = i % 100;
            long amount = 1000 + i;
            long orderId = random.nextInt(30) + 1; // 1 ~ 30
            String transactionId = "tx-" + i;
            String status = random.nextBoolean() ? "PAID" : "CANCEL";

            list.add(PaymentHistory.create(userId, amount, orderId, transactionId, status));
        }

        paymentJdbcRepository.saveAll(list);

        int count = paymentJdbcRepository.count();
        System.out.println("저장된 결제 이력 수: " + count);
        Assertions.assertThat(count).isEqualTo(53_000);
    }

    @Test
    void 쿼리_평균_시간_테스트() {
        long totalTime = 0;
        for (int i = 0; i < 100; i++) {
            long start = System.currentTimeMillis();
            List<Map<String, Object>> result = paymentJdbcRepository.findTop5OrdersByPaidStatusIndexed();
            long end = System.currentTimeMillis();
            totalTime += (end - start);
        }
        System.out.println("쿼리 평균 실행 시간: " + (totalTime / 100.0) + "ms");
    }

}
