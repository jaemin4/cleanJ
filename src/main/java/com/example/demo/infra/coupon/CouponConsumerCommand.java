package com.example.demo.infra.coupon;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CouponConsumerCommand {

    @Getter
    @NoArgsConstructor
    public static class Issue{
        private Long userId;
        private Long couponId;

        private Issue(Long userId, Long couponId) {
            this.userId = userId;
            this.couponId = couponId;
        }

        public static Issue of(Long userId, Long couponId) {
            return new Issue(userId, couponId);
        }
    }

}
