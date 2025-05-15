package com.example.demo.support.constants;

public class RabbitmqConstant {

    // Exchanges
    public static final String EXCHANGE_ACCESS_LOG = "exchange.access.log";
    public static final String EXCHANGE_PAYMENT_HISTORY = "exchange.payment.history";
    public static final String EXCHANGE_COUPON = "exchange.coupon";
    public static final String DEAD_LETTER_EXCHANGE_PAYMENT = "exchange.payment.dlx"; // 수정됨!

    // Queues
    public static final String QUEUE_ACCESS_LOG_SAVE = "queue.access.log.save";
    public static final String QUEUE_PAYMENT_HISTORY_DB_SAVE = "queue.payment.history.db.save";
    public static final String QUEUE_PAYMENT_HISTORY_REDIS_UPDATE = "queue.payment.history.redis.update";
    public static final String QUEUE_COUPON_ISSUE = "queue.coupon.issue";

    // Routing Keys
    public static final String ROUTE_ACCESS_LOG_SAVE = "route.access.log.save";
    public static final String ROUTE_PAYMENT_HISTORY_DB_SAVE = "route.payment.history.db.save";
    public static final String ROUTE_PAYMENT_HISTORY_REDIS_UPDATE = "route.payment.history.redis.update";
    public static final String ROUTE_COUPON_ISSUE = "route.coupon.issue";
}
