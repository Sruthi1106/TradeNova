package com.ttcrypto.entity;

public enum OrderStatus {
    PENDING("pending", "Waiting for execution"),
    PARTIALLY_FILLED("partially_filled", "Order partially filled"),
    FILLED("filled", "Order fully filled"),
    CANCELLED("cancelled", "Order cancelled by user"),
    REJECTED("rejected", "Order rejected by system");

    private final String value;
    private final String description;

    OrderStatus(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }
}
