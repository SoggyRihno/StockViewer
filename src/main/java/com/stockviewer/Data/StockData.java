package com.stockviewer.Data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.stockviewer.Exceptions.APIException;
import com.stockviewer.StockViewer;

import java.time.LocalDateTime;
import java.util.List;

public class StockData {
    private final long timeStamp = System.currentTimeMillis();
    private final String symbol;
    private List<StockDataPoint> data;
    private static final String timeSeriesRegex = "(Time\\sSeries\\s\\()(5|15|30|60)(min\\))";

    private StockData(String symbol, List<StockDataPoint> data) {
        this.symbol = symbol;
        this.data = data;
    }

    public static StockData newStockData(String symbol) throws APIException {
        try {
            String raw = DataManager.getStockData(StockViewer.getSymbol(), APIInterval.FIVE_MINUTES).get();
            JsonObject json = JsonParser.parseString(raw).getAsJsonObject();
            String series = json.keySet().stream().filter(i -> i.matches(timeSeriesRegex)).findFirst().orElse("");
            JsonObject data = json.get(series).getAsJsonObject();
            final LocalDateTime currentTime = LocalDateTime.now().minusDays(1);
            return new StockData(symbol, data.keySet().stream()
                    .map(i -> new StockDataPoint(i, data.get(i).getAsJsonObject()))
                    .filter(i -> i.getLocalDateTime().isAfter(currentTime))
                    .sorted(StockDataPoint::compareTo)
                    .toList());
        } catch (Exception e) {
            throw new APIException();
        }
    }

    public long getTimeStamp(){
        return timeStamp;
    }

    public String getSymbol() {
        return symbol;
    }

    private List<StockDataPoint> getData(){
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
        return data.size() > 0 ? data.get(data.size() - 1).getLocalDateTime().format(StockDataPoint.dateFormatter) : "\\_(.-.)_/";
    }

    public String getLatestChange() {
        double difference = data.get(data.size() - 1).getOpen() - data.get(data.size() - 2).getOpen();
        return String.format("%s (%.2f %%)",
                difference >= 0 ? String.format("+ %,.2f", difference) : String.format("%,.2f", difference),
                difference / data.get(data.size() - 2).getOpen() * 100);
    }
}
