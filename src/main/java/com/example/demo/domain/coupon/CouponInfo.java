package com.example.demo.domain.coupon;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CouponInfo {

    @Getter
    public static class GetAllQuantity{
        private final Long couponId;
        private final Long quantity;

        private GetAllQuantity(Long couponId, Long quantity) {
            this.couponId = couponId;
            this.quantity = quantity;
        }

        public static GetAllQuantity of(Long couponId, Long quantity) {
            return new GetAllQuantity(couponId, quantity);
        }
    }

}
