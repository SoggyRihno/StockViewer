package com.stockviewer.Data;

public enum APIInterval {
    ONE_MINUTE("1min" , 1000),
    FIVE_MINUTES("5min",5000),
    FIFTEEN_MINUTES("15min",15000),
    THIRTY_MINUTES("30min",30000),
    SIXTY_MINUTES("60min",60000);

    private final String API_VAlUE;
    private final int TIME_IN_MIlI;
    APIInterval(String apiValue, int timeInMil){
        API_VAlUE = apiValue;
        TIME_IN_MIlI = timeInMil;
    }

    @Override
    public String toString() {
        return API_VAlUE;
    }

    public int getTimeInMilli(){
        return this.TIME_IN_MIlI;
    }
}
