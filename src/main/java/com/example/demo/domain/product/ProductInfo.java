package com.example.demo.domain.product;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ProductInfo {
    /**
     * 판매 중인 상품 목록 응답
     */
    @Getter
    public static class Products {
        private final List<Product> products;

        private Products(List<Product> products) {
            this.products = products;
        }

        public static Products of(List<Product> products) {
            return new Products(products);
        }
    }

    /**
     * 단일 상품 정보 응답
     */
    @Getter
    public static class Product {
        private final Long productId;
        private final String productName;
        private final Long productPrice;

        private Product(Long productId, String productName, Long productPrice) {
            this.productId = productId;
            this.productName = productName;
            this.productPrice = productPrice;
        }

        public static Product of(Long productId, String productName, Long productPrice) {
            return new Product(productId, productName, productPrice);
        }
    }
}
