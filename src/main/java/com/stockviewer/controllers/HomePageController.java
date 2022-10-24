package com.stockviewer.controllers;

import com.stockviewer.StockViewer;
import com.stockviewer.data.DataManager;
import com.stockviewer.data.Order;
import com.stockviewer.data.SellOrder;
import javafx.collections.FXCollections;
import javafx.event.EventType;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
    private MenuItem APIMenuItem;
    @FXML
    private BorderPane borderPane;
    @FXML
    private LineChart<String, Number> lineChart;

    @FXML
    public void initialize() {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Date");
        xAxis.setGapStartAndEnd(false);

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Price");
        yAxis.setAutoRanging(false);

        lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setCreateSymbols(false);

        searchTextField.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode().equals(KeyCode.ENTER))
                search();
        });

        copeMenuItem.addEventHandler(EventType.ROOT, event -> {
        });
        APIMenuItem.addEventHandler(EventType.ROOT, event -> setAPIKey());


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
        //fixme
        importMenuItem.addEventHandler(EventType.ROOT, event -> {
            Alert alert = new Alert(Alert.AlertType.NONE, "Do you want to permanently overwrite your data?", ButtonType.YES, ButtonType.NO);
            alert.showAndWait();
            if (alert.getResult().equals(ButtonType.YES)) {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Import File");
                File file = fileChooser.showOpenDialog(Window.getWindows().stream().filter(Window::isShowing).findFirst().orElse(null));
                if (file != null) {
                    try {
                        DataManager.importFile(file.toPath());
                    } catch (IOException e) {
                        alert = new Alert(Alert.AlertType.ERROR, "Unable to import file");
                        alert.showAndWait();
                    }
                }
            }
        });

        searchButton.setOnAction(actionEvent -> search());
        List<Order> orders = DataManager.getOrders();
        List<XYChart.Data<String, Number>> data = new ArrayList<>();
        for (int i = 0; i < orders.size(); i++) {
            double current = DataManager.getInitial();
            for (int j = 0; j < i; j++) {
                Order order = orders.get(j);
                current += (order instanceof SellOrder ? 1 : -1) * order.getAmount() * order.getBuyPrice();
            }
            data.add(new XYChart.Data<>(orders.get(i).getBuyDate(), current));
        }

        if(!data.isEmpty()){
            List<Double> yRange = data.stream().map(XYChart.Data::getYValue).map(Number::doubleValue).toList();
            ((NumberAxis) lineChart.getYAxis()).setLowerBound(Math.floor(yRange.get(0)));
            ((NumberAxis) lineChart.getYAxis()).setUpperBound(Math.ceil(yRange.get(yRange.size() - 1)));
            lineChart.getData().clear();
            lineChart.getData().add(new XYChart.Series<>(FXCollections.observableList(data)));
        }
    }

    //todo api key is not getting set on load
    void search() {
        if (searchButton.getText().equalsIgnoreCase("")) return;
        try {
            FXMLLoader loader = new FXMLLoader(StockViewer.class.getResource("XML/StockPage.fxml"));
            loader.setController(new StockPageController(searchTextField.getText()));
            StockViewer.getStage().setScene(loader.load());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void setAPIKey() {
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
            new Alert(Alert.AlertType.NONE, "API key was empty or invalid", ButtonType.OK).show();

    }

}