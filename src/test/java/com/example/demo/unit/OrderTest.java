package com.example.demo.unit;

import com.example.demo.domain.order.*;
import com.example.demo.infra.order.OrderItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
public class OrderTest {

    @InjectMocks
    private OrderService orderService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("주문 생성 성공")
    void createOrder_success() {
        // given
        Long userId = 1L;
        Long totalPrice = 10000L;
        List<OrderCommand.OrderProduct> products = List.of(
                OrderCommand.OrderProduct.of(10L, 2L),
                OrderCommand.OrderProduct.of(20L, 1L)
        );
        OrderCommand.CreateOrder command = OrderCommand.CreateOrder.of(userId, totalPrice, products);

        Order mockOrder = Order.create(userId, totalPrice);
        setId(mockOrder, 1L);
        when(orderRepository.save(any())).thenReturn(mockOrder);

        // when
        OrderInfo.CreateOrder result = orderService.createOrder(command);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getProductTotalPrice()).isEqualTo((long)totalPrice);
        verify(orderRepository).save(any());
        verify(orderItemRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("상품 없이 주문하면 예외 발생")
    void createOrder_fail_when_no_products() {
        OrderCommand.CreateOrder command = OrderCommand.CreateOrder.of(1L, 10000L, List.of());
        assertThatThrownBy(() -> orderService.createOrder(command))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("상품이 비어 있습니다");
    }

    @Test
    @DisplayName("주문 단건 조회 성공")
    void getOrderById_success() {
        Order order = Order.create(1L, 5000L);
        setId(order, 1L);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        OrderInfo.GetOrder result = orderService.getOrderById(1L);

        assertThat(result.getOrderId()).isEqualTo(1L);
        assertThat(result.getProductTotalPrice()).isEqualTo(5000L);
    }

    @Test
    @DisplayName("없는 주문 조회 시 예외 발생")
    void getOrderById_fail_not_found() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("해당 주문이 존재하지 않습니다");
    }

    @Test
    @DisplayName("주문 아이템 목록 조회 성공")
    void getOrderItemByOrderId_success() {
        List<OrderItem> items = List.of(
                OrderItem.of(1L, 2L),
                OrderItem.of(2L, 1L)
        );
        when(orderItemRepository.findByOrderId(1L)).thenReturn(items);

        OrderInfo.GetOrderItems result = orderService.getOrderItemByOrderId(1L);
        assertThat(result.getItems()).hasSize(2);
    }

    @Test
    @DisplayName("주문 상태 업데이트 성공")
    void updateOrderStatus_success() {
        Order order = Order.create(1L, 10000L);
        setId(order, 1L);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        orderService.updateOrderStatus(1L, OrderStatus.PAID);

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PAID);
        verify(orderRepository).save(order);
    }

    private void setId(Order order, Long id) {
        try {
            var field = Order.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(order, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
