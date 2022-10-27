package com.stockviewer.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.stockviewer.StockViewer;
import com.stockviewer.exceptions.API.APIException;
import com.stockviewer.exceptions.Poor.InsufficientFundsException;
import com.stockviewer.exceptions.Poor.NoStockException;
import com.stockviewer.exceptions.Poor.PoorException;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DataManager {
    private static final ScheduledExecutorService ses = Executors.newScheduledThreadPool(3);
    private static final String URL_FORMAT_STRING = "https://www.alphavantage.co/query?apikey=%s&datatype=json&symbol=%s%s";
    private static Path ORDER_PATH = Path.of("src/main/resources/com/stockviewer/Data/orders.json");
    private static final Path DATA_PATH = Path.of("src/main/resources/com/stockviewer/Data/data.json");
    private static final List<Runnable> queue = new ArrayList<>();
    private static long rateLimitedUntil = 0;
    private static String API_KEY = "";
    private static List<Order> orders = new ArrayList<>();
    private static Map<String, String> cache = new HashMap<>();
    private static double initial = 100000;
    private static final DateTimeFormatter dateTimeFormatter = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd")
            .optionalStart()
            .appendPattern(" HH:mm:ss")
            .optionalEnd()
            .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
            .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
            .toFormatter();
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final Type mapType = new TypeToken<Map<String, Object>>() {
    }.getType();
    private static final Type listType = new TypeToken<ArrayList<Order>>() {
    }.getType();

    static {
        loadJson();
        ses.scheduleWithFixedDelay(() -> {
            if (!queue.isEmpty()) ses.submit(queue.remove(0));
        }, 5, 12, TimeUnit.SECONDS);
        ses.scheduleWithFixedDelay(DataManager::saveJson, 5, 5, TimeUnit.MINUTES);
        ses.scheduleWithFixedDelay(DataManager::cleanCache, 1, 1, TimeUnit.MINUTES);
    }


    public static void setAPIKey(String apiKey) {
        API_KEY = apiKey;
        saveJson();
    }

    public static double getInitial() {
        return initial;
    }

    public static void clear() throws IOException {
        Files.newInputStream(ORDER_PATH, StandardOpenOption.TRUNCATE_EXISTING);
        loadJson();
        saveJson();
    }

    public static void saveJson() {
        try (FileWriter fw = new FileWriter(DATA_PATH.toFile())) {
            Map<String, Object> data = Map.ofEntries(Map.entry("initial", initial), Map.entry("API_KEY", API_KEY));
            gson.toJson(data, mapType, fw);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try (FileWriter fw = new FileWriter(ORDER_PATH.toFile())) {
            gson.toJson(orders, listType, fw);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void loadJson() {
        try (Reader reader = Files.newBufferedReader(DATA_PATH)) {
            Map<String, Object> data = gson.fromJson(reader, mapType);
            if (data != null) {
                if (data.get("initial") != null) {
                    initial = ((Double) data.getOrDefault("initial", 10_000)).doubleValue();
                } else {
                    initial = 10000;
                }
                if (data.get("API_KEY") != null && data.get("API_KEY") != "") {
                    API_KEY = (String) data.getOrDefault("API_KEY", "");
                } else {
                    StockViewer.forceApiKey();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try (Reader reader = Files.newBufferedReader(ORDER_PATH)) {
            List<Order> fromJson = gson.fromJson(reader, listType);
            if (fromJson != null)
                orders = fromJson;
        } catch (IOException ignored) {
        }
    }

    public static void importFile(Path path) throws IOException {
        byte[] current = Files.readAllBytes(path);
        Reader reader = Files.newBufferedReader(DATA_PATH);
        List<Order> importedData = gson.fromJson(reader, listType);
        if (importedData != null) {
            orders = importedData;
            saveJson();
        }else
            Files.write(path, current);
    }

    public static void stop() {
        saveJson();
        ses.shutdown();
    }

    private static String getSync(String url) throws ExecutionException, InterruptedException {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).get().body();
    }

    public static CompletableFuture<String> getStockData(String symbol, Interval interval) {
        CompletableFuture<String> result = new CompletableFuture<>();
        String url = String.format(URL_FORMAT_STRING, API_KEY, symbol, interval.getApiValue());
        if (cache.containsKey(url)) {
            result.completeAsync(() -> cache.get(url));
        } else {
            Runnable task = () -> {
                try {
                    String raw = getSync(url);
                    cache.put(url, raw);
                    result.complete(raw);
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    throw new RuntimeException(new APIException("Api timed out with url : " + url));
                }
            };
            //time shouldn't change between conditions or else possible negative schedule
            long current = System.nanoTime();
            if (rateLimitedUntil - current > 0) {
                ses.submit(task);
            } else {
                ses.schedule(task, rateLimitedUntil - current, TimeUnit.NANOSECONDS);
            }
            rateLimitedUntil = (long) (current + 1.2E9);
        }
        return result;
    }

    public static void cleanCache() {
        cache = cache.keySet().stream().limit(50).collect(Collectors.toMap(Function.identity(), i -> cache.get(i)));
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
        if (ownedAmount >= amount) {
            if (amount * buyPrice <= calculateCurrent()) {
                orders.add(new SellOrder(amount, buyPrice, symbol));
            } else {
                throw new InsufficientFundsException();
            }
        }else {
            throw new NoStockException();
        }
    }

    public static String formatByInterval(LocalDateTime time, Interval interval){
       return switch (interval) {
            case ONE_DAY -> time.format(DateTimeFormatter.ofPattern("HH:mm"));
            case YTD -> time.format(DateTimeFormatter.ofPattern("MMM/dd/yyyy"));
            default -> time.format(DateTimeFormatter.ofPattern("MMM/dd HH:mm"));
        };
    }

    public static List<Order> getOrders() {
        return orders;
    }

    public static DateTimeFormatter getDateTimeFormatter() {
        return dateTimeFormatter;
    }
}