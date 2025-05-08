package com.example.demo.application.order;

import com.example.demo.domain.order.OrderInfo;
import com.example.demo.domain.order.OrderService;
import com.example.demo.domain.product.ProductCommand;
import com.example.demo.domain.product.ProductService;
import com.example.demo.domain.stock.StockService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class OrderTransaction {

    private final ProductService productService;
    private final StockService stockService;
    private final OrderService orderService;

    @Transactional
    public OrderResult.Order createOrderWithTransaction(OrderCriteria.Order criteria) {
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
