package com.example.demo.interfaces.payment;

import com.example.demo.application.payment.PaymentCriteria;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PaymentRequest {

    @Getter
    @NoArgsConstructor
    public static class Payment {

        @NotNull(message = "사용자 ID는 필수입니다.")
        @Positive(message = "사용자 ID는 양수여야 합니다.")
        private Long userId;

        @NotNull(message = "주문 ID는 필수입니다.")
        @Positive(message = "주문 ID는 양수여야 합니다.")
        private Long orderId;

        public PaymentCriteria.Payment toCriteria() {
            return PaymentCriteria.Payment.of(userId, orderId);
        }
    }

}
