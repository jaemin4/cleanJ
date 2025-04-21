package com.example.demo.domain.coupon;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "t1_coupon")
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "coupon_id")
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private int discountRate;

    @Column(nullable = false)
    private Long quantity;

    private Coupon(String title, int discountRate, Long quantity) {
        this.title = title;
        this.discountRate = discountRate;
        this.quantity = quantity;
    }

    public static Coupon create(String title, int discountRate, Long quantity) {
        return new Coupon(title, discountRate, quantity);
    }

    // 수량 감소 메서드
    public void use() {
        if (quantity <= 0) {
            throw new IllegalStateException("쿠폰 수량이 부족합니다.");
        }
        this.quantity--;
    }

}
