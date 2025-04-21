package com.example.demo.infra.payment;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TopOrderResult {
    private Long orderId;
    private Long count;
}
