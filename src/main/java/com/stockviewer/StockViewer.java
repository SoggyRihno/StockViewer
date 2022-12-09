package com.stockviewer;

import com.stockviewer.Functionality.DataManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Optional;

public class StockViewer extends Application {
    private static Stage STAGE;

    public static Stage getStage(){
        return STAGE;
    }

    @Override
    public void start(Stage stage) throws IOException {
        STAGE = stage;
        FXMLLoader fxmlLoader = new FXMLLoader(StockViewer.class.getResource("Pages/HomePage.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 600, 500);
        stage.setTitle("Stock Viewer");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
    @Override
    public void stop() {
        DataManager.stop();
    }
    public static void forceApiKey() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setGraphic(null);
        dialog.setHeaderText("Set API Key");
        dialog.setTitle("Set API Key");
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && result.get().matches("[A-Z0-9]{16}")) {
            DataManager.setAPIKey(result.get());
        }else {
            Alert fail = new Alert(Alert.AlertType.ERROR, "Api key was empty or invalid");
            fail.showAndWait();
            forceApiKey();
        }
    }
}