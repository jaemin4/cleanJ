package com.example.demo.application.payment;

import com.example.demo.domain.balance.BalanceCommand;
import com.example.demo.domain.payment.PaymentHistoryCommand;
import com.example.demo.infra.payment.PaymentMockRequest;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PaymentCriteria {

    @Getter
    public static class Payment {
        private final Long orderId;
        private final Long userId;

        private Payment(Long orderId, Long userId) {
            this.orderId = orderId;
            this.userId = userId;
        }

        public static Payment of(Long orderId, Long userId) {
            return new Payment(userId, orderId );
        }

        public BalanceCommand.Use toBalanceUseCommand(Long amount){
            return BalanceCommand.Use.of(userId, amount);
        }

        public PaymentMockRequest.Mock toPaymentMockRequest(Long amount) {
            return PaymentMockRequest.Mock.of(orderId,userId,amount);
        }

        public PaymentHistoryCommand.Save toPaymentHistoryCommand(String transactionId, String status, Long amount ) {
            return PaymentHistoryCommand.Save.of(userId,amount,orderId,transactionId,status);
        }



    }

}
