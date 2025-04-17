package com.example.demo.application.order;

import com.example.demo.domain.coupon.CouponService;
import com.example.demo.domain.order.OrderInfo;
import com.example.demo.domain.order.OrderService;
import com.example.demo.domain.product.ProductCommand;
import com.example.demo.domain.product.ProductInfo;
import com.example.demo.domain.product.ProductService;
import com.example.demo.domain.stock.StockService;
import com.example.demo.support.Utils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderFacade {

    private final OrderService orderService;
    private final StockService stockService;
    private final ProductService productService;
    private final CouponService couponService;

    @Transactional
    public OrderResult.Order order(OrderCriteria.Order criteria) {
        // 1. 재고 유효성 검사
        ProductInfo.Products products = productService.findSellingProductsByIds(criteria.toProductsCommand());

        // 2. 재고 차감
        stockService.deductStock(criteria.toDeductStockCommand());

        // 3. 쿠폰 사용
        if(criteria.getCouponId() != null){
            log.info("쿠폰 사용 시작: command={}", Utils.toJson(criteria.toCouponUseCommand()));
            couponService.use(criteria.toCouponUseCommand());
        }
        // 4. 할인율 조회
        double discountRate = couponService.getDiscountRate(criteria.toGetDiscountRateCommand());

        //5. 상품 총금액
        long productTotalAmount = productService.calculateTotalPrice(
                ProductCommand.Products.of(
                        criteria.getItems().stream()
                                .map(OrderCriteria.OrderProduct::getProductId)
                                .toList()
                )
        );

        // 5. 주문 생성
        OrderInfo.CreateOrder orderInfo = orderService.createOrder(criteria.toCreateOrderCommand(discountRate,productTotalAmount));


        // 6. 결과 반환
        OrderResult.Order result = OrderResult.Order.of(
                orderInfo.getOrderId(),
                orderInfo.getFinalTotalAmount()
        );

        return result;
    }
}
