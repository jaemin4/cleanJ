package com.example.demo.infra.payment;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ResTopOrderFive {
    private Long orderId;
    private Long count;

    public static ResTopOrderFive of(Long orderId, Long count) {
        return new ResTopOrderFive(orderId, count);
    }
}
