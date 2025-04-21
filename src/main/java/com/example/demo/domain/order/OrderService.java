package com.example.demo.domain.order;

import com.example.demo.infra.order.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.weaver.ast.Or;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    /**
     * 주문 생성
     */
    public OrderInfo.CreateOrder createOrder(OrderCommand.CreateOrder command) {
        if (command.getOrderProducts() == null || command.getOrderProducts().isEmpty()) {
            throw new RuntimeException("상품이 비어 있습니다");
        }

        Order order = Order.create(
                command.getUserId(),
                command.getDiscountRate(),
                command.getProductTotalAmount()
        );
        orderRepository.save(order);
        List<OrderItem> items = command.getOrderProducts().stream()
                .map(ci -> {
                    OrderItem item = OrderItem.of(ci.getProductId(), (long) ci.getQuantity().intValue());
                    item.setOrderId(order.getId());
                    return item;
                })
                .toList();

        orderItemRepository.saveAll(items);

        OrderInfo.CreateOrder result = OrderInfo.CreateOrder.of(
                order.getId(),
                order.getProductTotalPrice()
        );
        return result;
    }

    /**
     * 주문 단건 조회
     */
    public OrderInfo.GetOrder getOrderById(Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("해당 주문이 존재하지 않습니다. orderId=" + orderId));
        return OrderInfo.GetOrder.of(
                order.getId(),
                order.getOrderStatus(),
                order.getProductTotalPrice()
        );
    }

    public OrderInfo.GetOrderItems getOrderItemById(Long orderId) {
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);

        return OrderInfo.GetOrderItems.of(
                items.stream()
                        .map(item -> OrderInfo.OrderProduct.of(item.getProductId(), item.getQuantity()))
                        .collect(Collectors.toList())
        );
    }

    /**
     * 주문 결제 마무리
     */
    public void updateOrderStatus(Long orderId, OrderStatus orderStatus) {
        Order order = orderRepository.findById(orderId).orElseThrow(
                () -> new RuntimeException("해당 주문이 존재하지 않습니다")
        );
        order.updateStatus(orderStatus);

        orderRepository.save(order);
    }


}
