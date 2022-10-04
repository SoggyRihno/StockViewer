package com.stockviewer.data;

public class SellOrder extends Order{
    public SellOrder(int amount, double price, String symbol) {
        super(amount, price, symbol);
    }

    @Override
    public String toString() {
        return super.toString() + "\tSOLD";
    }
}
