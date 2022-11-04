package com.stockviewer.Controllers;

import com.stockviewer.Functionality.DataManager;
import com.stockviewer.Functionality.Interval;
import com.stockviewer.Functionality.Order;
import com.stockviewer.Functionality.SellOrder;
import com.stockviewer.StockViewer;
import javafx.collections.FXCollections;
import javafx.event.Event;
import javafx.event.EventType;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class HomePageController {
    @FXML
    private ChoiceBox<String> rangeChoiceBox;
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
    private VBox chartBox;
    private LineChart<String, Number> lineChart;

    @FXML
    public void initialize() {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Date");
        xAxis.setGapStartAndEnd(false);

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Price");

        lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setCreateSymbols(false);

        chartBox.getChildren().add(lineChart);
        portfolioList.setEditable(false);
        searchTextField.setOnKeyPressed(keyEvent -> {if (keyEvent.getCode().equals(KeyCode.ENTER)) search();});
        copeMenuItem.addEventHandler(EventType.ROOT, event -> cope());
        APIMenuItem.addEventHandler(EventType.ROOT, event -> setAPIKey());
        resetMenuItem.addEventHandler(EventType.ROOT, event -> {
            Alert alert = new Alert(Alert.AlertType.NONE, "Do you want to permanently clear your data?", ButtonType.YES, ButtonType.NO);
            alert.showAndWait();
            if (alert.getResult().equals(ButtonType.YES)) {
                DataManager.clear();
                updateChart();
            }
        });
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
            updateList();
            updateChart();
        });
        searchButton.setOnAction(actionEvent -> search());
        for (Interval value : Interval.values())
            rangeChoiceBox.getItems().add(value.toString());
        rangeChoiceBox.getSelectionModel().select(0);
        rangeChoiceBox.setOnAction(actionEvent -> updateChart());
        portfolioList.setOnMouseClicked(mouseEvent -> {
            String selected = portfolioList.getSelectionModel().getSelectedItem();
            if(selected !=null)
                search(selected.substring(0,selected.indexOf(' ')));
        });
        updateList();
        updateChart();
    }

    void search(){
        if (searchTextField != null)
            search(searchTextField.getText());
    }

    void search(String symbol) {
        if (symbol == null || symbol.isEmpty())
            return;
        try {
            FXMLLoader loader = new FXMLLoader(StockViewer.class.getResource("Pages/StockPage.fxml"));
            loader.setController(new StockPageController(searchTextField.getText()));
            StockViewer.getStage().setScene(new Scene(loader.load()));
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

    void updateList(){
        List<Order> orders = DataManager.getOrders();
        if(!orders.isEmpty()){
            portfolioList.setItems(FXCollections.observableList(orders
                    .stream()
                    .map(Order::toString)
                    .toList()
            ));
        }
    }

    void updateChart() {
        List<Order> orders = DataManager.getOrders();
        if (!orders.isEmpty()) {
            Interval interval = Interval.fromString(rangeChoiceBox.getSelectionModel().getSelectedItem());

            long minDay = orders.stream().parallel().map(Order::getBuyDate).map(i -> LocalDateTime.parse(i, DataManager.getDateTimeFormatter())).mapToLong(i -> Duration.between(LocalDateTime.now(), i).toDays()).max().orElse(0);
            LocalDateTime earliest = LocalDate.now().minusDays(interval.equals(Interval.YTD) ? --minDay : interval.getRange()).atTime(9, 0);
            List<XYChart.Data<String, Number>> data = new ArrayList<>();

            double starting = DataManager.getInitial();
            for (Order value : orders)
                if (LocalDateTime.parse(value.getBuyDate(), DataManager.getDateTimeFormatter()).isAfter(earliest))
                    starting += (value instanceof SellOrder ? 1 : -1) * value.getAmount() * value.getBuyPrice();
            data.add(new XYChart.Data<>(DataManager.formatByInterval(earliest, interval), starting));

            for (int i = 0; i < orders.size(); i++) {
                if (LocalDateTime.parse(orders.get(i).getBuyDate(), DataManager.getDateTimeFormatter()).isBefore(earliest)) {
                    LocalDateTime time = LocalDateTime.parse(orders.get(i).getBuyDate(), DataManager.getDateTimeFormatter());
                    double current = DataManager.getInitial();
                    for (int j = 0; j < i; j++) {
                        Order order = orders.get(j);
                        current += (order instanceof SellOrder ? 1 : -1) * order.getAmount() * order.getBuyPrice();
                    }
                    data.add(new XYChart.Data<>(DataManager.formatByInterval(time, interval), current));
                }
            }
            if (!data.isEmpty()) {
                data.add(new XYChart.Data<>(DataManager.formatByInterval(LocalDateTime.now(), interval), data.get(data.size() - 1).getYValue().doubleValue()));
                List<Double> yRange = data.stream().parallel().map(XYChart.Data::getYValue).map(Number::doubleValue).sorted().toList();
                ((NumberAxis) lineChart.getYAxis()).setLowerBound(Math.floor(yRange.get(0)));
                ((NumberAxis) lineChart.getYAxis()).setUpperBound(Math.ceil(yRange.get(yRange.size() - 1)));
                lineChart.setData(FXCollections.observableList(List.of(new XYChart.Series<>(FXCollections.observableList(data)))));
                lineChart.requestFocus();
            }
        }
    }

    private void cope() {
        Alert alert = new Alert(Alert.AlertType.NONE, ":(", ButtonType.OK, ButtonType.NO);
        alert.setTitle("That sounds like a personal problem");
        alert.setX(1000 * Math.random());
        alert.setY(1000 * Math.random());
        alert.setOnHiding(Event::consume);
        Optional<ButtonType> result = alert.showAndWait();
        if(result.isPresent() && result.get().equals(ButtonType.NO))
            cope();
    }
}