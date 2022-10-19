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
import java.util.List;
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
        graphChoiceBox.getSelectionModel().selectedIndexProperty().addListener(i -> updateChart());
        amountField.textProperty().addListener((observable, oldValue, newValue) -> amountField.setText(newValue.replaceAll("\\D", "")));
        backButton.setOnAction(actionEvent -> back());

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
        symbolLabel.setText(symbol);
        dateLabel.setText(currentData.getLatestTimeFormatted());
        updateChart();
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

    public void updateChart() {
        Interval interval = Interval.fromString(graphChoiceBox.getValue());
        try {
            StockData stockData = interval.equals(Interval.ONE_DAY) ? currentData : StockData.newStockData(symbol, interval);
            LocalDateTime date = LocalDate.now().minusDays(interval.getRange()).atTime(9, 0);


            List<XYChart.Data<Number, Number>> list = stockData.getData().stream()
                    .sorted(Comparator.comparing(StockDataPoint::getLocalDateTime))
                    .filter(i -> interval.equals(Interval.YTD) || i.getLocalDateTime().isAfter(date))
                    .map(i -> {
                        double time = switch (interval) {
                            // hours since day before at 9
                            case ONE_DAY -> Duration.between(date, i.getLocalDateTime()).toMinutes()/(24 *60.0);
                            case SEVEN_DAY, THIRTY_DAY -> Duration.between(date, i.getLocalDateTime()).toHours() / 24.0;
                            case YTD -> Duration.between(date, i.getLocalDateTime()).toDays();
                        };
                        return new XYChart.Data<Number, Number>(time, i.getClose());
                    }).toList();


            ObservableList<XYChart.Data<Number, Number>> data = FXCollections.observableList(list);

            NumberAxis xAxis = new NumberAxis();
            xAxis.setLabel(graphChoiceBox.getValue());
            /*xAxis.setAutoRanging(false);
            xAxis.setTickUnit(1);
            xAxis.setTickUnit(1);


            switch (interval) {
                case ONE_DAY:
                    xAxis.setLowerBound(0);
                    xAxis.setUpperBound(9);
                    break;
                case SEVEN_DAY:
                    xAxis.setLowerBound(0);
                    xAxis.setUpperBound(7);
                    break;
                case THIRTY_DAY:
                    xAxis.setLowerBound(0);
                    xAxis.setUpperBound(30);
                    break;
                case YTD:
                    xAxis.setLowerBound(data.get(0).getXValue().doubleValue());
                    xAxis.setUpperBound(data.get(data.size() - 1).getXValue().doubleValue());
                    break;
            }

             */
            double yMin = data.stream().map(XYChart.Data::getYValue).map(Number::doubleValue).map(Math::ceil).min(Double::compareTo).orElse(0.0);
            double yMax = data.stream().map(XYChart.Data::getYValue).map(Number::doubleValue).map(Math::ceil).max(Double::compareTo).orElse(0.0);

            NumberAxis yAxis = new NumberAxis();
            yAxis.setLabel("Price");
            yAxis.setAutoRanging(false);
            yAxis.setLowerBound(yMin);
            yAxis.setUpperBound(yMax);
            yAxis.setTickUnit(.1);

            LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);
            lineChart.getData().add(new XYChart.Series<>(data));
            lineChart.setCreateSymbols(false);

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