package com.ttcrypto.entity;

public enum TransactionType {
    BUY("buy", "Buy order execution"),
    SELL("sell", "Sell order execution"),
    DEPOSIT("deposit", "Balance deposit"),
    WITHDRAWAL("withdrawal", "Balance withdrawal"),
    TRADING_FEE("trading_fee", "Trading fee charged");

    private final String value;
    private final String description;

    TransactionType(String value, String description) {
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
