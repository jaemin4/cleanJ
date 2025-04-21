package com.example.demo.domain.stock;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class StockCommand {
    @Getter
    public static class DeductStock {
        private final List<OrderProduct> products;

        private DeductStock(List<OrderProduct> products) {
            this.products = products;
        }

        public static DeductStock of(List<OrderProduct> products) {
            return new DeductStock(products);
        }

    }

    @Getter
    public static class RecoveryStock {
        private final List<OrderProduct> products;

        private RecoveryStock(List<OrderProduct> products) {
            this.products = products;
        }

        public static RecoveryStock of(List<OrderProduct> products) {
            return new RecoveryStock(products);
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

        public static OrderProduct of(Long productId, Long quantity) {
            return new OrderProduct(productId, quantity);
        }
    }
}
