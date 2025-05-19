package com.example.demo.infra.payment;

import com.example.demo.domain.payment.PaymentHistoryCommand;
import com.example.demo.domain.payment.PaymentHistoryService;
import com.example.demo.support.util.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import static com.example.demo.infra.payment.PaymentPopularScheduler.POPULAR_PRODUCTS_KEY;
import static com.example.demo.support.constants.RabbitmqConstant.QUEUE_PAYMENT_HISTORY_DB_SAVE;
import static com.example.demo.support.constants.RabbitmqConstant.QUEUE_PAYMENT_HISTORY_REDIS_UPDATE;

@Service
@Profile("consumer")
@Slf4j
@RequiredArgsConstructor
public class PaymentHistoryConsumerService {

    private final PaymentHistoryService paymentHistoryService;
    private final RedisTemplate<String,Object> redisTemplate;

    @RabbitListener(queues =QUEUE_PAYMENT_HISTORY_DB_SAVE, concurrency = "1")
    public void save(PaymentHistoryConsumerCommand.Save command) {
        try{
            paymentHistoryService.recordPaymentHistory(
                    PaymentHistoryCommand.Save.of(command.getUserId(),command.getAmount(),command.getOrderId(),command.getTransactionId(),command.getStatus())
            );

        }catch (Exception e) {
            log.error("결제 이력 저장 실패 : {}", Utils.toJson(command));
        }
    }

    @RabbitListener(queues = QUEUE_PAYMENT_HISTORY_REDIS_UPDATE, concurrency = "1")
    public void updatePaymentHistoryRank(String orderId) {
        try {
            redisTemplate.opsForZSet().incrementScore(POPULAR_PRODUCTS_KEY, orderId, 1);
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis 연결 실패: orderId={}", orderId);
        } catch (Exception e) {
            log.error("인기 상품 랭킹 업데이트 실패: orderId={}", orderId, e);
        }
    }


}
