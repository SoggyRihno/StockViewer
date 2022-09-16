package com.stockviewer.Data.Graphing;

public enum GraphInterval {
    DAY(1),
    WEEK(7),
    MONTH(30),
    YEAR_TO_DAY(-1);

    private int days;

    GraphInterval(int days){
        this.days = days;
    }

    public int getDays(){
        return days;
    }
}
