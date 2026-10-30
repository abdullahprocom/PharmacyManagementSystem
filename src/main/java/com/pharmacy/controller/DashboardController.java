package com.pharmacy.controller;

import com.pharmacy.dao.ReportDAO;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class DashboardController {

    @FXML private Label lblTotalMedicines;
    @FXML private Label lblTotalStock;
    @FXML private Label lblTodaySales;
    @FXML private Label lblLowStockCount;

    @FXML private BarChart<String, Number> salesBarChart;

    @FXML private TableView<ReportDAO.LowStockItem> tblLowStock;
    @FXML private TableColumn<ReportDAO.LowStockItem, String> colLowName;
    @FXML private TableColumn<ReportDAO.LowStockItem, Number> colLowStock;
    @FXML private TableColumn<ReportDAO.LowStockItem, Number> colLowLimit;

    @FXML private TableView<ReportDAO.BatchAlert> tblExpiry;
    @FXML private TableColumn<ReportDAO.BatchAlert, String> colExpName;
    @FXML private TableColumn<ReportDAO.BatchAlert, String> colExpBatch;
    @FXML private TableColumn<ReportDAO.BatchAlert, String> colExpDate;
    @FXML private TableColumn<ReportDAO.BatchAlert, Number> colExpRemaining;
    @FXML private TableColumn<ReportDAO.BatchAlert, String> colExpStatus;

    private final ReportDAO reportDAO = new ReportDAO();

    @FXML
    public void initialize() {
        setupLowStockTable();
        setupExpiryTable();
        refreshDashboard();
    }

    private void setupLowStockTable() {
        colLowName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().medicineName()));
        colLowStock.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().sellableStock()));
        colLowLimit.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().reorderLevel()));
        
        // Highlight critical stock in red/amber
        colLowStock.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item.toString());
                    if (item.intValue() == 0) {
                        setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #d97706; -fx-font-weight: bold;");
                    }
                }
            }
        });
    }

    private void setupExpiryTable() {
        colExpName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().medicineName()));
        colExpBatch.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().batchNumber()));
        colExpDate.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().expiryDate()));
        colExpRemaining.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().remainingQuantity()));
        colExpStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().status()));

        colExpStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label();
                    if ("EXPIRED".equalsIgnoreCase(status)) {
                        badge.setText("منتهي الصلاحية");
                        badge.getStyleClass().add("badge-expired");
                    } else {
                        badge.setText("قريب الانتهاء");
                        badge.getStyleClass().add("badge-warning");
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });
    }

    @FXML
    public void refreshDashboard() {
        try {
            // 1. KPIs
            ReportDAO.DashboardKPIs kpis = reportDAO.getDashboardKPIs();
            lblTotalMedicines.setText(String.format("%,d", kpis.totalMedicines()));
            lblTotalStock.setText(String.format("%,d", kpis.totalSellableStock()));
            lblTodaySales.setText(String.format("%,.2f ج.م", kpis.todaySalesEgp()));
            lblLowStockCount.setText(String.format("%,d", kpis.lowStockCount()));

            // 2. Bar Chart
            salesBarChart.getData().clear();
            Map<String, BigDecimal> chartMap = reportDAO.getLast7DaysSalesChartData();
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("المبيعات اليومية");
            for (Map.Entry<String, BigDecimal> entry : chartMap.entrySet()) {
                // Short date display (MM-dd)
                String dateLabel = entry.getKey().length() >= 10 ? entry.getKey().substring(5) : entry.getKey();
                series.getData().add(new XYChart.Data<>(dateLabel, entry.getValue().doubleValue()));
            }
            salesBarChart.getData().add(series);

            // 3. Tables
            List<ReportDAO.LowStockItem> lowStockList = reportDAO.getLowStockMedicines();
            tblLowStock.setItems(FXCollections.observableArrayList(lowStockList));

            List<ReportDAO.BatchAlert> expiryList = reportDAO.getExpirationAlerts(90);
            tblExpiry.setItems(FXCollections.observableArrayList(expiryList));

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
