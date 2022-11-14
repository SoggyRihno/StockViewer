package com.stockviewer.Functionality.wrappers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.stockviewer.Exceptions.API.APIException;
import com.stockviewer.Exceptions.API.InvalidCallException;
import com.stockviewer.Exceptions.API.InvalidKeyException;
import com.stockviewer.Functionality.DataManager;
import com.stockviewer.Functionality.Interval;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class StockData {
    private final String symbol;
    private final List<StockDataPoint> data;

    private StockData(String symbol, List<StockDataPoint> data) {
        this.symbol = symbol;
        this.data = data;
    }

    public static StockData newStockData(String symbol, Interval interval) throws ExecutionException, InterruptedException, APIException {
        LocalDateTime time = LocalDateTime.now().minusDays(1).minusDays(interval.getRange());
        final LocalDateTime startTime = time.minusDays(time.getDayOfWeek().getValue() > 5 ? Math.abs(5 - time.getDayOfWeek().getValue()) : 0);

        String raw = DataManager.getStockData(symbol, interval).get();
        JsonObject json = JsonParser.parseString(raw).getAsJsonObject();

        if (json.keySet().contains("Error Message")) {
            String result = json.get("Error Message").getAsString();
            if (result.contains("apikey is invalid or missing"))
                throw new InvalidKeyException(result);
            else if (result.contains("Invalid API call"))
                throw new InvalidCallException(result);
        }
        String key = json.keySet().stream().filter(i -> !i.equals("Meta Data")).findFirst().orElse(null);
        if (key == null)
            throw new APIException();

        JsonObject data = json.get(key).getAsJsonObject();
        List<StockDataPoint> points = new ArrayList<>();
        for (String s : data.keySet())
            if (startTime.isBefore(LocalDateTime.parse(s, DataManager.getDateTimeFormatter())))
                points.add(new StockDataPoint(s, data.get(s).getAsJsonObject()));
        Collections.sort(points);
        return new StockData(symbol, points);
        /*

        return new StockData(symbol,
                data.keySet().stream()
                        .map(i -> new StockDataPoint(i, data.get(i).getAsJsonObject()))
                        .filter(i -> i.getLocalDateTime().isAfter(startTime))
                        .sorted(StockDataPoint::compareTo)
                        .toList());
         */
    }

    public static StockData newStockData(String symbol) throws APIException, ExecutionException, InterruptedException {
        return newStockData(symbol, Interval.ONE_DAY);
    }

    public String getSymbol() {
        return symbol;
    }

    public List<StockDataPoint> getData() {
        return data;
    }

    public double getLatestOpen() {
        if (data.isEmpty())
            return 0;
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
        if (data.isEmpty())
            return "";
        double difference = data.get(data.size() - 1).getOpen() - data.get(data.size() - 2).getOpen();
        return String.format("%s (%.2f %%)",
                difference >= 0 ? String.format("+ %,.2f", difference) : String.format("%,.2f", difference),
                difference / data.get(data.size() - 2).getOpen() * 100);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result += 31 * result + symbol.hashCode();
        result += 31 * result + data.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj instanceof StockData sd)
            return Objects.equals(sd.getSymbol(), symbol) && data.equals(sd.getData());
        return false;
    }
}