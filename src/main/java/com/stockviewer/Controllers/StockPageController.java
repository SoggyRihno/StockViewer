package com.stockviewer.Controllers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.stockviewer.API.APIManager;
import com.stockviewer.API.Interval;
import com.stockviewer.Data.DataPoint;
import com.stockviewer.StockViewer;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class StockPageController {

    @FXML
    private Button BuyButton;

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
    private Button sellButton;

    @FXML
    private Label symbolLabel;

    @FXML
    private Label volumeLabel;


    @FXML
    void initialize() {
        update();
    }

    void update() {
        try {
            String raw = APIManager.getStockData(StockViewer.getSymbol(), Interval.FIVE_MINUTES).get();
            JsonObject json = JsonParser.parseString(raw).getAsJsonObject();

            final String timeSeriesRegex = "(Time\\sSeries\\s\\()(5|15|30|60)(min\\))";

            if (json.keySet().contains("Error Message"))
                badSymbol();
            else {
                String series = json.keySet().stream().filter(i -> i.matches(timeSeriesRegex)).findFirst().orElse("");
                if (!series.equals("")) {
                    JsonObject data = json.get(series).getAsJsonObject();
                    final LocalDateTime currentTime = LocalDateTime.now().minusDays(1);

                    List<DataPoint> latestData = data.keySet().stream()
                            .map(i -> new DataPoint(i, data.get(i).getAsJsonObject()))
                            .filter(i -> i.getLocalDateTime().isAfter(currentTime))
                            .sorted(DataPoint::compareTo)
                            .toList();

                    if (latestData.size() > 1) {
                        symbolLabel.setText(StockViewer.getSymbol().toUpperCase());
                        openLabel.setText(String.valueOf(latestData.get(latestData.size() - 1).getOpen()));
                        volumeLabel.setText(String.valueOf(latestData.stream().mapToInt(DataPoint::getVolume).sum()));
                        dateLabel.setText(latestData.get(latestData.size() - 1).getLocalDateTime().format(DataPoint.dateFormatter));
                        double difference = latestData.get(latestData.size() - 1).getOpen() - latestData.get(latestData.size() - 2).getOpen();
                        String change = String.format("%s (%.2f %%)", difference >= 0 ? String.format("+ %,.2f", difference) : String.format("%,.2f", difference), (difference / latestData.get(latestData.size() - 2).getOpen()) * 100);
                        changeLabel.setText(change);
                    }

                }
            }


        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            update();
        }
    }

    void badSymbol() {
        System.out.println(";(");
    }

    @FXML
    void backButtonAction(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(StockViewer.class.getResource("XML/HomePage.fxml"));
            Stage stage = (Stage) backButton.getScene().getWindow();
            Scene scene = new Scene(loader.load());
            stage.setScene(scene);
        } catch (IOException io) {
            io.printStackTrace();
        }
    }

    @FXML
    void buyAction(ActionEvent event) {

    }

    @FXML
    void sellAction(ActionEvent event) {

    }

    public void backAction(ActionEvent actionEvent) {
    }
}
