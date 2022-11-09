package com.stockviewer.Controllers;

import com.stockviewer.Functionality.DataManager;
import com.stockviewer.Functionality.Interval;
import com.stockviewer.Functionality.Order;
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
import java.util.Arrays;
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
    private ListView<Order> portfolioList;
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
        lineChart.setAnimated(false);
        chartBox.getChildren().add(lineChart);

        searchTextField.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode().equals(KeyCode.ENTER)) search();
        });
        copeMenuItem.addEventHandler(EventType.ROOT, event -> cope());
        APIMenuItem.addEventHandler(EventType.ROOT, event -> setAPIKey());
        resetMenuItem.addEventHandler(EventType.ROOT, event -> clearData());
        importMenuItem.addEventHandler(EventType.ROOT, event -> importData());
        searchButton.setOnAction(actionEvent -> search());

        rangeChoiceBox.setOnAction(actionEvent -> updateChart());
        rangeChoiceBox.setItems(FXCollections.observableList(Arrays.stream(Interval.values()).map(String::valueOf).toList()));
        rangeChoiceBox.getSelectionModel().select(0);

        portfolioList.setEditable(false);
        portfolioList.setOnMouseClicked(mouseEvent -> {
            Order selected = portfolioList.getSelectionModel().getSelectedItem();
            if (selected != null)
                search(selected.getSymbol());
        });
        portfolioList.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Order order, boolean empty) {
                super.updateItem(order, empty);
                if (empty || order == null)
                    setText(null);
                else
                    setText(String.format("%s\t%d\t%,.2f\t%s", order.getSymbol(), order.getAmount(), order.getBuyPrice(), order.isSold() ? "SOLD" : ""));
            }
        });
        updateList();
        updateChart();
    }

    private void clearData() {
        Alert alert = new Alert(Alert.AlertType.NONE, "Do you want to permanently clear your data?", ButtonType.YES, ButtonType.NO);
        alert.showAndWait();
        if (alert.getResult().equals(ButtonType.YES)) {
            DataManager.clear();
            updateChart();
        }
    }

    private void importData() {
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
    }

    void search() {
        if (searchTextField != null)
            search(searchTextField.getText());
    }

    void search(String symbol) {
        if (symbol == null || symbol.isBlank())
            return;
        try {
            FXMLLoader loader = new FXMLLoader(StockViewer.class.getResource("Pages/StockPage.fxml"));
            loader.setController(new StockPageController(symbol));
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

    void updateList() {
        List<Order> orders = DataManager.getOrders();
        if (!orders.isEmpty()) {
            portfolioList.setItems(FXCollections.observableList(orders));
        }
    }

    void updateChart() {
        List<Order> orders = DataManager.getOrders();
        if (orders.isEmpty()) return;

        Interval interval = Interval.fromString(rangeChoiceBox.getSelectionModel().getSelectedItem());

        long minDay = orders.stream().parallel().map(Order::getBuyDate).map(i -> LocalDateTime.parse(i, DataManager.getDateTimeFormatter())).mapToLong(i -> Duration.between(LocalDateTime.now(), i).toDays()).max().orElse(0);
        LocalDateTime earliest = LocalDate.now().minusDays(interval.equals(Interval.YTD) ? --minDay : interval.getRange()).atTime(9, 0);
        List<XYChart.Data<String, Number>> data = new ArrayList<>();

        double starting = DataManager.getInitial() + orders.stream().mapToDouble(Order::getSignedValue).sum();

        data.add(new XYChart.Data<>(DataManager.formatByInterval(earliest, interval), starting));

        for (int i = 0; i < orders.size(); i++) {
            if (LocalDateTime.parse(orders.get(i).getBuyDate(), DataManager.getDateTimeFormatter()).isBefore(earliest)) {
                LocalDateTime time = LocalDateTime.parse(orders.get(i).getBuyDate(), DataManager.getDateTimeFormatter());
                double current = DataManager.getInitial();
                for (int j = 0; j < i; j++)
                    current += orders.get(j).getSignedValue();
                data.add(new XYChart.Data<>(DataManager.formatByInterval(time, interval), current));
            }
        }
        if (!data.isEmpty()) {
            data.add(new XYChart.Data<>(DataManager.formatByInterval(LocalDateTime.now(), interval), data.get(data.size() - 1).getYValue().doubleValue()));
            List<Double> yRange = data.stream().parallel().map(XYChart.Data::getYValue).map(Number::doubleValue).sorted().toList();
            ((NumberAxis) lineChart.getYAxis()).setLowerBound(Math.floor(yRange.get(0)));
            ((NumberAxis) lineChart.getYAxis()).setUpperBound(Math.ceil(yRange.get(yRange.size() - 1)));

            if (lineChart.getData() == null)
                lineChart.setData(FXCollections.observableArrayList());
            lineChart.getData().clear();
            lineChart.getData().add(new XYChart.Series<>());

            XYChart.Series<String, Number> series = lineChart.getData().get(0);
            data.forEach(i->series.getData().add(i));
        }
    }

    private void cope() {
        Alert alert = new Alert(Alert.AlertType.NONE, ":(", ButtonType.OK, ButtonType.NO);
        alert.setTitle("That sounds like a personal problem");
        alert.setX(1000 * Math.random());
        alert.setY(1000 * Math.random());
        alert.setOnHiding(Event::consume);
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isEmpty() || result.get().equals(ButtonType.NO))
            cope();
    }
}