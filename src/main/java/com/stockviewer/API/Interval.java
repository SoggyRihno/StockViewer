package com.stockviewer.API;

public enum Interval {
    ONE_MINUTE("1min" ),
    FIVE_MINUTES("5min"),
    FIFTEEN_MINUTES("15min"),
    THIRTY_MINUTES("30min"),
    SIXTY_MINUTES("60min");

    private final String API_VAlUE;
    Interval(String apiValue){
        API_VAlUE = apiValue;
    }

    @Override
    public String toString() {
        return API_VAlUE;
    }
}
