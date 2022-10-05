package com.stockviewer.data;

import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;

public enum Interval {
    ONE_DAY("1-Day", "&function=TIME_SERIES_INTRADAY&interval=5min", 1, ChronoUnit.HOURS,"yyyy-MM-dd HH:mm:ss"),
    SEVEN_DAY("7-Day", "&function=TIME_SERIES_DAILY&outputsize=compact", 7, ChronoUnit.HOURS,"yyyy-MM-dd"),
    THIRTY_DAY("30-Day", "&function=TIME_SERIES_DAILY&outputsize=compact", 30, ChronoUnit.HALF_DAYS,"yyyy-MM-dd"),
    YTD("YTD", "&function=TIME_SERIES_WEEKLY&outputsize=full", -1, ChronoUnit.DAYS,"yyyy-MM-dd");

    private final String value;
    private final String apiValue;
    private final int range;
    private final TemporalUnit unit;
    private final DateTimeFormatter formatter;

    Interval(String value, String apiValue, int range, TemporalUnit unit, String pattern) {
        this.value = value;
        this.apiValue = apiValue;
        this.range = range;
        this.unit = unit;
        this.formatter = DateTimeFormatter.ofPattern(pattern);
    }

    public static Interval fromString(String value) {
        return switch (value.toLowerCase()) {
            case "1-day" -> ONE_DAY;
            case "7-day" -> SEVEN_DAY;
            case "30-day" -> THIRTY_DAY;
            default -> YTD;
        };
    }

    public String getApiValue() {
        return apiValue;
    }

    public int getRange() {
        return range;
    }

    @Override
    public String toString() {
        return value;
    }

    public TemporalUnit getUnit() {
        return unit;
    }

    public DateTimeFormatter getFormatter(){
        return formatter;
    }

}
