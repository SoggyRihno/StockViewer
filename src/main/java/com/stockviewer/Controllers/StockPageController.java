package com.stockviewer.Controllers;

import com.stockviewer.Data.StockData;
import com.stockviewer.Exceptions.APIException;
import com.stockviewer.StockViewer;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.stage.Popup;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class StockPageController {
    private final ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();
    private final String symbol;

    @FXML
    private Button BuyButton;
    @FXML
    private Button sellButton;
    @FXML
    private ImageView GraphImage;
    @FXML
    private Button backButton;
    @FXML
    private Label changeLabel;
    @FXML
    private Label dateLabel;
    @FXML
    private ChoiceBox<?> graphChoiceBox;
    @FXML
    private Label openLabel;
    @FXML
    private Label symbolLabel;
    @FXML
    private Label volumeLabel;

    public StockPageController() {
        this.symbol = StockViewer.getSymbol();
    }
    @FXML
    void initialize() {
        Platform.runLater(()->{try {update();} catch (APIException ignored) {badSymbol();}});
        ses.scheduleWithFixedDelay(()->Platform.runLater(()->{try {update();} catch (APIException ignored) {}}),1,1, TimeUnit.MINUTES);

        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();

        xAxis.setLabel("X axis");
        yAxis.setLabel("Y axis");


        LineChart<Integer,Integer> lineChart = new LineChart(xAxis,yAxis);

    }

    void update() throws APIException {
        LocalDateTime date = LocalDateTime.now().minusDays(1);

        StockData data = StockData.newStockData(symbol);
        openLabel.setText(String.valueOf(data.getLatestOpen()));
        volumeLabel.setText(String.valueOf(data.getDailyVolume(date)));
        changeLabel.setText(data.getLatestChange());
        changeLabel.setStyle(changeLabel.getText().contains("-") ? "-fx-text-fill: red" : "-fx-text-fill: green");
        symbolLabel.setText(symbol);
        dateLabel.setText(data.getLatestTimeFormatted());
    }
    @FXML
    void backButtonAction(ActionEvent event) {
        back();
    }
    @FXML
    void buyAction(ActionEvent event) {
        Popup popup = new Popup();
        popup.getContent().add(new TextField("test"));
        popup.show(BuyButton.getScene().getWindow());

    }
    @FXML
    void sellAction(ActionEvent event) {}
    public void backAction(ActionEvent actionEvent) {
        back();
    }
    public void back() {
        ses.shutdown();
        try {
            FXMLLoader loader = new FXMLLoader(StockViewer.class.getResource("XML/HomePage.fxml"));
            Stage stage = (Stage) backButton.getScene().getWindow();
            Scene scene = new Scene(loader.load());
            stage.setScene(scene);
        } catch (IOException e) {
            throw new RuntimeException(":(");
        }
    }

    private void badSymbol() {
        Alert alert = new Alert(Alert.AlertType.NONE, symbol + " is not a recognised symbol", ButtonType.OK);
        alert.showAndWait();
        back();
    }
}