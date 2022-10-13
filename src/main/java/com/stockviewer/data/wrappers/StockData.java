package com.stockviewer.data.wrappers;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.stockviewer.data.DataManager;
import com.stockviewer.data.Interval;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class StockData {
    private final String symbol;
    private final List<StockDataPoint> data;
    private final Interval interval;

    private StockData(String symbol, Interval interval, List<StockDataPoint> data) {
        this.symbol = symbol;
        this.data = data;
        this.interval = interval;
    }

    public static CompletableFuture<StockData> newStockData(String symbol, Interval interval){
        final LocalDateTime currentTime = LocalDateTime.now().minusDays(1).minusDays(interval.getRange());
        return DataManager.getStockData(symbol, interval)
                .thenApply(JsonParser::parseString)
                .thenApply(JsonElement::getAsJsonObject)
                .thenApply(i -> i.get(i.keySet().stream().filter(j -> !j.equals("Meta Data")).findFirst().orElse("")))
                .thenApply(JsonElement::getAsJsonObject)
                .thenApply(i -> i.keySet().stream()
                        .map(j -> new StockDataPoint(j, i.get(j).getAsJsonObject()))
                        .filter(j -> j.getLocalDateTime().isAfter(currentTime))
                        .sorted(StockDataPoint::compareTo)
                        .toList())
                .thenApply(i -> new StockData(symbol, interval, i));
    }

    public static CompletableFuture<StockData> newStockData(String symbol){
        return newStockData(symbol, Interval.ONE_DAY);
    }

    public String getSymbol() {
        return symbol;
    }

    public List<StockDataPoint> getData() {
        return data;
    }

    public double getLatestOpen() {
        return data.get(data.size() - 1).getOpen();
    }

    public int getDailyVolume(LocalDateTime date) {
        return data.stream()
                .filter(i -> i.getLocalDateTime().getDayOfMonth() == date.getDayOfMonth())
                .mapToInt(StockDataPoint::getVolume)
                .sum();
    }

    public String getLatestTimeFormatted() {
        return !data.isEmpty() ? DataManager.getDateTimeFormatter().format(data.get(data.size() - 1).getLocalDateTime()) : "\\_(.-.)_/";
    }

    public String getLatestChange() {
        double difference = data.get(data.size() - 1).getOpen() - data.get(data.size() - 2).getOpen();
        return String.format("%s (%.2f %%)",
                difference >= 0 ? String.format("+ %,.2f", difference) : String.format("%,.2f", difference),
                difference / data.get(data.size() - 2).getOpen() * 100);
    }
}
