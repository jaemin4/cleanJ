package com.example.demo.interfaces.order;

import com.example.demo.application.order.OrderCriteria;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;


@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class OrderRequest {

    @Getter
    @NoArgsConstructor
    public static class Order{
        @NotNull
        @Positive
        private Long userId;

        private Long couponId;

        @Valid
        @NotEmpty
        private List<OrderProduct> items;

        public OrderCriteria.Order toCriteria() {
            List<OrderCriteria.OrderProduct> products = items.stream()
                    .map(i -> OrderCriteria.OrderProduct.of(
                            i.getProductId(),
                            i.getQuantity()
                    ))
                    .toList();

            return OrderCriteria.Order.of(userId, couponId, products);
        }

    }

    @Getter
    public static class OrderProduct{
        private final Long productId;
        private final Long quantity;

        private OrderProduct(Long productId, Long quantity) {
            this.productId = productId;
            this.quantity = quantity;
        }


    }


}
