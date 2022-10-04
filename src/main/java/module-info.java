module com.stockviewer.stockviewer {
    requires jdk.unsupported;
    requires javafx.controls;
    requires javafx.fxml;
    requires org.controlsfx.controls;
    requires org.kordamp.bootstrapfx.core;
    requires java.net.http;
    requires com.google.gson;

    exports com.stockviewer;
    exports com.stockviewer.controllers;
    exports com.stockviewer.exceptions.API;
    exports com.stockviewer.exceptions.Poor;
    exports com.stockviewer.data;
    exports com.stockviewer.data.wrappers;




    opens com.stockviewer to javafx.fxml;
    opens com.stockviewer.controllers to javafx.fxml;
    opens com.stockviewer.data to com.google.gson;
}