package com.stockviewer.Functionality.wrappers;

import com.google.gson.JsonObject;
import com.stockviewer.Functionality.DataManager;

import java.time.LocalDateTime;

public class StockDataPoint implements Comparable<StockDataPoint> {

    private final LocalDateTime localDateTime;
    private final double open;
    private final double high;
    private final double low;
    private final double close;
    private final int volume;

    public StockDataPoint(String timeStamp, JsonObject json) {
        this.localDateTime = LocalDateTime.parse(timeStamp, DataManager.getDateTimeFormatter());
        this.open = json.keySet().contains("1. open")     ? json.get("1. open").getAsDouble()   :  0;
        this.high = json.keySet().contains("2. high")     ? json.get("2. high").getAsDouble()   :  0;
        this.low = json.keySet().contains("3. low")       ? json.get("3. low").getAsDouble()    :  0;
        this.close = json.keySet().contains("4. close")   ? json.get("4. close").getAsDouble()  :  0;
        this.volume = json.keySet().contains("5. volume") ? json.get("5. volume").getAsInt()    :  0;
    }

    public LocalDateTime getLocalDateTime() {
        return localDateTime;
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

    @Override
    public int hashCode() {
        int result = 0;
        result += 31 * result + open;
        result += 31 * result + high;
        result += 31 * result + low;
        result += 31 * result + close;
        result += 31 * result + volume;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if(obj instanceof StockDataPoint dp)
            return dp.getVolume() == volume
                   && dp.getOpen() == open
                   && dp.getHigh() == high
                   && dp.getLow() == low
                   && dp.getClose() == close
                   && dp.getLocalDateTime().equals(getLocalDateTime());
        return false;
    }
}
