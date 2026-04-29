module com.smartlib {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql; // Added this line

    opens com.smartlib to javafx.fxml;
    opens com.smartlib.model to javafx.base;
    opens com.smartlib.ui to javafx.fxml;
    opens com.smartlib.ui.pages to javafx.fxml;

    exports com.smartlib;
    exports com.smartlib.model;
    exports com.smartlib.service;
    exports com.smartlib.ui;
    exports com.smartlib.ui.pages;
}
