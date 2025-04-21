package com.example.demo.domain.order;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "t_order")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false)
    private OrderStatus orderStatus;

    @Column(name = "product_total_price", nullable = false)
    private long productTotalPrice;

    private Order(Long userId, OrderStatus orderStatus, long productTotalPrice) {
        this.userId = userId;
        this.orderStatus = orderStatus;
        this.productTotalPrice = productTotalPrice;
    }

    public void updateStatus(OrderStatus orderStatus) {
        this.orderStatus = orderStatus;
    }

    public static Order create(Long userId, long productTotalPrice) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId는 1 이상이어야 합니다.");
        }

        if (productTotalPrice < 0) {
            throw new IllegalArgumentException("상품 총액은 0 이상이어야 합니다.");
        }

        OrderStatus status = OrderStatus.CREATED;
        return new Order(userId, status, productTotalPrice);
    }

}
