package com.example.demo.domain.balance;

import lombok.Getter;

@Getter
public enum BalanceHistoryType {

    CHARGE("충전"),
    USE("사용");

    private final String description;

    BalanceHistoryType(String description) {
        this.description = description;
    }

}