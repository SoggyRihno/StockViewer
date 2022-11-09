package com.stockviewer.Functionality;

public enum Interval {
    ONE_DAY("1-Day", "&function=TIME_SERIES_INTRADAY&interval=5min", 1),
    SEVEN_DAY("7-Day", "&function=TIME_SERIES_DAILY_ADJUSTED&outputsize=full", 7),
    THIRTY_DAY("30-Day", "&function=TIME_SERIES_DAILY_ADJUSTED&outputsize=compact", 30),
    YTD("YTD", "&function=TIME_SERIES_WEEKLY&outputsize=compact", Integer.MAX_VALUE);

    private final String value;
    private final String apiValue;
    private final int range;

    Interval(String value, String apiValue, int range) {
        this.value = value;
        this.apiValue = apiValue;
        this.range = range;
    }

    public static Interval fromString(String value) {
        return switch (value) {
            case "1-Day" -> ONE_DAY;
            case "7-Day" -> SEVEN_DAY;
            case "30-Day" -> THIRTY_DAY;
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
}
