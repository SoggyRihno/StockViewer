package com.stockviewer.Data.Graphing;

import com.stockviewer.Data.StockDataPoint;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

public class GraphMaker {
    public static void newGraph(List<StockDataPoint> data, GraphInterval interval, LocalDateTime currentTime) {
        List<StockDataPoint> dataInRange = !interval.equals(GraphInterval.YEAR_TO_DAY)
                ? data
                : data.stream().filter(i -> i.getLocalDateTime().isAfter(currentTime.minusDays(interval.getDays()))).toList();
        double maxPrice = dataInRange.stream().mapToDouble(StockDataPoint::getHigh).max().orElse(10);

        long[] yIntervals = IntStream.range(0,6).mapToLong(i->i*(Math.round(Math.floor(maxPrice / 20) * 100) / 100)).toArray();


    }
}
