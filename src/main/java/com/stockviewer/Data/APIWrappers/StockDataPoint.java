package com.stockviewer.Data.APIWrappers;

import com.google.gson.JsonObject;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class StockDataPoint implements  Comparable<StockDataPoint>{
    public static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final String timeStamp;
    private final double open;
    private final double high;
    private final double low;
    private final double close;
    private final int volume;

    public StockDataPoint(String timeStamp, JsonObject json) {
        this.timeStamp = timeStamp;
        this.open = json.keySet().contains("1. open") ? json.get("1. open").getAsDouble() : 0;
        this.high = json.keySet().contains("2. high") ? json.get("2. high").getAsDouble() : 0;
        this.low = json.keySet().contains("3. low") ? json.get("3. low").getAsDouble() : 0;
        this.close = json.keySet().contains("4. close") ? json.get("4. close").getAsDouble() : 0;
        this.volume = json.keySet().contains("5. volume") ? json.get("5. volume").getAsInt() : 0;
    }

    public LocalDateTime getLocalDateTime() {
        return LocalDateTime.parse(timeStamp,dateFormatter);
    }

    public double getOpen() {
        return open;
    }

    public double getHigh() {
        return high;
    }

    public double getLow() {
        return low;
    }

    public double getClose() {
        return close;
    }

    public int getVolume() {
        return volume;
    }

    @Override
    public int compareTo(StockDataPoint stockDataPoint) {
        return getLocalDateTime().compareTo(stockDataPoint.getLocalDateTime());
    }
}
