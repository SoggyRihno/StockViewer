package com.stockviewer.Functionality;

import java.time.LocalDateTime;
import java.util.UUID;

public class Order {
    private final String symbol;
    private final int amount;
    private final double price;
    private final String date;
    private final boolean sold;
    private final UUID uuid = UUID.randomUUID();

    public Order(int amount, double buyPrice, String symbol, boolean sold) {
        this.amount = amount;
        this.price = buyPrice;
        this.symbol = symbol;
        this.date = DataManager.getDateTimeFormatter().format(LocalDateTime.now());
        this.sold = sold;
    }

    public Order(int amount, double buyPrice, String symbol) {
        this(amount, buyPrice, symbol, false);
    }

    public double getSignedValue(){
        return (sold ? 1 : -1) * amount * price;
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

    public boolean isSold() {
        return sold;
    }

    public UUID getUuid() {
        return uuid;
    }

    @Override
    public int hashCode() {
        int result = 0;
        result += 31 * result + symbol.hashCode();
        result += 31 * result + amount;
        result += 31 * result + price;
        result += 31 * result + date.hashCode();
        result += 31 * result + (sold ? 1 : 0);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj instanceof Order order)
            return order.sold == sold
                   && order.date.equals(date)
                   && order.price == price
                   && order.amount == amount;
        return false;
    }

    @Override
    public String toString() {
        return "Order{" +
                "symbol='" + symbol + '\'' +
                ", amount=" + amount +
                ", price=" + price +
                ", date='" + date + '\'' +
                ", sold=" + sold +
                ", uuid=" + uuid +
                '}';
    }
}