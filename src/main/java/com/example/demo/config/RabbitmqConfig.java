package com.example.demo.config;

import com.example.demo.support.properties.RabbitmqProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@RequiredArgsConstructor
@Configuration
public class RabbitmqConfig {
    private final RabbitmqProperties rabbitmqProperties;

    /*
        AccessLog
    */
    @Bean
    public DirectExchange exchangeAccessLog() {
        return new DirectExchange("exchange.access.log");
    }

    @Bean
    public Queue queueAccessLogSave() {
        return new Queue("queue.access.log.save", true);
    }

    @Bean
    public Binding bindingMailSend(Queue queueAccessLogSave, DirectExchange exchangeAccessLog) {
        return BindingBuilder.bind(queueAccessLogSave)
                .to(exchangeAccessLog)
                .with("route.access.log.save");
    }
    /*
        PaymentHistory
    */
    @Bean
    public DirectExchange exchangePaymentHistory() {
        return new DirectExchange("exchange.payment.history");
    }

    @Bean
    public Queue queuePaymentHistorySave() {
        return new Queue("queue.payment.history.save", true);
    }

    @Bean
    public Binding bindingPaymentHistorySave(Queue queuePaymentHistorySave, DirectExchange exchangePaymentHistory) {
        return BindingBuilder.bind(queuePaymentHistorySave)
                .to(exchangePaymentHistory)
                .with("route.payment.history.save");
    }

    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setHost(rabbitmqProperties.getHost());
        connectionFactory.setPort(rabbitmqProperties.getPort());
        connectionFactory.setUsername(rabbitmqProperties.getUsername());
        connectionFactory.setPassword(rabbitmqProperties.getPassword());
        return connectionFactory;
    }

    /*
        connection
    */

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
