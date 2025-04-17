package com.example.demo.domain.order;

import com.example.demo.infra.order.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;

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
                    OrderItem item = OrderItem.of(ci.getProductId(), ci.getQuantity().intValue());
                    item.setOrderId(order.getId());
                    return item;
                })
                .toList();

        orderItemRepository.saveAll(items);

        OrderInfo.CreateOrder result = OrderInfo.CreateOrder.of(
                order.getId(),
                order.getFinalTotalPrice()
        );
        return result;
    }

    /**
     * 주문 단건 조회
     */
    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("해당 주문이 존재하지 않습니다. orderId=" + orderId));
    }

    /**
     * 주문 마무리
     */
    public void completeOrder(Long orderId) {
        Order order = getOrderById(orderId);
        order.updateStatus(OrderStatus.PAID);
        orderRepository.save(order);
    }
}
