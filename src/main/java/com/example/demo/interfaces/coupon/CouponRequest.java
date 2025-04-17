package com.example.demo.interfaces.coupon;

import com.example.demo.domain.coupon.CouponCommand;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CouponRequest {

    @Getter
    @NoArgsConstructor
    public static class Issue{
        @NotNull
        @Positive
        private Long userId;

        @NotNull
        @Positive
        private Long couponId;


        public CouponCommand.Issue toCommand(){
            return CouponCommand.Issue.of(userId, couponId);
        }

    }


}
