package com.example.demo.application.order;

import com.example.demo.domain.order.OrderInfo;
import com.example.demo.domain.order.OrderService;
import com.example.demo.domain.product.ProductCommand;
import com.example.demo.domain.product.ProductService;
import com.example.demo.domain.stock.StockService;
import com.example.demo.support.comm.aop.DistributedLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderFacade {

    private final ProductService productService;
    private final StockService stockService;
    private final OrderService orderService;

    @DistributedLock(key = "'order:user:' + #criteria.userId", waitTime = 3, leaseTime = 5)
    @Transactional
    public OrderResult.Order order(OrderCriteria.Order criteria) {
        productService.findSellingProductsByIds(criteria.toProductsCommand());

        stockService.deductStock(criteria.toDeductStockCommand());

        long productTotalAmount = productService.calculateTotalPrice(
                ProductCommand.Products.of(
                        criteria.getItems().stream()
                                .map(item -> ProductCommand.Products.OrderProduct.of(item.getProductId(), item.getQuantity()))
                                .toList()
                )
        );

        OrderInfo.CreateOrder orderInfo = orderService.createOrder(criteria.toCreateOrderCommand(productTotalAmount));

        return OrderResult.Order.of(
                orderInfo.getOrderId(),
                orderInfo.getProductTotalPrice(),
                criteria.getItems().stream()
                        .map(item -> OrderResult.OrderProduct.of(item.getProductId(), item.getQuantity()))
                        .collect(Collectors.toList())
        );
    }




}
