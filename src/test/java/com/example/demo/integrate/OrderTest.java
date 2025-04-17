package com.example.demo.integrate;

import com.example.demo.application.order.OrderCriteria;
import com.example.demo.application.order.OrderFacade;
import com.example.demo.application.order.OrderResult;
import com.example.demo.domain.coupon.CouponCommand;
import com.example.demo.domain.coupon.CouponService;
import com.example.demo.domain.coupon.UserCoupon;
import com.example.demo.domain.coupon.UserCouponRepository;
import com.example.demo.domain.order.*;
import com.example.demo.infra.order.OrderItemRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
class OrderTest {

    @Autowired
    private OrderFacade orderFacade;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private CouponService couponService;

    @Autowired
    private UserCouponRepository userCouponRepository;

    @Test
    @DisplayName("주문 생성 시 Order와 OrderItem이 모두 정상적으로 저장된다")
    void createOrder_정상_생성된다() {
        // given
        Long userId = 1L;
        double discountRate = 10.0;
        long productTotalAmount = 10000;

        List<OrderCommand.OrderProduct> products = List.of(
                OrderCommand.OrderProduct.of(1L, 30L),
                OrderCommand.OrderProduct.of(2L, 10L)
        );

        OrderCommand.CreateOrder command = OrderCommand.CreateOrder.of(
                userId, discountRate, productTotalAmount, products
        );

        // when
        OrderInfo.CreateOrder result = orderService.createOrder(command);

        // then - Order 저장 확인
        Optional<Order> savedOrderOpt = orderRepository.findById(result.getOrderId());
        assertThat(savedOrderOpt).isPresent();

        Order savedOrder = savedOrderOpt.get();
        assertThat(savedOrder.getUserId()).isEqualTo(userId);
        assertThat(savedOrder.getOrderStatus()).isEqualTo(OrderStatus.CREATED);

        double expectedDiscount = 0.01 * discountRate * productTotalAmount;
        double expectedFinalPrice = productTotalAmount - expectedDiscount;

        assertThat(savedOrder.getDiscountPrice()).isEqualTo(expectedDiscount);
        assertThat(savedOrder.getFinalTotalPrice()).isEqualTo(expectedFinalPrice);

        // then - OrderItem 저장 확인
        List<OrderItem> savedItems = orderItemRepository.findByOrderId(savedOrder.getId());
        assertThat(savedItems).hasSize(2);
        assertThat(savedItems).extracting(OrderItem::getProductId)
                .containsExactlyInAnyOrder(1L, 2L);

        // then - 반환값 검증
        assertThat(result.getOrderId()).isEqualTo(savedOrder.getId());
        assertThat(result.getFinalTotalAmount()).isEqualTo(expectedFinalPrice);
    }

    @Test
    @DisplayName("상품이 없으면 예외가 발생한다")
    void createOrder_상품없음_예외() {
        OrderCommand.CreateOrder command = OrderCommand.CreateOrder.of(
                1L, 10.0, 10000L, List.of()
        );

        assertThatThrownBy(() -> orderService.createOrder(command))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("할인율이 음수면 예외가 발생한다")
    void createOrder_할인율_예외() {
        OrderCommand.CreateOrder command = OrderCommand.CreateOrder.of(
                1L, -1.0, 10000L,
                List.of(OrderCommand.OrderProduct.of(1L, 1L))
        );

        assertThatThrownBy(() -> orderService.createOrder(command))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("총액이 음수면 예외가 발생한다")
    void createOrder_총액_예외() {
        OrderCommand.CreateOrder command = OrderCommand.CreateOrder.of(
                1L, 10.0, -5000L,
                List.of(OrderCommand.OrderProduct.of(1L, 1L))
        );

        assertThatThrownBy(() -> orderService.createOrder(command))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("수량이 0이면 예외가 발생한다")
    void createOrder_수량0_예외() {
        OrderCommand.CreateOrder command = OrderCommand.CreateOrder.of(
                1L, 10.0, 10000L,
                List.of(OrderCommand.OrderProduct.of(1L, 0L))
        );

        assertThatThrownBy(() -> orderService.createOrder(command))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("userId가 0이면 예외가 발생한다")
    void createOrder_userId0_예외() {
        OrderCommand.CreateOrder command = OrderCommand.CreateOrder.of(
                0L, 10.0, 10000L,
                List.of(OrderCommand.OrderProduct.of(1L, 1L))
        );

        assertThatThrownBy(() -> orderService.createOrder(command))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("주문 처리 플로우 전체 테스트")
    void testOrderFacadeSuccess() {
        // given
        Long userId = 100L;
        Long couponId = 5L;

        // 1. 쿠폰 발급
        couponService.issue(CouponCommand.Issue.of(userId, couponId));

        // 2. 발급된 쿠폰 PK(userCouponId) 조회
        Long userCouponId = userCouponRepository.findByCouponIdAndUserId(couponId, userId)
                .map(UserCoupon::getCouponId)
                .orElseThrow(() -> new RuntimeException("발급된 쿠폰이 존재하지 않습니다"));

        // 3. 주문 요청 생성
        OrderCriteria.Order criteria = OrderCriteria.Order.of(
                userId,
                couponId,
                List.of(
                        OrderCriteria.OrderProduct.of(1L, 2L),
                        OrderCriteria.OrderProduct.of(2L, 1L)
                ) );



        // when
        OrderResult.Order result = orderFacade.order(criteria);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isNotNull();
        assertThat(result.getTotalPrice()).isGreaterThan(45000L);
    }



}
