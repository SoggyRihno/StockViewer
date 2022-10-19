package com.stockviewer.data.wrappers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.stockviewer.data.DataManager;
import com.stockviewer.data.Interval;
import com.stockviewer.exceptions.API.APIException;
import com.stockviewer.exceptions.API.InvalidCallException;
import com.stockviewer.exceptions.API.InvalidKeyException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class StockData {
    private final String symbol;
    private final List<StockDataPoint> data;

    private StockData(String symbol, List<StockDataPoint> data) {
        this.symbol = symbol;
        this.data = data;

    }

    public static StockData newStockData(String symbol, Interval interval) throws ExecutionException, InterruptedException, APIException {
        final LocalDateTime currentTime = LocalDateTime.now().minusDays(1).minusDays(interval.getRange());
        String raw = DataManager.getStockData(symbol,interval).get();
        JsonObject json = JsonParser.parseString(raw).getAsJsonObject();

        if(json.keySet().contains("Error Message")){
            String result = json.get("Error Message").getAsString();
            if(result.contains("apikey is invalid or missing"))
                throw new InvalidKeyException(result);
            else if (result.contains("Invalid API call"))
                throw new InvalidCallException(result);
        }
        String key = json.keySet().stream().filter(j -> !j.equals("Meta Data")).findFirst().orElse("");

        if(key == "")
            throw new APIException();

        JsonObject data = json.get(key).getAsJsonObject();
        return new StockData(symbol,
                data.keySet().stream()
                .map(i -> new StockDataPoint(i, data.get(i).getAsJsonObject()))
                .filter(i -> i.getLocalDateTime().isAfter(currentTime))
                .sorted(StockDataPoint::compareTo)
                .toList());
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