package com.stockviewer.data;

import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;

public enum Interval {
    ONE_DAY("1-Day", "&function=TIME_SERIES_INTRADAY&interval=5min", 1, ChronoUnit.HOURS),
    SEVEN_DAY("7-Day", "&function=TIME_SERIES_DAILY&outputsize=compact", 7, ChronoUnit.HOURS),
    THIRTY_DAY("30-Day", "&function=TIME_SERIES_DAILY&outputsize=compact", 30, ChronoUnit.HALF_DAYS),
    YTD("YTD", "&function=TIME_SERIES_WEEKLY&outputsize=full", -1, ChronoUnit.DAYS);

    private final String value;
    private final String apiValue;
    private final int range;
    private final TemporalUnit unit;

    Interval(String value, String apiValue, int range, TemporalUnit unit) {
        this.value = value;
        this.apiValue = apiValue;
        this.range = range;
        this.unit = unit;
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
}
