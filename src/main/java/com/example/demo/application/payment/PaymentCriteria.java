package com.example.demo.application.payment;

import com.example.demo.domain.balance.BalanceCommand;
import com.example.demo.domain.coupon.CouponCommand;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PaymentCriteria {

    @Getter
    public static class Payment {
        private final Long orderId;
        private final Long userId;
        private final Long couponId;

        private Payment(Long orderId, Long userId, Long couponId) {
            this.orderId = orderId;
            this.userId = userId;
            this.couponId = couponId;
        }

        public static Payment of(Long orderId, Long userId, Long couponId) {
            return new Payment(orderId,userId,couponId);
        }

        public BalanceCommand.Use toBalanceUseCommand(Long amount){
            return BalanceCommand.Use.of(userId, amount);
        }

        public CouponCommand.Use toUseCouponCommand() {
            return CouponCommand.Use.of(userId,couponId);
        }



    }

}
