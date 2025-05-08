package com.example.demo.domain.payment;

import com.example.demo.infra.payment.ResTopOrderFive;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PaymentHistoryInfo {

    @Getter
    public static class Top5Orders {
        private final Long orderId;
        private final Long count;

        public Top5Orders(Long orderId, Long count) {
            this.orderId = orderId;
            this.count = count;
        }

        public static Top5Orders of(Long orderId, Long count) {
            return new Top5Orders(orderId, count);
        }

        public static List<Top5Orders> fromResList(List<ResTopOrderFive> list) {
            return list.stream()
                    .map(item -> Top5Orders.of(item.getOrderId(), item.getCount()))
                    .toList();
        }

    }

    @Getter
    public static class Top5OrdersForCaching {
        private final Long orderId;
        private final Long count;

        public Top5OrdersForCaching(Long orderId, Long count) {
            this.orderId = orderId;
            this.count = count;
        }

        public static Top5OrdersForCaching of(Long orderId, Long count) {
            return new Top5OrdersForCaching(orderId, count);
        }

        public static List<Top5OrdersForCaching> fromResList(List<ResTopOrderFive> list) {
            return list.stream()
                    .map(item -> Top5OrdersForCaching.of(item.getOrderId(), item.getCount()))
                    .toList();
        }

        public static List<Top5OrdersForCaching> fromResTopList(List<ResTopOrderFive> list) {
            if (list == null || list.isEmpty()) {
                return List.of();
            }

            return list.stream()
                    .map(res -> new Top5OrdersForCaching(
                            res.getOrderId(),
                            res.getCount()
                    ))
                    .collect(Collectors.toList());
        }

    }


}
