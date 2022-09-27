package com.stockviewer.Controllers;

import com.stockviewer.Data.DataManager;
import com.stockviewer.Data.Order;
import com.stockviewer.Data.SellOrder;
import com.stockviewer.StockViewer;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

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
        DataManager.getOrders().stream()
                .filter(i -> !(i instanceof SellOrder))
                .map(Order::toString)
                .forEach(i -> portfolioList.getItems().add(i));

        searchTextField.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode().equals(KeyCode.ENTER))
                search();
        });
        /* TODO Finish
        searchButton.getScene().getRoot().setOnKeyPressed(keyEvent -> {
            if (!searchTextField.isFocused()) {
                searchTextField.setText(searchTextField.getText() + keyEvent.getText());
                searchTextField.requestFocus();
            }
        });
         */
    }

    @FXML
    void SearchAction(ActionEvent event) {
        search();
    }

    void search() {
        if (Objects.equals(searchButton.getText(), "")) return;
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