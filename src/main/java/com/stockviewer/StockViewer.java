package com.stockviewer;

import com.stockviewer.Functionality.DataManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;
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
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("Pages/Default.css")).toExternalForm());
        stage.setTitle("Stock Viewer");
        stage.setScene(scene);
        //stage.setResizable(false);
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
        Button button = new Button("Set");
        button.setOnAction(actionEvent -> dialog.close());
        Optional<String> result = dialog.showAndWait();

        if (result.isPresent() && result.get().matches("")) {
            DataManager.setAPIKey(result.get());
        }else {
            forceApiKey();
        }
    }
}