package com.stockviewer.Data;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class Order {
    private final String symbol;
    private final int amount;
    private final double price;
    private final String date;

    private final UUID uuid = UUID.randomUUID();

    public Order(int amount, double buyPrice, String symbol) {
        this.amount = amount;
        this.price = buyPrice;
        this.symbol = symbol;
        this.date = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss").format(LocalDateTime.now());
    }

    public String getSymbol() {
        return symbol;
    }

    public int getAmount() {
        return amount;
    }

    public double getBuyPrice() {
        return price;
    }

    public String getBuyDate() {
        return date;
    }

    public UUID getUuid() {
        return uuid;
    }

    @Override
    public String toString() {
        return symbol.toUpperCase() + "\t" + amount + "\t" + price + "\t" + date.substring(0,10).trim();
    }
}