package com.example.demo.config;

import com.example.demo.support.properties.RabbitmqProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.example.demo.support.constants.RabbitmqConstant.*;

@RequiredArgsConstructor
@Configuration
public class RabbitmqConfig {
    private final RabbitmqProperties rabbitmqProperties;

    /*
        AccessLog
    */
    @Bean
    public DirectExchange exchangeAccessLog() {
        return new DirectExchange(EXCHANGE_ACCESS_LOG);
    }

    @Bean
    public Queue queueAccessLogSave() {
        return new Queue(QUEUE_ACCESS_LOG_SAVE, true);
    }

    @Bean
    public Binding bindingAccessLogSave(Queue queueAccessLogSave, DirectExchange exchangeAccessLog) {
        return BindingBuilder.bind(queueAccessLogSave)
                .to(exchangeAccessLog)
                .with(ROUTE_ACCESS_LOG_SAVE);
    }

    /*
        PaymentHistory + DLQ
    */
    @Bean
    public DirectExchange exchangePaymentHistory() {
        return new DirectExchange(EXCHANGE_PAYMENT_HISTORY);
    }

    @Bean
    public Queue queuePaymentHistoryDbSave() {
        return QueueBuilder.durable(QUEUE_PAYMENT_HISTORY_DB_SAVE)
                .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE_PAYMENT)
                .withArgument("x-dead-letter-routing-key", ROUTE_PAYMENT_HISTORY_DB_SAVE_DLQ)
                .build();
    }

    @Bean
    public Binding bindingPaymentHistoryDbSave(Queue queuePaymentHistoryDbSave, DirectExchange exchangePaymentHistory) {
        return BindingBuilder.bind(queuePaymentHistoryDbSave)
                .to(exchangePaymentHistory)
                .with(ROUTE_PAYMENT_HISTORY_DB_SAVE);
    }

    @Bean
    public Queue queuePaymentHistoryRankingUpdate() {
        return new Queue(QUEUE_PAYMENT_HISTORY_REDIS_UPDATE, true);
    }

    @Bean
    public Binding bindingPaymentHistoryRankingUpdate(Queue queuePaymentHistoryRankingUpdate, DirectExchange exchangePaymentHistory) {
        return BindingBuilder.bind(queuePaymentHistoryRankingUpdate)
                .to(exchangePaymentHistory)
                .with(ROUTE_PAYMENT_HISTORY_REDIS_UPDATE);
    }

    /*
        PaymentHistory DLQ
    */
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE_PAYMENT);
    }

    @Bean
    public Queue queuePaymentHistoryDbSaveDLQ() {
        return new Queue(QUEUE_PAYMENT_HISTORY_DB_SAVE_DLQ, true);
    }

    @Bean
    public Binding bindingPaymentHistoryDbSaveDLQ(Queue queuePaymentHistoryDbSaveDLQ, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(queuePaymentHistoryDbSaveDLQ)
                .to(deadLetterExchange)
                .with(ROUTE_PAYMENT_HISTORY_DB_SAVE_DLQ);
    }

    /*
        Coupon
    */
    @Bean
    public DirectExchange exchangeCoupon() {
        return new DirectExchange(EXCHANGE_COUPON);
    }

    @Bean
    public Queue queueCouponIssue() {
        return new Queue(QUEUE_COUPON_ISSUE, true);
    }

    @Bean
    public Binding bindingCouponIssue(Queue queueCouponIssue, DirectExchange exchangeCoupon) {
        return BindingBuilder.bind(queueCouponIssue)
                .to(exchangeCoupon)
                .with(ROUTE_COUPON_ISSUE);
    }

    /*
        Connection & Template
    */
    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setHost(rabbitmqProperties.getHost());
        connectionFactory.setPort(rabbitmqProperties.getPort());
        connectionFactory.setUsername(rabbitmqProperties.getUsername());
        connectionFactory.setPassword(rabbitmqProperties.getPassword());
        return connectionFactory;
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }
}
