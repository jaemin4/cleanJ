package com.example.demo.infra.coupon;

import com.example.demo.domain.coupon.CouponCommand;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CouponConsumerCommand {

    @Getter
    public static class Issue{
        private final Long userId;
        private final Long couponId;

        private Issue(Long userId, Long couponId) {
            this.userId = userId;
            this.couponId = couponId;
        }

        public static Issue of(Long userId, Long couponId) {
            return new Issue(userId, couponId);
        }
    }

}
