package com.example.demo.domain.order;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "order_item")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_item_id")
    private Long id;

    @Setter
    @Column(name = "order_id", nullable = false)
    private Long orderId;  // 외래키만 저장

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Long quantity;

    private OrderItem(Long productId, Long quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("상품 수량은 1 이상이어야 합니다.");
        }
        this.productId = productId;
        this.quantity = quantity;
    }

    /**
     * Factory method for creating an OrderItem
     */
    public static OrderItem of(Long productId, Long quantity) {
        return new OrderItem(productId, quantity);
    }

    /**
     * Convenience method to calculate item total price
     */
    public long calculatePrice(long unitPrice) {
        return unitPrice * this.quantity;
    }
}