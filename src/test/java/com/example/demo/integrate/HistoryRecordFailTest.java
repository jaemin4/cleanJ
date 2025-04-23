package com.example.demo.integrate;

import com.example.demo.application.order.OrderCriteria;
import com.example.demo.application.order.OrderFacade;
import com.example.demo.application.order.OrderResult;
import com.example.demo.application.payment.PaymentCriteria;
import com.example.demo.application.payment.PaymentFacade;
import com.example.demo.domain.balance.Balance;
import com.example.demo.domain.balance.BalanceCommand;
import com.example.demo.domain.balance.BalanceRepository;
import com.example.demo.domain.balance.BalanceService;
import com.example.demo.domain.coupon.*;
import com.example.demo.domain.order.Order;
import com.example.demo.domain.order.OrderRepository;
import com.example.demo.domain.order.OrderStatus;
import com.example.demo.domain.payment.PaymentHistoryRepository;
import com.example.demo.domain.product.Product;
import com.example.demo.domain.product.ProductRepository;
import com.example.demo.domain.product.ProductSellingStatus;
import com.example.demo.domain.stock.Stock;
import com.example.demo.infra.stock.StockJpaRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Import(TestConfig.class)
@SpringBootTest
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class HistoryRecordFailTest {

    @Autowired private OrderFacade orderFacade;
    @Autowired private ProductRepository productRepository;
    @Autowired private StockJpaRepository stockRepository;
    @Autowired private PaymentFacade paymentFacade;
    @Autowired private OrderRepository orderRepository;
    @Autowired private BalanceRepository balanceRepository;
    @Autowired private PaymentHistoryRepository paymentHistoryRepository;
    @Autowired private CouponService couponService;
    @Autowired private CouponRepository couponRepository;
    @Autowired private BalanceService balanceService;

    private Long userId = 100L;
    private Long productId1;
    private Long productId2;

    @BeforeEach
    void setUp() {
        // 1. TODO  상품 등록
        Product p1 = Product.create("딸기케이크", 3000L, ProductSellingStatus.SELLING);
        Product p2 = Product.create("마카롱", 1500L, ProductSellingStatus.SELLING);
        productRepository.saveAll(List.of(p1, p2));
        productId1 = p1.getId();
        productId2 = p2.getId();

        // 2. TODO 재고 등록
        stockRepository.saveAll(List.of(
                new Stock(productId1, 10),
                new Stock(productId2, 10)
        ));
    }

    @Test
    @DisplayName("결제 중 결제이력 저장 실패해도 예외는 전파되지 않고 롤백되지 않는다.")
    void testPaymentHistorySaveFailure_exceptionCatch() {
        // given
        List<OrderCriteria.OrderProduct> items = List.of(
                OrderCriteria.OrderProduct.of(productId1, 2L),
                OrderCriteria.OrderProduct.of(productId2, 3L)
        );
        OrderResult.Order result = orderFacade.order(OrderCriteria.Order.of(userId, items));
        Coupon savedCoupon = couponRepository.save(Coupon.create("test", 10, 10L));
        couponService.issue(CouponCommand.Issue.of(userId, savedCoupon.getId()));
        balanceService.charge(BalanceCommand.Charge.of(userId, 50000L));
        PaymentCriteria.Payment payCriteria = PaymentCriteria.Payment.of(
                result.getOrderId(), userId, savedCoupon.getId()
        );

        // when
        // 예외는 발생 X , 결제내역 저장 강제로 일으키기
        assertDoesNotThrow(() -> {
            paymentFacade.pay(payCriteria);
        });

        // then
        // 주문 상태 PAID
        Order updatedOrder = orderRepository.findById(result.getOrderId()).orElseThrow();
        assertThat(updatedOrder.getOrderStatus()).isEqualTo(OrderStatus.PAID);

        // 잔액도 차감된 상태
        Balance balance = balanceRepository.findByUserId(userId).orElseThrow();
        assertThat(balance.getAmount()).isLessThan(50000L);

        // 결제 이력 저장되지 않음
        assertThat(paymentHistoryRepository.existsByOrderId(result.getOrderId())).isFalse();
    }
}
