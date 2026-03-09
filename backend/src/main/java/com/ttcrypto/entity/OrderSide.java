package com.ttcrypto.entity;

public enum OrderSide {
    BUY("buy"),
    SELL("sell");

    private final String value;

    OrderSide(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
