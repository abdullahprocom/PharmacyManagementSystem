package com.pharmacy.controller;

import com.pharmacy.dao.MedicineDAO;
import com.pharmacy.dao.PurchaseDAO;
import com.pharmacy.dao.ReportDAO;
import com.pharmacy.dao.SalesDAO;
import com.pharmacy.model.Medicine;
import com.pharmacy.model.Purchase;
import com.pharmacy.model.Sale;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class ReportsController {

    // TAB 1: Sales
    @FXML private DatePicker dpSalesFrom;
    @FXML private DatePicker dpSalesTo;
    @FXML private Label lblSalesReportCount;
    @FXML private Label lblSalesReportTotal;
    @FXML private TableView<Sale> tblSalesReport;
    @FXML private TableColumn<Sale, Number> colRepSaleId;
    @FXML private TableColumn<Sale, String> colRepSaleDate;
    @FXML private TableColumn<Sale, String> colRepSaleMethod;
    @FXML private TableColumn<Sale, BigDecimal> colRepSaleTotal;

    // TAB 2: Purchases
    @FXML private DatePicker dpPurchasesFrom;
    @FXML private DatePicker dpPurchasesTo;
    @FXML private Label lblPurchasesReportCount;
    @FXML private Label lblPurchasesReportTotal;
    @FXML private TableView<Purchase> tblPurchasesReport;
    @FXML private TableColumn<Purchase, Number> colRepPurId;
    @FXML private TableColumn<Purchase, String> colRepPurInvoice;
    @FXML private TableColumn<Purchase, String> colRepPurSupplier;
    @FXML private TableColumn<Purchase, String> colRepPurDate;
    @FXML private TableColumn<Purchase, BigDecimal> colRepPurTotal;

    // TAB 3: Low Stock
    @FXML private TableView<Medicine> tblLowStockReport;
    @FXML private TableColumn<Medicine, Number> colRepLowId;
    @FXML private TableColumn<Medicine, String> colRepLowName;
    @FXML private TableColumn<Medicine, String> colRepLowCategory;
    @FXML private TableColumn<Medicine, Number> colRepLowStock;
    @FXML private TableColumn<Medicine, Number> colRepLowLimit;
    @FXML private TableColumn<Medicine, BigDecimal> colRepLowPrice;

    // TAB 4: Expiry Radar
    @FXML private ComboBox<String> cmbExpiryFilter;
    @FXML private TableView<ReportDAO.BatchAlert> tblExpiryReport;
    @FXML private TableColumn<ReportDAO.BatchAlert, String> colRepExpName;
    @FXML private TableColumn<ReportDAO.BatchAlert, String> colRepExpBatch;
    @FXML private TableColumn<ReportDAO.BatchAlert, String> colRepExpDate;
    @FXML private TableColumn<ReportDAO.BatchAlert, Number> colRepExpRemaining;
    @FXML private TableColumn<ReportDAO.BatchAlert, String> colRepExpStatus;

    private final SalesDAO salesDAO = new SalesDAO();
    private final PurchaseDAO purchaseDAO = new PurchaseDAO();
    private final MedicineDAO medicineDAO = new MedicineDAO();
    private final ReportDAO reportDAO = new ReportDAO();

    @FXML
    public void initialize() {
        LocalDate firstOfMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate today = LocalDate.now();

        dpSalesFrom.setValue(firstOfMonth);
        dpSalesTo.setValue(today);
        dpPurchasesFrom.setValue(firstOfMonth);
        dpPurchasesTo.setValue(today);

        setupSalesTab();
        setupPurchasesTab();
        setupLowStockTab();
        setupExpiryTab();

        handleFilterSalesReport();
        handleFilterPurchasesReport();
        loadLowStockReport();
        loadExpiryReport();
    }

    private void setupSalesTab() {
        colRepSaleId.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getSaleId()));
        colRepSaleDate.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSaleDate()));
        colRepSaleMethod.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPaymentMethod()));
        colRepSaleTotal.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getTotalAmountEgp()));

        colRepSaleTotal.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal val, boolean empty) {
                super.updateItem(val, empty);
                setText((empty || val == null) ? null : String.format("%.2f ج.م", val));
            }
        });
    }

    private void setupPurchasesTab() {
        colRepPurId.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getPurchaseId()));
        colRepPurInvoice.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getInvoiceNumber() != null ? d.getValue().getInvoiceNumber() : "—"
        ));
        colRepPurSupplier.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSupplierName()));
        colRepPurDate.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPurchaseDate()));
        colRepPurTotal.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getTotalAmountEgp()));

        colRepPurTotal.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal val, boolean empty) {
                super.updateItem(val, empty);
                setText((empty || val == null) ? null : String.format("%.2f ج.م", val));
            }
        });
    }

    private void setupLowStockTab() {
        colRepLowId.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getMedicineId()));
        colRepLowName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getMedicineName()));
        colRepLowCategory.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCategory()));
        colRepLowStock.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getSellableStock()));
        colRepLowLimit.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getReorderLevel()));
        colRepLowPrice.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getSalePriceEgp()));

        colRepLowPrice.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal val, boolean empty) {
                super.updateItem(val, empty);
                setText((empty || val == null) ? null : String.format("%.2f ج.م", val));
            }
        });

        colRepLowStock.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Number val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(val.toString());
                    setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
                }
            }
        });
    }

    private void setupExpiryTab() {
        cmbExpiryFilter.setItems(FXCollections.observableArrayList(
            "خلال 90 يوماً (90 Days Ahead)",
            "خلال 60 يوماً (60 Days Ahead)",
            "خلال 30 يوماً (30 Days Ahead)",
            "منتهية الصلاحية فقط (Expired Only)"
        ));
        cmbExpiryFilter.setValue("خلال 90 يوماً (90 Days Ahead)");
        cmbExpiryFilter.valueProperty().addListener((obs, o, n) -> loadExpiryReport());

        colRepExpName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().medicineName()));
        colRepExpBatch.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().batchNumber()));
        colRepExpDate.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().expiryDate()));
        colRepExpRemaining.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().remainingQuantity()));
        colRepExpStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().status()));

        colRepExpStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label("EXPIRED".equalsIgnoreCase(status) ? "منتهي الصلاحية" : "قريب الانتهاء");
                    badge.getStyleClass().add("EXPIRED".equalsIgnoreCase(status) ? "badge-expired" : "badge-warning");
                    setGraphic(badge);
                }
            }
        });
    }

    @FXML
    public void handleFilterSalesReport() {
        String from = dpSalesFrom.getValue() != null ? dpSalesFrom.getValue().toString() : "";
        String to = dpSalesTo.getValue() != null ? dpSalesTo.getValue().toString() : "";
        try {
            List<Sale> sales = salesDAO.getSalesHistory(from, to);
            tblSalesReport.setItems(FXCollections.observableArrayList(sales));
            lblSalesReportCount.setText(String.format("%,d", sales.size()));

            BigDecimal total = BigDecimal.ZERO;
            for (Sale s : sales) {
                total = total.add(s.getTotalAmountEgp());
            }
            lblSalesReportTotal.setText(String.format("%,.2f", total));
        } catch (SQLException e) {
            showError("فشل تحميل تقرير المبيعات: " + e.getMessage());
        }
    }

    @FXML
    public void handleFilterPurchasesReport() {
        String from = dpPurchasesFrom.getValue() != null ? dpPurchasesFrom.getValue().toString() : "";
        String to = dpPurchasesTo.getValue() != null ? dpPurchasesTo.getValue().toString() : "";
        try {
            List<Purchase> purchases = purchaseDAO.getPurchasesHistory(from, to);
            tblPurchasesReport.setItems(FXCollections.observableArrayList(purchases));
            lblPurchasesReportCount.setText(String.format("%,d", purchases.size()));

            BigDecimal total = BigDecimal.ZERO;
            for (Purchase p : purchases) {
                total = total.add(p.getTotalAmountEgp());
            }
            lblPurchasesReportTotal.setText(String.format("%,.2f", total));
        } catch (SQLException e) {
            showError("فشل تحميل تقرير المشتريات: " + e.getMessage());
        }
    }

    @FXML
    public void loadLowStockReport() {
        try {
            List<Medicine> all = medicineDAO.getMedicines("", "All", "Active");
            List<Medicine> low = all.stream().filter(m -> m.getSellableStock() <= m.getReorderLevel()).toList();
            tblLowStockReport.setItems(FXCollections.observableArrayList(low));
        } catch (SQLException e) {
            showError("فشل تحميل تقرير النواقص: " + e.getMessage());
        }
    }

    @FXML
    public void loadExpiryReport() {
        try {
            int days = 90;
            String filter = cmbExpiryFilter.getValue();
            if (filter != null) {
                if (filter.contains("30")) days = 30;
                else if (filter.contains("60")) days = 60;
                else if (filter.contains("90")) days = 90;
                else if (filter.contains("Expired")) days = 0;
            }

            List<ReportDAO.BatchAlert> list = reportDAO.getExpirationAlerts(days);
            if (filter != null && filter.contains("Expired")) {
                list = list.stream().filter(b -> "EXPIRED".equalsIgnoreCase(b.status())).toList();
            }
            tblExpiryReport.setItems(FXCollections.observableArrayList(list));
        } catch (SQLException e) {
            showError("فشل تحميل تقرير الصلاحيات: " + e.getMessage());
        }
    }

    // CSV EXPORTS with UTF-8 BOM for Arabic support in Excel
    @FXML
    public void handleExportSalesCSV() {
        List<Sale> items = tblSalesReport.getItems();
        if (items.isEmpty()) {
            showError("لا توجد بيانات لتصديرها.");
            return;
        }

        File file = chooseSaveFile("Sales_Report.csv");
        if (file == null) return;

        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            writer.write('\uFEFF'); // Byte Order Mark for Excel UTF-8 Arabic
            writer.println("Invoice ID,Date Time,Payment Method,Total Amount (EGP)");
            for (Sale s : items) {
                writer.printf("%d,\"%s\",\"%s\",%.2f%n",
                    s.getSaleId(), s.getSaleDate(), s.getPaymentMethod(), s.getTotalAmountEgp());
            }
            showSuccess("تم تصدير تقرير المبيعات بنجاح إلى:\n" + file.getAbsolutePath());
        } catch (Exception e) {
            showError("فشل تصدير CSV: " + e.getMessage());
        }
    }

    @FXML
    public void handleExportPurchasesCSV() {
        List<Purchase> items = tblPurchasesReport.getItems();
        if (items.isEmpty()) {
            showError("لا توجد بيانات لتصديرها.");
            return;
        }

        File file = chooseSaveFile("Purchases_Report.csv");
        if (file == null) return;

        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            writer.write('\uFEFF');
            writer.println("Purchase ID,Invoice Number,Supplier Name,Purchase Date,Total Amount (EGP)");
            for (Purchase p : items) {
                writer.printf("%d,\"%s\",\"%s\",\"%s\",%.2f%n",
                    p.getPurchaseId(),
                    p.getInvoiceNumber() != null ? p.getInvoiceNumber() : "",
                    p.getSupplierName(),
                    p.getPurchaseDate(),
                    p.getTotalAmountEgp());
            }
            showSuccess("تم تصدير تقرير المشتريات بنجاح إلى:\n" + file.getAbsolutePath());
        } catch (Exception e) {
            showError("فشل تصدير CSV: " + e.getMessage());
        }
    }

    @FXML
    public void handleExportLowStockCSV() {
        List<Medicine> items = tblLowStockReport.getItems();
        if (items.isEmpty()) {
            showError("لا توجد بيانات لتصديرها.");
            return;
        }

        File file = chooseSaveFile("Low_Stock_Report.csv");
        if (file == null) return;

        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            writer.write('\uFEFF');
            writer.println("Medicine ID,Medicine Name,Category,Current Sellable Stock,Reorder Level,Sale Price (EGP)");
            for (Medicine m : items) {
                writer.printf("%d,\"%s\",\"%s\",%d,%d,%.2f%n",
                    m.getMedicineId(), m.getMedicineName(), m.getCategory(),
                    m.getSellableStock(), m.getReorderLevel(), m.getSalePriceEgp());
            }
            showSuccess("تم تصدير تقرير النواقص بنجاح إلى:\n" + file.getAbsolutePath());
        } catch (Exception e) {
            showError("فشل تصدير CSV: " + e.getMessage());
        }
    }

    @FXML
    public void handleExportExpiryCSV() {
        List<ReportDAO.BatchAlert> items = tblExpiryReport.getItems();
        if (items.isEmpty()) {
            showError("لا توجد بيانات لتصديرها.");
            return;
        }

        File file = chooseSaveFile("Expiry_Radar_Report.csv");
        if (file == null) return;

        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            writer.write('\uFEFF');
            writer.println("Medicine Name,Batch Number,Expiry Date,Remaining Quantity,Status");
            for (ReportDAO.BatchAlert b : items) {
                writer.printf("\"%s\",\"%s\",\"%s\",%d,\"%s\"%n",
                    b.medicineName(), b.batchNumber(), b.expiryDate(), b.remainingQuantity(), b.status());
            }
            showSuccess("تم تصدير تقرير الصلاحيات بنجاح إلى:\n" + file.getAbsolutePath());
        } catch (Exception e) {
            showError("فشل تصدير CSV: " + e.getMessage());
        }
    }

    private File chooseSaveFile(String defaultName) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("حفظ التقرير كملف CSV");
        chooser.setInitialFileName(defaultName);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"));
        return chooser.showSaveDialog(tblSalesReport.getScene().getWindow());
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("خطأ");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private void showSuccess(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("تم التصدير");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
