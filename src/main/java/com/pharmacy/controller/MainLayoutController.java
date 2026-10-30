package com.pharmacy.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class MainLayoutController {

    @FXML private Button btnDashboard;
    @FXML private Button btnSales;
    @FXML private Button btnMedicines;
    @FXML private Button btnPurchases;
    @FXML private Button btnSuppliers;
    @FXML private Button btnReports;

    @FXML private StackPane contentArea;

    private final Map<String, Node> viewCache = new HashMap<>();
    private Button currentActiveButton;

    @FXML
    public void initialize() {
        currentActiveButton = btnDashboard;
        showDashboard();
    }

    @FXML
    public void showDashboard() {
        setActiveButton(btnDashboard);
        loadView("dashboard.fxml", false);
    }

    @FXML
    public void showSales() {
        setActiveButton(btnSales);
        loadView("sales.fxml", false);
    }

    @FXML
    public void showMedicines() {
        setActiveButton(btnMedicines);
        loadView("medicines.fxml", false);
    }

    @FXML
    public void showPurchases() {
        setActiveButton(btnPurchases);
        loadView("purchases.fxml", false);
    }

    @FXML
    public void showSuppliers() {
        setActiveButton(btnSuppliers);
        loadView("suppliers.fxml", false);
    }

    @FXML
    public void showReports() {
        setActiveButton(btnReports);
        loadView("reports.fxml", false);
    }

    private void setActiveButton(Button button) {
        if (currentActiveButton != null) {
            currentActiveButton.getStyleClass().remove("nav-button-active");
        }
        currentActiveButton = button;
        if (!currentActiveButton.getStyleClass().contains("nav-button-active")) {
            currentActiveButton.getStyleClass().add("nav-button-active");
        }
    }

    private void loadView(String fxmlName, boolean cache) {
        try {
            if (cache && viewCache.containsKey(fxmlName)) {
                contentArea.getChildren().setAll(viewCache.get(fxmlName));
                return;
            }

            URL resource = getClass().getResource("/com/pharmacy/fxml/" + fxmlName);
            if (resource == null) {
                System.err.println("Could not find FXML: " + fxmlName);
                return;
            }

            FXMLLoader loader = new FXMLLoader(resource);
            Node view = loader.load();

            if (cache) {
                viewCache.put(fxmlName, view);
            }

            contentArea.getChildren().setAll(view);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
