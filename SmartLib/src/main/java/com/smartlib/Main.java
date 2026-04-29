package com.smartlib;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import com.smartlib.ui.MainWindow;


public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        MainWindow mainWindow = new MainWindow();
        // Root is now a StackPane to allow overlays
        Scene scene = new Scene(mainWindow.getRoot(), 1280, 750);

        String css = getClass().getResource("/css/dark-theme.css").toExternalForm();
        scene.getStylesheets().add(css);

        primaryStage.setTitle("SmartLib — Library Management System");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}


//C:\Users\YourName\smartlib_data\smartlib.mv.db   tthis is where the database data is