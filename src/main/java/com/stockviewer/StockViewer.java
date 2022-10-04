package com.stockviewer;

import com.stockviewer.data.DataManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class StockViewer extends Application {

    @Override
    public void start(Stage stage) throws IOException {
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
}