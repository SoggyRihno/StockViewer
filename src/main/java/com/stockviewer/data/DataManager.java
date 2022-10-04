package com.stockviewer.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.stockviewer.exceptions.Poor.InsufficientFundsException;
import com.stockviewer.exceptions.Poor.NoStockException;
import com.stockviewer.exceptions.Poor.PoorException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DataManager {
    private static final ScheduledExecutorService ses = Executors.newScheduledThreadPool(3);
    private static final String URL_FORMAT_STRING = "https://www.alphavantage.co/query?apikey=%s&datatype=json&symbol=%s%s";
    private static final String FILE_PATH = "src/main/resources/com/stockviewer/Data/data.json";
    private static final List<Runnable> queue = new ArrayList<>();

    private static String API_KEY = "48PVUTGUNVYAYHA2";
    private static List<Order> orders = new ArrayList<>();
    private static Map<String, String> cache = new HashMap<>();
    private static double initial = 100000;

    static {
        loadJson();
        ses.scheduleWithFixedDelay(() -> {
            if (!queue.isEmpty()) ses.submit(queue.remove(0));
        }, 5, 12, TimeUnit.SECONDS);
        ses.scheduleWithFixedDelay(DataManager::saveJson, 5, 5, TimeUnit.MINUTES);
        ses.scheduleWithFixedDelay(DataManager::cleanCache, 1, 1, TimeUnit.MINUTES);
    }

    public static void saveJson() {
        try {
            FileWriter fw = new FileWriter(FILE_PATH);
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(Map.ofEntries(Map.entry("initial", initial), Map.entry("API_KEY", API_KEY), Map.entry("orders", orders)), fw);
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void loadJson() {
        File dataFile = new File(FILE_PATH);
        try {
            if (!dataFile.exists())
                dataFile.createNewFile();
            if (dataFile.length() < 0)
                saveJson();
            Gson gson = new Gson();
            Reader reader = Files.newBufferedReader(dataFile.toPath());
            Map<String, Object> map = gson.fromJson(reader, Map.class);
            initial = (double) map.getOrDefault("initial", 10000.0);
            API_KEY = (String) map.getOrDefault("API_KEY", "default");
            orders = (List<Order>) map.getOrDefault("orders", new ArrayList<>());
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("could not load data :(");
        }
    }

    public static void clear() throws IOException {
        Files.newInputStream(Path.of(FILE_PATH), StandardOpenOption.TRUNCATE_EXISTING);
        loadJson();
        saveJson();
    }

    public static void importFile(File file) throws IOException {
        File dataFile = new File(FILE_PATH);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Reader reader = Files.newBufferedReader(dataFile.toPath());
        Map<String, Object> map = gson.fromJson(reader, Map.class);
        if (map.containsKey("initial") && map.containsKey("API_KEY") && map.containsKey("orders")) {
            FileWriter fw = new FileWriter(file);
            gson.toJson(map, fw);
            fw.close();
            loadJson();
        } else {
            saveJson();
            throw new IOException("Missing arguments");
        }
    }

    public static void stop() {
        saveJson();
        ses.shutdown();
    }

    // FIXME: 9/28/2022
    private static String getSync(String url) {
        System.out.println(url);
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).build();
        try {
            return client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).get().body();
        } catch (ExecutionException | InterruptedException e) {
            return "";
        }
    }

    public static CompletableFuture<String> getStockData(String symbol, Interval interval) {
        CompletableFuture<String> result = new CompletableFuture<>();
        String url = String.format(URL_FORMAT_STRING, API_KEY, symbol, interval.getApiValue());
        if (cache.containsKey(url))
            result.complete(cache.get(symbol));
        else
            queue.add(() -> result.completeAsync(() -> cacheData(url, getSync(url))));
        return result;
    }

    public static String cacheData(String url, String raw) {
        cache.put(url, raw);
        cleanCache();
        return raw;
    }

    public static void cleanCache() {
        cache = cache.keySet().stream().limit(10).collect(Collectors.toMap(Function.identity(), i -> cache.get(i)));
    }

    public static double calculateCurrent() {
        return initial + orders.stream().mapToDouble(i -> i.getBuyPrice() * i.getAmount() * (i instanceof SellOrder ? -1 : 1)).sum();
    }

    public static Map<String, Integer> getOwned() {
        Map<String, Integer> map = new HashMap<>();
        orders.stream()
                .filter(i -> !(i instanceof SellOrder))
                .forEach(i -> map.compute(i.getSymbol(), (k, v) -> map.getOrDefault(i.getSymbol(), 0) + i.getAmount()));
        return map;
    }

    public static void buy(int amount, double buyPrice, String symbol) throws InsufficientFundsException {
        if (amount * buyPrice > calculateCurrent())
            throw new InsufficientFundsException();
        orders.add(new Order(amount, buyPrice, symbol));
    }

    public static void sell(UUID uuid) throws PoorException {
        Order order = orders.stream()
                .filter(i -> i.getUuid().equals(uuid))
                .filter(i -> !(i instanceof SellOrder))
                .findFirst()
                .orElse(null);
        if (order != null)
            sell(order.getAmount(), order.getBuyPrice(), order.getSymbol());
    }

    public static void sell(int amount, double buyPrice, String symbol) throws PoorException {
        int ownedAmount = getOwned().getOrDefault(symbol, 0);
        if (ownedAmount >= amount)
            if (amount * buyPrice <= calculateCurrent())
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