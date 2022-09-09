package com.stockviewer;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class StockViewer extends Application {
    //this is the stupid
    private static String Symbol = "";


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

    public static void setSymbol(String symbol){
        Symbol = symbol;
    }

    public static String getSymbol(){
        return Symbol;
    }


}