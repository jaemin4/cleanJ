package com.example.demo.interfaces.product;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ProductResponse {

    @Getter
    @NoArgsConstructor
    public static class Product {

        private Long productId;
        private String productName;
        private Long productPrice;

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
