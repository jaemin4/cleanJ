package com.example.demo.domain.coupon;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CouponCommand {

    @Getter
    public static class Issue {
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

     @Getter
     public static class Use {
        private final Long userId;
        private final Long userCouponId;

         private Use(Long userId,Long userCouponId) {
             this.userId = userId;
             this.userCouponId = userCouponId;
         }

         public static Use of(Long userId,Long userCouponId) {
             return new Use(userId, userCouponId);
         }
     }

    @Getter
    public static class GetDiscountRate {
        private final Long couponId;

        private GetDiscountRate(Long couponId) {
            this.couponId = couponId;
        }

        public static GetDiscountRate of(Long couponId) {
            return new GetDiscountRate(couponId);
        }
    }


}
