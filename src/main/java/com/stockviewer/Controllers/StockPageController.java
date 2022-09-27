package com.stockviewer.Controllers;

import com.stockviewer.Data.DataManager;
import com.stockviewer.Data.APIWrappers.StockData;
import com.stockviewer.Exceptions.API.APIException;
import com.stockviewer.Exceptions.Poor.InsufficientFundsException;
import com.stockviewer.Exceptions.Poor.NoStockException;
import com.stockviewer.Exceptions.Poor.PoorException;
import com.stockviewer.StockViewer;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class StockPageController {
    private final ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();
    private final String symbol;
    private StockData currentData;
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
    @FXML
    private TextField AmountField;

    public StockPageController(String symbol) {
        this.symbol = symbol;
    }

    @FXML
    void initialize() {
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
        AmountField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                AmountField.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });
    }

    @FXML
    void buyAction(ActionEvent event) throws APIException {
        update();
        if (!AmountField.getText().isEmpty() && Integer.parseInt(AmountField.getText()) != 0) {
            try {
                DataManager.buy(Integer.parseInt(AmountField.getText()), currentData.getLatestOpen(), symbol);
            } catch (InsufficientFundsException e) {
                showError("You are to poor :(");
            }
        }
    }

    @FXML
    void sellAction(ActionEvent event) throws APIException {
        update();
        if (!AmountField.getText().isEmpty() && Integer.parseInt(AmountField.getText()) != 0) {
            try {
                DataManager.sell(Integer.parseInt(AmountField.getText()), currentData.getLatestOpen(), symbol);
            } catch (PoorException e) {
                if (e instanceof NoStockException)
                    showError("You don't own enough of this stock");
                else
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