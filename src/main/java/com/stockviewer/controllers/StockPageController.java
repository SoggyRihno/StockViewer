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
import java.time.LocalDateTime;
import java.util.Comparator;
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

        backButton.setOnAction(actionEvent -> back());
        Platform.runLater(() -> {
            try {
                update();
            } catch (APIException e) {
                showError(symbol + " is not a recognised symbol");
                back();
            }
        });
        ses.scheduleWithFixedDelay(() -> Platform.runLater(() -> {
            try {
                update();
            } catch (APIException ignored) {
            }
        }), 1, 1, TimeUnit.MINUTES);
    }

    void update() throws APIException {
        LocalDateTime date = LocalDateTime.now().minusDays(1);
        currentData = StockData.newStockData(symbol);
        openLabel.setText(String.valueOf(currentData.getLatestOpen()));
        volumeLabel.setText(String.valueOf(currentData.getDailyVolume(date)));
        changeLabel.setText(currentData.getLatestChange());
        changeLabel.setStyle(changeLabel.getText().contains("-") ? "-fx-text-fill: red" : "-fx-text-fill: green");
        symbolLabel.setText(symbol);
        dateLabel.setText(currentData.getLatestTimeFormatted());
        amountField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                amountField.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });
        updateChart();
    }

    @FXML
    void buyAction(ActionEvent event) throws APIException {
        update();
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


    public void updateChart() {
        try {

            Interval interval = Interval.fromString(graphChoiceBox.getValue());
            StockData stockData = StockData.newStockData(symbol, interval);

            NumberAxis xAxis = new NumberAxis();
            xAxis.setLabel(graphChoiceBox.getValue());
            NumberAxis yAxis = new NumberAxis();
            yAxis.setLabel("Price");

            XYChart.Series<Number, Number> series = new XYChart.Series<>();
            ObservableList<XYChart.Data<Number, Number>> data = series.getData();

            LocalDateTime date = LocalDateTime.now().minusDays(1).minusDays(interval.getRange());
            (interval.equals(Interval.YTD)
                    ? stockData.getData()
                    : stockData.getData().stream()
                        .sorted(Comparator.comparing(StockDataPoint::getLocalDateTime))
                        .filter(i -> i.getLocalDateTime().isAfter(date))
                        .toList()
            ).forEach(i -> data.add(new XYChart.Data<>(Duration.between(i.getLocalDateTime(), date).toHours(), i.getOpen())));

            LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);
            lineChart.getData().add(series);

            chartBox.getChildren().clear();
            chartBox.getChildren().add(lineChart);
        } catch (APIException e) {
            throw new RuntimeException(e);
        }
    }
}