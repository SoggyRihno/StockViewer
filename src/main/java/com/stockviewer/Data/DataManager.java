package com.stockviewer.Data;

import com.google.gson.*;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class DataManager {
    public static List<Order> orders = new ArrayList<>();

    public static double initial = 100000;

    public static ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();

    static {
        ses.scheduleWithFixedDelay(DataManager::saveJson, 5, 5, TimeUnit.MINUTES);
    }

    // load Json
    static {
        File dataFile = new File("src/main/resources/com/stockviewer/Data/data.json");
        try {
            if (!dataFile.exists())
                dataFile.createNewFile();
            if (dataFile.length() < 0)
                saveJson();

            Gson gson = new Gson();
            Reader reader = Files.newBufferedReader(dataFile.toPath());
            Map<String, List<Order>> map = gson.fromJson(reader, Map.class);

            orders = map.getOrDefault("orders", Collections.EMPTY_LIST);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveJson() {
        try {
            FileWriter fw = new FileWriter("src/main/resources/com/stockviewer/Data/data.json");
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Map<String, List<Order>> root = new HashMap<>();
            root.put("current", orders);
            gson.toJson(root, fw);
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<Order> getOrders() {
        return orders;
    }


    public static double calculateCurrent() {
        return initial + orders.stream().mapToDouble(i -> {
            if (i.isSold())
                return i.getSellPrice() * i.getAmount();
            return i.getBuyPrice() * i.getAmount() * -1;
        }).sum();
    }

    public static boolean buy(int amount, double buyPrice, String symbol) {
        if (amount * buyPrice > calculateCurrent())
            return false;
        return orders.add(new Order(amount, buyPrice, symbol));
    }

    public static boolean sell(UUID uuid) {
        return false;
    }

    public static List<Order> getActive() {
        return orders.stream().filter(Order::isSold).collect(Collectors.toList());
    }
}
