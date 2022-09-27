package com.stockviewer.Data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.stockviewer.Exceptions.Poor.InsufficientFundsException;
import com.stockviewer.Exceptions.Poor.NoStockException;
import com.stockviewer.Exceptions.Poor.PoorException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DataManager {
    private static final ScheduledExecutorService ses = Executors.newScheduledThreadPool(3);
    public static List<Order> orders = new ArrayList<>();
    private static String API_KEY = "48PVUTGUNVYAYHA2";
    private static final String urlFormatString = "https://www.alphavantage.co/query?function=TIME_SERIES_INTRADAY&symbol=%s&interval=%s&apikey=%s&datatype=json";
    private static final List<Runnable> queue = new ArrayList<>();
    private static Map<String, String> cache = new HashMap<>();
    public static double initial = 100000;

    static {
        ses.scheduleWithFixedDelay(() -> {
            if (queue.size() > 0) ses.submit(queue.remove(1));
        }, 0, 12, TimeUnit.SECONDS);
        ses.scheduleWithFixedDelay(DataManager::saveJson, 5, 5, TimeUnit.MINUTES);
        ses.scheduleWithFixedDelay(DataManager::cleanCache, 1, 1, TimeUnit.MINUTES);
        loadJson();
    }

    public static void saveJson() {
        try {
            FileWriter fw = new FileWriter("src/main/resources/com/stockviewer/Data/data.json");
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Map<String, Object> root = new HashMap<>();
            root.put("API_KEY", API_KEY);
            root.put("current", orders);
            gson.toJson(root, fw);
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void loadJson() {
        File dataFile = new File("src/main/resources/com/stockviewer/Data/data.json");
        try {
            if (!dataFile.exists())
                dataFile.createNewFile();
            if (dataFile.length() < 0)
                saveJson();
            Gson gson = new Gson();
            Reader reader = Files.newBufferedReader(dataFile.toPath());
            Map<String, Object> map = gson.fromJson(reader, Map.class);
            orders = (List<Order>) map.getOrDefault("orders", new ArrayList<>());
            API_KEY = (String) map.getOrDefault("API_KEY", "default");
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("could not load data :(");
        }
    }

    public static void stop() {
        saveJson();
        ses.shutdown();
    }

    private static String getSync(String url) {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).build();
        try {
            return client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).get().body();
        } catch (ExecutionException | InterruptedException e) {
            return "";
        }
    }

    public static CompletableFuture<String> getStockData(String symbol, APIInterval APIInterval) {
        CompletableFuture<String> result = new CompletableFuture<>();
        if (cache.containsKey(symbol))
            result.complete(cache.get(symbol));
        else {
            Runnable task = () -> result.completeAsync(() -> cacheData(symbol, getSync(String.format(urlFormatString, symbol, APIInterval, API_KEY))));
            if (queue.size() >= 1)
                queue.add(task);
            else
                ses.submit(task);
        }
        return result;
    }

    public static String cacheData(String symbol, String raw) {
        cache.put(symbol, raw);
        cleanCache();
        return raw;
    }

    public static void cleanCache() {
        cache = cache.keySet().stream().limit(10).collect(Collectors.toMap(Function.identity(), i -> cache.get(i)));
    }

    public static double calculateCurrent() {
        return initial + orders.stream().mapToDouble(i -> i.getBuyPrice() * i.getAmount() * (i instanceof SellOrder ? -1 : 1)).sum();
    }

    public static Map<String, Integer> getOwned(){
        Map<String, Integer> map = new HashMap<>();
        orders.stream()
                .map(Order::getSymbol)
                .forEach(i->{
                    map.compute(i,(k,v)->{
                        v = map.getOrDefault(i,0)+1;
                        return v;
                    });
                });
        return map;
    }

    public static void buy(int amount, double buyPrice, String symbol) throws InsufficientFundsException{
        if (amount * buyPrice > calculateCurrent())
            throw new InsufficientFundsException();
        orders.add(new Order(amount, buyPrice, symbol));
    }


    public static void sell(UUID uuid, double price) throws PoorException {
        Order order = orders.stream()
                .filter(i->i.getUuid().equals(uuid))
                .filter(i->! (i instanceof SellOrder))
                .findFirst()
                .orElse(null);
        if(order !=null)
            sell(order.getAmount(),order.getBuyPrice(),order.getSymbol());


    }
    public static void sell(int amount, double buyPrice, String symbol) throws PoorException {
        int ownedAmount = getOwned().getOrDefault(symbol,0);
        if ( ownedAmount>= amount)
            if(amount * buyPrice<= calculateCurrent())
                orders.add(new SellOrder(amount, buyPrice, symbol));
            else
                throw new InsufficientFundsException();
        else
            throw new NoStockException();
    }

    public static List<Order> getOrders() {
        return orders;
    }
}