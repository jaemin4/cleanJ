package com.example.demo.domain.stock;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(staticName = "of")
public class StockInfo {
    private final Long stockId;
    private final int quantity;
}
