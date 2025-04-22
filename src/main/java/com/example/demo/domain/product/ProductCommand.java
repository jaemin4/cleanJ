package com.example.demo.domain.product;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ProductCommand {

    /**
     * 단순 상품 ID 리스트용
     */
    @Getter
    public static class ProductIds {
        private final List<Long> productIds;

        private ProductIds(List<Long> productIds) {
            this.productIds = productIds;
        }

        public static ProductIds of(List<Long> productIds) {
            return new ProductIds(productIds);
        }
    }

    /**
     * 상품 ID + 수량을 함께 처리할 때 사용
     */
    @Getter
    public static class Products {
        private final List<OrderProduct> products;

        private Products(List<OrderProduct> products) {
            this.products = products;
        }

        public static Products of(List<OrderProduct> products) {
            return new Products(products);
        }

        @Getter
        public static class OrderProduct {
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
}
