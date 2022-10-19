package com.stockviewer.controllers;

import com.stockviewer.StockViewer;
import com.stockviewer.data.DataManager;
import javafx.event.EventType;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;

public class HomePageController {
    @FXML
    private MenuItem copeMenuItem;
    @FXML
    private Menu fileMenu;
    @FXML
    private ImageView graphImageView;
    @FXML
    private Menu helpMenu;
    @FXML
    private MenuItem importMenuItem;
    @FXML
    private MenuBar mainMenuBar;
    @FXML
    private ListView<String> portfolioList;
    @FXML
    private MenuItem resetMenuItem;
    @FXML
    private Button searchButton;
    @FXML
    private TextField searchTextField;
    @FXML
    private MenuItem settingsMenuItem;

    @FXML
    public void initialize() {
        searchTextField.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode().equals(KeyCode.ENTER))
                search();
        });

        copeMenuItem.addEventHandler(EventType.ROOT, event -> System.out.println("cope"));

        resetMenuItem.addEventHandler(EventType.ROOT, event -> {
            Alert alert = new Alert(Alert.AlertType.NONE, "Do you want to permanently clear your data?", ButtonType.YES, ButtonType.NO);
            alert.showAndWait();
            if (alert.getResult().equals(ButtonType.YES)) {
                try {
                    DataManager.clear();
                } catch (IOException e) {
                    alert = new Alert(Alert.AlertType.ERROR, "Unable to clear File");
                    alert.showAndWait();
                }
            }
        });

        importMenuItem.addEventHandler(EventType.ROOT, event -> {
            Alert alert = new Alert(Alert.AlertType.NONE, "Do you want to permanently overwrite your data?", ButtonType.YES, ButtonType.NO);
            alert.showAndWait();
            if (alert.getResult().equals(ButtonType.YES)) {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Import File");
                File file = fileChooser.showOpenDialog(Window.getWindows().stream().filter(Window::isShowing).findFirst().orElse(null));
                try {
                    DataManager.importFile(file);
                } catch (IOException e) {
                    alert = new Alert(Alert.AlertType.ERROR, "Unable to import file");
                    alert.showAndWait();
                }
            }
        });
        searchButton.setOnAction(actionEvent -> search());
    }
    void search() {
        if (searchButton.getText().isEmpty()) return;
        try {
            FXMLLoader loader = new FXMLLoader(StockViewer.class.getResource("XML/StockPage.fxml"));
            loader.setController(new StockPageController(searchTextField.getText()));
            Stage stage = (Stage) searchButton.getScene().getWindow();
            Scene scene = new Scene(loader.load());
            stage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}