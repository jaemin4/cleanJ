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
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
//@Transactional
//@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class OrderPaymentTest {

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
    @Autowired
    private UserCouponRepository userCouponRepository;


    @BeforeEach
    void setUp() {
        Product p1 = Product.create("딸기케이크", 3000L, ProductSellingStatus.SELLING);
        Product p2 = Product.create("마카롱", 1500L, ProductSellingStatus.SELLING);
        productRepository.saveAll(List.of(p1, p2));
        productId1 = p1.getId();
        productId2 = p2.getId();

        stockRepository.saveAll(List.of(
                new Stock(productId1, 10),
                new Stock(productId2, 10)
        ));
    }

    @Test
    @DisplayName("OrderFacade 주문 통합 테스트")
    void orderFacade_success() {
        // given
        List<OrderCriteria.OrderProduct> items = List.of(
                OrderCriteria.OrderProduct.of(productId1, 2L),  // 6000
                OrderCriteria.OrderProduct.of(productId2, 3L)   // 4500
        );
        OrderCriteria.Order criteria = OrderCriteria.Order.of(userId, items);

        // when
        OrderResult.Order result = orderFacade.order(criteria);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isNotNull();
        assertThat(result.getTotalPrice()).isEqualTo(10500L);
        assertThat(result.getItems()).hasSize(2);
    }

    @Test
    @DisplayName("정상 결제 처리 테스트")
    public void testPaymentSuccess() throws InterruptedException {
        // given  10,500
        List<OrderCriteria.OrderProduct> items = List.of(
                OrderCriteria.OrderProduct.of(productId1, 2L),
                OrderCriteria.OrderProduct.of(productId2, 3L)
        );
        OrderCriteria.Order criteria = OrderCriteria.Order.of(userId, items);
        Coupon savedCoupon = couponRepository.save(Coupon.create("test",10,10L));
        couponService.issue(CouponCommand.Issue.of(userId, savedCoupon.getId()));
        long userCouponId = userCouponRepository.findByCouponId(savedCoupon.getId()).get().getId();
        balanceService.charge(BalanceCommand.Charge.of(userId,50000L));
        OrderResult.Order result = orderFacade.order(criteria);
        PaymentCriteria.Payment criteriaPay = PaymentCriteria.Payment.of(
                result.getOrderId()
                ,userId
                ,userCouponId
        );

        // when
        paymentFacade.pay(criteriaPay);
        Thread.sleep(5_000);


        // then
        Order updatedOrder = orderRepository.findById(result.getOrderId()).orElseThrow();
        assertEquals(OrderStatus.PAID, updatedOrder.getOrderStatus());
        Balance balance = balanceRepository.findByUserId(userId).orElseThrow();
        assertEquals(50000 - 10500 * 0.9, (long) balance.getAmount());
        assertTrue(paymentHistoryRepository.existsByOrderId(result.getOrderId()));
    }


    @Test
    @DisplayName("결제 중 실패했을 때 복구가 정상적으로 된다.")
    void testPaymentFailure_Recovery() {
        // given
        List<OrderCriteria.OrderProduct> items = List.of(
                OrderCriteria.OrderProduct.of(productId1, 2L),
                OrderCriteria.OrderProduct.of(productId2, 3L)
        );
        OrderResult.Order result = orderFacade.order(OrderCriteria.Order.of(userId, items));
        Long invalidCouponId = 9999L;
        PaymentCriteria.Payment payCriteria = PaymentCriteria.Payment.of(
                result.getOrderId(), userId, invalidCouponId
        );

        // when
        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            paymentFacade.pay(payCriteria);
        });

        // then
        assertThat(ex.getMessage()).isEqualTo("결제 처리 중 예외 발생");

        Order updatedOrder = orderRepository.findById(result.getOrderId()).orElseThrow();
        assertThat(updatedOrder.getOrderStatus()).isEqualTo(OrderStatus.CANCELED);

        Stock stock1 = stockRepository.findByProductId(productId1).get();
        Stock stock2 = stockRepository.findByProductId(productId2).get();
        assertThat(stock1.getQuantity()).isEqualTo(10);
        assertThat(stock2.getQuantity()).isEqualTo(10);

        assertThat(paymentHistoryRepository.existsByOrderId(result.getOrderId())).isFalse();
    }
    @Test
    void test_2(){
        balanceRepository.save(Balance.create(1L, 10000000L));
    }

    @DisplayName("결제 API가 실패했을때 주문이 복구되고 결제도 복구되어야 한다")
    @Test
    void test_1() {
        // given
        Long userId = 1001L;
        long initialBalance = 100000000L;
        int initialStock = 5;
        long initialCouponQty = 10L;

        balanceRepository.save(Balance.create(userId, initialBalance));
        Product product = productRepository.save(Product.create("타르트", 2000L, ProductSellingStatus.SELLING));
        stockRepository.save(Stock.create(product.getId(), initialStock));
        Coupon coupon = couponRepository.save(Coupon.create("테스트", 20, initialCouponQty));

        OrderResult.Order orderResult = orderFacade.order(OrderCriteria.Order.of(
                userId,
                List.of(OrderCriteria.OrderProduct.of(product.getId(), 1L))
        ));

        assertThrows(RuntimeException.class, () -> {
            paymentFacade.pay(PaymentCriteria.Payment.of(
                    orderResult.getOrderId(), userId, 3L
            ));
        });

        await().atMost(3, TimeUnit.SECONDS).untilAsserted(() -> {
            Order order = orderRepository.findById(orderResult.getOrderId()).orElseThrow();
            assertEquals(OrderStatus.CANCELED, order.getOrderStatus());

            Stock stock = stockRepository.findByProductId(product.getId()).orElseThrow();
            assertEquals(initialStock, stock.getQuantity());

            Balance balance = balanceRepository.findByUserId(userId).orElseThrow();
            assertEquals(initialBalance, balance.getAmount());

            Coupon couponResult = couponRepository.findById(coupon.getId()).orElseThrow();
            assertEquals(initialCouponQty, couponResult.getQuantity());
        });
    }
}
