package com.pharmacy;

import com.pharmacy.database.Database;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {

    @Override
    public void init() {
        // Initialize SQLite embedded database, tables, and seed data
        Database.initializeDatabase();
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("/com/pharmacy/fxml/main-layout.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, 1220, 800);
            scene.getStylesheets().add(App.class.getResource("/com/pharmacy/css/style.css").toExternalForm());

            primaryStage.setTitle("نظام إدارة الصيدلية المتكامل — PharmaCare Desktop (FEFO Community Edition)");
            primaryStage.setMinWidth(1080);
            primaryStage.setMinHeight(700);
            primaryStage.setScene(scene);
            primaryStage.show();

        } catch (IOException e) {
            System.err.println("Failed to load application layout: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
