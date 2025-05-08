package com.example.demo.interfaces.payment;

import com.example.demo.domain.payment.PaymentHistoryInfo;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PaymentResponse {

    @Getter
    public static class Top5Orders {
        private final Long orderId;
        private final Long count;

        private Top5Orders(Long orderId, Long count) {
            this.orderId = orderId;
            this.count = count;
        }

        public static List<Top5Orders> toResponseList(List<PaymentHistoryInfo.Top5Orders> list) {
            return list.stream()
                    .sorted((a, b) -> Long.compare(b.getCount(), a.getCount()))
                    .map(item -> new Top5Orders(item.getOrderId(), item.getCount()))
                    .collect(Collectors.toList());
        }
    }

    @Getter
    static class Top5OrdersCaching{
        private final Long orderId;
        private final Long count;

        private Top5OrdersCaching(Long orderId, Long count) {
            this.orderId = orderId;
            this.count = count;
        }

        public static List<Top5OrdersCaching> toResponseList(List<PaymentHistoryInfo.Top5OrdersForCaching> list) {
            return list.stream()
                    .sorted((a, b) -> Long.compare(b.getCount(), a.getCount()))
                    .map(item -> new Top5OrdersCaching(item.getOrderId(), item.getCount()))
                    .collect(Collectors.toList());
        }
    }





}
