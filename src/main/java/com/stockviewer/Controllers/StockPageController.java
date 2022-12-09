package com.stockviewer.Controllers;

import com.stockviewer.Exceptions.API.APIException;
import com.stockviewer.Functionality.DataManager;
import com.stockviewer.Functionality.Interval;
import com.stockviewer.Functionality.wrappers.StockData;
import com.stockviewer.Functionality.wrappers.StockDataPoint;
import com.stockviewer.StockViewer;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class StockPageController {
    @FXML
    private Button buyButton;
    @FXML
    private Button sellButton;
    @FXML
    private Button backButton;
    @FXML
    private Label changeLabel;
    @FXML
    private Label dateLabel;
    @FXML
    private ChoiceBox<String> graphChoiceBox;
    @FXML
    private Label openLabel;
    @FXML
    private Label symbolLabel;
    @FXML
    private Label volumeLabel;
    @FXML
    private Text resultText;
    @FXML
    private TextField amountField;
    @FXML
    private VBox chartBox;

    private final ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();
    private final String symbol;
    private long lastUpdated = System.currentTimeMillis();
    private StockData currentData;
    private LineChart<String, Number> lineChart;

    public StockPageController(String symbol) {
        this.symbol = symbol;
    }

    @FXML
    void initialize() {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Date");
        xAxis.setGapStartAndEnd(false);

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Price");
        yAxis.setAutoRanging(false);

        lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setCreateSymbols(false);
        lineChart.setAnimated(false);

        chartBox.getChildren().add(lineChart);
        for (Interval value : Interval.values())
            graphChoiceBox.getItems().add(value.toString());
        graphChoiceBox.getSelectionModel().select(0);
        graphChoiceBox.setOnAction(actionEvent -> updateChart());

        amountField.textProperty().addListener((observable, oldValue, newValue) -> amountField.setText(newValue.replaceAll("\\D", "")));
        backButton.setOnAction(actionEvent -> back());
        buyButton.setOnAction(actionEvent -> buy());
        sellButton.setOnAction(actionEvent -> sell());
        symbolLabel.setText(symbol.toUpperCase());
        if (!update())
            back();
        ses.scheduleWithFixedDelay(this::update, 1, 1, TimeUnit.MINUTES);
    }

    boolean update() {
        try {
            loadData();
            Platform.runLater(() -> {
                openLabel.setText(String.format("Open : %s",currentData.getLatestOpen()));
                volumeLabel.setText(String.format("Volume : %s",currentData.getDailyVolume(LocalDateTime.now().minusDays(1))));
                changeLabel.setText(currentData.getLatestChange());
                dateLabel.setText(currentData.getLatestTimeFormatted());
                changeLabel.setStyle(changeLabel.getText().contains("-") ? "-fx-text-fill: red" : "-fx-text-fill: green");
                updateChart();
            });
        } catch (APIException e) {
            return false;
        }
        return true;
    }

    void loadData() throws APIException {
        try {
            currentData = StockData.newStockData(symbol);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    void back() {
        ses.shutdown();
        try {
            FXMLLoader loader = new FXMLLoader(StockViewer.class.getResource("Pages/HomePage.fxml"));
            StockViewer.getStage().setScene(new Scene(loader.load()));
        } catch (IOException e) {
            DataManager.saveJson();
            System.exit(-1);
        }
    }

    void updateChart() {
        Interval interval = Interval.fromString(graphChoiceBox.getSelectionModel().getSelectedItem());
        try {
            StockData stockData = interval.equals(Interval.ONE_DAY) ? currentData : StockData.newStockData(symbol, interval);
            long minDay = stockData.getData().stream().parallel().map(StockDataPoint::getLocalDateTime).mapToLong(i -> Duration.between(LocalDate.now().minusDays(1).atTime(9, 0), i).toDays()).max().orElse(0);
            LocalDateTime marketOpen = LocalDate.now().minusDays(interval.equals(Interval.YTD) ? --minDay : interval.getRange()).atTime(9, 0).plusDays(1);

            List<XYChart.Data<String, Number>> points = stockData.getData().stream()
                    .parallel()
                    .filter(i -> interval.equals(Interval.YTD) || i.getLocalDateTime().isAfter(marketOpen))
                    .filter(i -> !interval.equals(Interval.YTD) || i.getLocalDateTime().getDayOfMonth() == 1)
                    .map(i -> new XYChart.Data<String, Number>(DataManager.formatByInterval(i.getLocalDateTime(), interval), i.getClose()))
                    .toList();

            lineChart.setTitle(String.valueOf(interval));
            if (!points.isEmpty()) {
                List<Double> yRange = points.stream().parallel().map(XYChart.Data::getYValue).map(Number::doubleValue).sorted().toList();
                ((NumberAxis) lineChart.getYAxis()).setLowerBound(Math.floor(yRange.get(0)));
                ((NumberAxis) lineChart.getYAxis()).setUpperBound(Math.ceil(yRange.get(yRange.size() - 1)));

                if (lineChart.getData() == null)
                    lineChart.setData(FXCollections.observableArrayList());
                lineChart.getData().clear();
                lineChart.getData().add(new XYChart.Series<>());

                XYChart.Series<String, Number> series = lineChart.getData().get(0);
                points.forEach(i->series.getData().add(i));
            }
        } catch (ExecutionException | InterruptedException | APIException e) {
            e.printStackTrace();
        }
    }

    void printResult(String string, String color) {
        lastUpdated = System.currentTimeMillis();
        resultText.setStyle("-fx-text-fill: " + color);
        resultText.setText(string);
        ses.schedule(() -> Platform.runLater(() -> {
            if (System.currentTimeMillis() - lastUpdated >= 900)
                resultText.setText("");
        }), 1, TimeUnit.SECONDS);
    }

    void buy() {
        if (amountField.getText().isEmpty() || Objects.equals(amountField.getText(), "0")) {
            printResult("Amount can't be 0 or empty", "red");
            amountField.setText("");
        } else {
            if (DataManager.buy(Integer.parseInt(amountField.getText()), currentData.getLatestOpen(), symbol)) {
                printResult(String.format("Successfully bought %n%s at %,.2f ", amountField.getText(), currentData.getLatestOpen()), "green");
            } else {
                printResult("You cant afford this L", "red");
            }
        }
    }

    void sell() {
        if (amountField.getText().isEmpty() || Objects.equals(amountField.getText(), "0")) {
            printResult("Amount can't be 0 or empty", "red");
            amountField.setText("");
        }else {
            if (DataManager.sell(Integer.parseInt(amountField.getText()), currentData.getLatestOpen(), symbol)) {
                printResult(String.format("Successfully sold %s at %,.2f ", amountField.getText(), currentData.getLatestOpen()), "green");
            }else {
                printResult("You don't own that many L", "red");
            }
        }
    }
}