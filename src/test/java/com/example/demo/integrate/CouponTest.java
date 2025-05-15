package com.example.demo.integrate;

import com.example.demo.domain.coupon.*;
import com.example.demo.infra.coupon.CouponConsumer;
import com.example.demo.infra.coupon.CouponConsumerCommand;
import com.example.demo.infra.coupon.CouponScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.Optional;

import static com.example.demo.support.constants.RabbitmqConstant.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
public class CouponTest {

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private UserCouponRepository userCouponRepository;

    @Autowired
    private CouponScheduler couponScheduler;

    @Autowired
    private CouponConsumer couponConsumer;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Test
    void 쿠폰_DLQ_테스트() throws InterruptedException {
        Long invalidCouponId = 999L;
        Long userId = 1L;

        CouponConsumerCommand.Issue command = CouponConsumerCommand.Issue.of(invalidCouponId, userId);
        rabbitTemplate.convertAndSend(EXCHANGE_COUPON, ROUTE_COUPON_ISSUE, command);

        Thread.sleep(12000);

        Message dlqMessage = rabbitTemplate.receive(QUEUE_COUPON_ISSUE_DLQ);
        assertThat(dlqMessage).isNotNull();
    }

}
