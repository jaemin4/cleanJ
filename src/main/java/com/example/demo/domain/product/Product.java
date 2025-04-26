package com.example.demo.domain.product;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "t1_product")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private long price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductSellingStatus sellStatus;

    private Product(String name, long price, ProductSellingStatus sellStatus) {
        validate(name, price, sellStatus);
        this.name = name;
        this.price = price;
        this.sellStatus = sellStatus;
    }

    public static Product create(String name, long price, ProductSellingStatus sellStatus) {
        return new Product(name, price, sellStatus);
    }

    private static void validate(String name, long price, ProductSellingStatus sellStatus) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("상품 이름은 필수입니다.");
        }

        if (price <= 0) {
            throw new IllegalArgumentException("상품 가격은 0보다 커야 합니다.");
        }

        if (sellStatus == null) {
            throw new IllegalArgumentException("상품 판매 상태는 필수입니다.");
        }
    }
}
