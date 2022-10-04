package com.stockviewer.data.wrappers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.stockviewer.data.DataManager;
import com.stockviewer.data.Interval;
import com.stockviewer.exceptions.API.APIException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class StockData {
    private final String symbol;
    private final List<StockDataPoint> data;
    private static final List<String> badSymbols = new ArrayList<>();
    private final Interval interval;
    private StockData(String symbol, Interval interval, List<StockDataPoint> data) {
        this.symbol = symbol;
        this.data = data;
        this.interval = interval;
    }

    public static StockData newStockData(String symbol, Interval interval) throws APIException {

        if (badSymbols.contains(symbol)) throw new APIException();
        try {
            String raw = DataManager.getStockData(symbol, interval).get();
            if (raw == null)
                throw new APIException();
            JsonObject json = JsonParser.parseString(raw).getAsJsonObject();
            String series = json.keySet().stream().filter(i -> i.contains("Time Series")).findFirst().orElse("");
            JsonObject data = json.get(series).getAsJsonObject();
            final LocalDateTime currentTime = LocalDateTime.now().minusDays(1).minusDays(interval.getRange());
            return new StockData(symbol,interval, data.keySet().stream()
                            .map(i -> new StockDataPoint(i, data.get(i).getAsJsonObject(), interval.getFormatter()))
                            .filter(i -> i.getLocalDateTime().isAfter(currentTime))
                            .sorted(StockDataPoint::compareTo)
                            .toList());
        } catch (InterruptedException | ExecutionException e) {
            badSymbols.add(symbol);
            throw new APIException();
        }
    }

    public static StockData newStockData(String symbol) throws APIException {
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
        return !data.isEmpty() ? data.get(data.size() - 1).getLocalDateTime().format(interval.getFormatter()) : "\\_(.-.)_/";
    }

    public String getLatestChange() {
        double difference = data.get(data.size() - 1).getOpen() - data.get(data.size() - 2).getOpen();
        return String.format("%s (%.2f %%)",
                difference >= 0 ? String.format("+ %,.2f", difference) : String.format("%,.2f", difference),
                difference / data.get(data.size() - 2).getOpen() * 100);
    }
}
