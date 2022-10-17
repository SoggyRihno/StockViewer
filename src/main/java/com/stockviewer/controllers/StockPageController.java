package com.stockviewer.controllers;

import com.stockviewer.StockViewer;
import com.stockviewer.data.DataManager;
import com.stockviewer.data.Interval;
import com.stockviewer.data.wrappers.StockData;
import com.stockviewer.data.wrappers.StockDataPoint;
import com.stockviewer.exceptions.API.APIException;
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
import java.util.Comparator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class StockPageController {
    private final ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();
    //todo ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
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
        graphChoiceBox.setValue(Interval.ONE_DAY.toString());

        //listeners
        graphChoiceBox.getSelectionModel().selectedIndexProperty().addListener(i -> updateChart());
        amountField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                amountField.setText(newValue.replaceAll("\\D", ""));
            }
        });
        backButton.setOnAction(actionEvent -> back());
        ses.scheduleWithFixedDelay(() -> Platform.runLater(this::update), 0, 1, TimeUnit.MINUTES);
    }

    void update() {
        LocalDateTime date = LocalDateTime.now().minusDays(1);
        loadData();
        openLabel.setText(String.valueOf(currentData.getLatestOpen()));
        volumeLabel.setText(String.valueOf(currentData.getDailyVolume(date)));
        changeLabel.setText(currentData.getLatestChange());
        changeLabel.setStyle(changeLabel.getText().contains("-") ? "-fx-text-fill: red" : "-fx-text-fill: green");
        symbolLabel.setText(symbol);
        dateLabel.setText(currentData.getLatestTimeFormatted());
        updateChart();
    }

    private void loadData() {
        try {
            currentData = StockData.newStockData(symbol).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateChart() {
        Interval interval = Interval.fromString(graphChoiceBox.getValue());

        try {
            StockData stockData = StockData.newStockData(symbol, interval).get();
            LocalDateTime date = LocalDate.now().minusDays(interval.getRange()).atTime(9, 0);
            XYChart.Series<Number, Number> series = new XYChart.Series<>();

            ObservableList<XYChart.Data<Number, Number>> data = FXCollections.observableList(stockData.getData().stream()
                    .sorted(Comparator.comparing(StockDataPoint::getLocalDateTime))
                    .filter(i -> interval.equals(Interval.YTD) || i.getLocalDateTime().isAfter(date))
                    .map(i -> {
                        if (interval.equals(Interval.ONE_DAY))
                            return new XYChart.Data<Number,Number>(i.getLocalDateTime().getHour() + 60.0 / i.getLocalDateTime().getMinute(), i.getClose());
                        else
                            return new XYChart.Data<Number,Number>(Duration.between(date, i.getLocalDateTime()).toSeconds() / 86400.0, i.getClose());
                    }).toList());

            NumberAxis xAxis = new NumberAxis();
            xAxis.setLabel(graphChoiceBox.getValue());
            xAxis.setAutoRanging(false);
            xAxis.setTickUnit(1);
            if (interval.equals(Interval.ONE_DAY)) {
                xAxis.setLowerBound(9.5);
                xAxis.setUpperBound(17);
            }else{
                xAxis.setLowerBound(data.get(0).getXValue().doubleValue());
                xAxis.setUpperBound(data.get(data.size()-1).getXValue().doubleValue());
            }

            NumberAxis yAxis = new NumberAxis();
            yAxis.setLabel("Price");
            yAxis.setAutoRanging(false);
            yAxis.setUpperBound(data.stream().map(XYChart.Data::getYValue).map(Number::doubleValue).map(Math::ceil).max(Double::compareTo).orElse(0.0));
            yAxis.setLowerBound(data.stream().map(XYChart.Data::getYValue).map(Number::doubleValue).map(Math::floor).min(Double::compareTo).orElse(0.0));
            yAxis.setTickUnit(.1);
            LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);
            lineChart.getData().add(series);
            lineChart.setCreateSymbols(false);

            chartBox.getChildren().clear();
            chartBox.getChildren().add(lineChart);

        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }

    }

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

    @FXML
    void sellAction(ActionEvent event) throws APIException {
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
}