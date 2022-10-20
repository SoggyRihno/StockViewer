package com.stockviewer.controllers;

import com.stockviewer.StockViewer;
import com.stockviewer.data.DataManager;
import com.stockviewer.data.Interval;
import com.stockviewer.data.wrappers.StockData;
import com.stockviewer.data.wrappers.StockDataPoint;
import com.stockviewer.exceptions.API.APIException;
import com.stockviewer.exceptions.API.InvalidCallException;
import com.stockviewer.exceptions.API.InvalidKeyException;
import com.stockviewer.exceptions.Poor.InsufficientFundsException;
import com.stockviewer.exceptions.Poor.NoStockException;
import com.stockviewer.exceptions.Poor.PoorException;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class StockPageController {
    private final ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();
    private final String symbol;
    private StockData currentData;

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
    private TextField amountField;
    @FXML
    private HBox chartBox;

    public StockPageController(String symbol) {
        this.symbol = symbol;
    }

    @FXML
    void initialize() {
        for (Interval value : Interval.values())
            graphChoiceBox.getItems().add(value.toString());
        graphChoiceBox.getSelectionModel().select(0);
        graphChoiceBox.setOnAction(actionEvent -> updateChart());
        amountField.textProperty().addListener((observable, oldValue, newValue) -> amountField.setText(newValue.replaceAll("\\D", "")));
        backButton.setOnAction(actionEvent -> back());
        symbolLabel.setText(symbol.toUpperCase());

        try {
            update();
        } catch (Exception e) {
            e.printStackTrace();
            back();
        }
        ses.scheduleWithFixedDelay(() -> Platform.runLater(this::update), 1, 1, TimeUnit.MINUTES);
    }

    void update() {
        LocalDateTime date = LocalDateTime.now().minusDays(1);
        loadData();
        openLabel.setText(String.valueOf(currentData.getLatestOpen()));
        volumeLabel.setText(String.valueOf(currentData.getDailyVolume(date)));
        changeLabel.setText(currentData.getLatestChange());
        changeLabel.setStyle(changeLabel.getText().contains("-") ? "-fx-text-fill: red" : "-fx-text-fill: green");
        dateLabel.setText(currentData.getLatestTimeFormatted());
        updateChart();
    }

    private void loadData() {
        try {
            currentData = StockData.newStockData(symbol);
        } catch (InterruptedException | ExecutionException | APIException e) {
            if (e instanceof InvalidCallException) {
                showError(e.toString());
                back();
            } else if (e instanceof InvalidKeyException) {
                // FIXME: 10/18/2022 api key bad
            } else throw new RuntimeException(e);
        }
    }

    public void back() {
        ses.shutdown();
        try {
            FXMLLoader loader = new FXMLLoader(StockViewer.class.getResource("XML/HomePage.fxml"));
            Stage stage = (Stage) backButton.getScene().getWindow();
            Scene scene = new Scene(loader.load());
            stage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.NONE, message, ButtonType.OK);
        alert.showAndWait();
    }

    public void updateChart() {
        Interval interval = Interval.fromString(graphChoiceBox.getSelectionModel().getSelectedItem());
        try {
            StockData stockData = interval.equals(Interval.ONE_DAY) ? currentData : StockData.newStockData(symbol, interval);

            long minDay = stockData.getData().stream().map(StockDataPoint::getLocalDateTime).mapToLong(i -> Duration.between(LocalDate.now().minusDays(1).atTime(9, 0), i).toDays()).max().orElse(0);
            LocalDateTime date = LocalDate.now().minusDays(interval.equals(Interval.YTD) ? --minDay : interval.getRange()).atTime(9, 0);

            ObservableList<XYChart.Data<String, Number>> data = FXCollections.observableList(stockData.getData().stream()
                    .filter(i -> interval.equals(Interval.YTD) || i.getLocalDateTime().isAfter(date))
                    .map(i -> {
                        String dateFormatted = switch (interval) {
                            case ONE_DAY -> i.getLocalDateTime().format(DateTimeFormatter.ofPattern("HH:mm"));
                            case SEVEN_DAY, THIRTY_DAY ->
                                    i.getLocalDateTime().format(DateTimeFormatter.ofPattern("MMM/dd HH:mm"));
                            case YTD -> i.getLocalDateTime().format(DateTimeFormatter.ofPattern("MMM/dd/yyyy"));
                        };
                        return new XYChart.Data<String, Number>(dateFormatted, i.getClose());
                    }).toList());

            CategoryAxis xAxis = new CategoryAxis();
            xAxis.setLabel("Date");
            xAxis.setGapStartAndEnd(false);


            ObservableList<String> categories = FXCollections.observableList(xAxis.getCategories().stream().map(i -> {
                int hour = Integer.parseInt(i.split(":")[0]);
                hour = hour > 12 ? hour - 12 : hour;
                return hour + ":" + i.split(":")[1];
            }).toList());


//https://stackoverflow.com/questions/14932788/javafx-customzied-categoryaxis-manual-without-useing-autoranging
/*
            xAxis.setAutoRanging(false);
            xAxis.setCategories(categories);
            xAxis.invalidateRange(categories);
 */
            double yMin = data.stream().map(XYChart.Data::getYValue).map(Number::doubleValue).map(Math::floor).min(Double::compareTo).orElse(0.0);
            double yMax = data.stream().map(XYChart.Data::getYValue).map(Number::doubleValue).map(Math::ceil).max(Double::compareTo).orElse(0.0);

            NumberAxis yAxis = new NumberAxis();
            yAxis.setLabel("Price");
            yAxis.setAutoRanging(false);
            yAxis.setLowerBound(yMin);
            yAxis.setUpperBound(yMax);

            LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
            lineChart.getData().add(new XYChart.Series<>(data));
            lineChart.setCreateSymbols(false);
            lineChart.setTitle(String.valueOf(interval));

            chartBox.getChildren().clear();
            chartBox.getChildren().add(lineChart);
        } catch (ExecutionException | InterruptedException | APIException e) {
            throw new RuntimeException(e);
        }
    }

    // FIXME: 10/18/2022
    @FXML
    void buyAction(ActionEvent event) {
        loadData();
        if (!amountField.getText().isEmpty() && Integer.parseInt(amountField.getText()) != 0) {
            try {
                DataManager.buy(Integer.parseInt(amountField.getText()), currentData.getLatestOpen(), symbol);
            } catch (InsufficientFundsException e) {
                showError("You are to poor :(");
            }
        }
    }

    // FIXME: 10/18/2022
    @FXML
    void sellAction(ActionEvent event) {
        update();
        if (!amountField.getText().isEmpty() && Integer.parseInt(amountField.getText()) != 0) {
            try {
                DataManager.sell(Integer.parseInt(amountField.getText()), currentData.getLatestOpen(), symbol);
            } catch (NoStockException e) {
                showError("You don't own enough of this stock");
            } catch (PoorException e) {
                showError("You are to poor");
            }
        }
    }
}