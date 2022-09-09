package com.stockviewer.Data;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class Order {
    private final String symbol;
    private final int amount;
    private final double buyPrice;
    private double sellPrice;
    private final String buyDate;
    private String sellDate;
    private boolean sold = false;

    private final UUID uuid = UUID.randomUUID();

    public Order(int amount, double buyPrice, String symbol) {
        this.amount = amount;
        this.buyPrice = buyPrice;
        this.symbol = symbol;
        this.buyDate = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss").format(LocalDateTime.now());
    }

    public void sell(double price) {
        sellPrice = price;
        sellDate = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss").format(LocalDateTime.now());
        sold = true;
    }

    public String getSymbol() {
        return symbol;
    }

    public int getAmount() {
        return amount;
    }

    public double getBuyPrice() {
        return buyPrice;
    }

    public double getSellPrice() {
        return sellPrice;
    }

    public String getBuyDate() {
        return buyDate;
    }

    public String getSellDate() {
        return sellDate;
    }

    public boolean isSold() {
        return sold;
    }

    public UUID getUuid() {
        return uuid;
    }
}
