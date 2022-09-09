package com.stockviewer.API;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.net.http.*;


public class APIManager {
    private static List<Runnable> queue = new ArrayList<>();
    private static ExecutorService es = Executors.newFixedThreadPool(3);
    private static ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();
    private static final String hardCodedAPIKeyBcStupid = "48PVUTGUNVYAYHA2";

    static {
        ses.scheduleWithFixedDelay(() -> {
            if (queue.size() > 0)
                es.submit(queue.remove(1));
        }, 0, 12, TimeUnit.SECONDS);
    }
    private static String getSync(String url){
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).build();
        try {
            return  client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).get().body();
        } catch (ExecutionException | InterruptedException e) {
            return "";
        }
    }

    public static CompletableFuture<String> getRateLimited(String url) {
        CompletableFuture<String> result = new CompletableFuture<>();
        Runnable task = () -> result.completeAsync(() -> APIManager.getSync(url));

        if (queue.size() >= 1)
            queue.add(task);
        else
            es.submit(task);
        return result;
    }

    public static CompletableFuture<String> getAsync(String url){
        CompletableFuture<String> result = new CompletableFuture<>();
        es.submit(() -> result.completeAsync(() -> APIManager.getSync(url)));
        return result;
    }

    public static CompletableFuture<String> getStockData(String symbol, Interval interval){
        String url = String.format("https://www.alphavantage.co/query?function=TIME_SERIES_INTRADAY&symbol=%s&interval=%s&apikey=%s&datatype=json",symbol,interval,hardCodedAPIKeyBcStupid);
        return getAsync(url);
    }


}
