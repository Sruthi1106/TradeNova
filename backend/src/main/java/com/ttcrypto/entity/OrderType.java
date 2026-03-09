package com.ttcrypto.entity;

public enum OrderType {
    MARKET("market"),
    LIMIT("limit");

    private final String value;

    OrderType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
