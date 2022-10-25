package com.stockviewer;

import com.stockviewer.data.DataManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Optional;

public class StockViewer extends Application {
    private static Stage mainStage;


    @Override
    public void start(Stage stage) throws IOException {
        mainStage = stage;
        FXMLLoader fxmlLoader = new FXMLLoader(StockViewer.class.getResource("XML/HomePage.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 600, 500);
        stage.setTitle("Stock Viewer");
        stage.setScene(scene);
        stage.show();
    } 

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void stop(){
        DataManager.stop();
    }

    public static Stage getStage(){
        return mainStage;
    }

    public static void forceApiKey(){
        TextInputDialog dialog = new TextInputDialog();
        dialog.setGraphic(null);
        dialog.setHeaderText("Set API Key");
        dialog.setTitle("Set API Key");
        Button button = new Button("Set");
        button.setOnAction(actionEvent -> dialog.close());
        Optional<String> result = dialog.showAndWait();


        if (result.isPresent() && result.get().matches("[a-zA-Z0-9]{16}"))
            DataManager.setAPIKey(result.get());
        else
            forceApiKey();
    }
}