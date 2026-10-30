package com.pharmacy.controller;

import com.pharmacy.dao.MedicineDAO;
import com.pharmacy.dao.SupplierDAO;
import com.pharmacy.model.Medicine;
import com.pharmacy.model.Purchase;
import com.pharmacy.model.PurchaseItem;
import com.pharmacy.model.Supplier;
import com.pharmacy.service.PurchaseService;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class PurchasesController {

    // Tab 1: New Invoice
    @FXML private ComboBox<Supplier> cmbSupplier;
    @FXML private TextField txtInvoiceNumber;
    @FXML private DatePicker dpPurchaseDate;

    @FXML private ComboBox<Medicine> cmbMedicine;
    @FXML private TextField txtBatchNumber;
    @FXML private DatePicker dpExpiryDate;
    @FXML private TextField txtQuantity;
    @FXML private TextField txtCostPrice;

    @FXML private TableView<PurchaseItem> tblNewItems;
    @FXML private TableColumn<PurchaseItem, String> colItemName;
    @FXML private TableColumn<PurchaseItem, String> colItemBatch;
    @FXML private TableColumn<PurchaseItem, String> colItemExpiry;
    @FXML private TableColumn<PurchaseItem, Number> colItemQty;
    @FXML private TableColumn<PurchaseItem, BigDecimal> colItemCost;
    @FXML private TableColumn<PurchaseItem, BigDecimal> colItemTotal;
    @FXML private TableColumn<PurchaseItem, Void> colItemAction;

    @FXML private Label lblItemsCount;
    @FXML private Label lblGrandTotal;

    // Tab 2: History
    @FXML private TextField txtSearchHistory;
    @FXML private TableView<Purchase> tblHistory;
    @FXML private TableColumn<Purchase, Number> colHistId;
    @FXML private TableColumn<Purchase, String> colHistInvoice;
    @FXML private TableColumn<Purchase, String> colHistSupplier;
    @FXML private TableColumn<Purchase, String> colHistDate;
    @FXML private TableColumn<Purchase, BigDecimal> colHistTotal;
    @FXML private TableColumn<Purchase, Number> colHistItems;
    @FXML private TableColumn<Purchase, Void> colHistActions;

    private final ObservableList<PurchaseItem> newInvoiceItems = FXCollections.observableArrayList();
    private final PurchaseService purchaseService = new PurchaseService();
    private final SupplierDAO supplierDAO = new SupplierDAO();
    private final MedicineDAO medicineDAO = new MedicineDAO();

    @FXML
    public void initialize() {
        dpPurchaseDate.setValue(LocalDate.now());
        setupNewItemsTable();
        setupHistoryTable();
        loadDropdowns();
        loadHistory();
    }

    private void loadDropdowns() {
        try {
            List<Supplier> suppliers = supplierDAO.getSuppliers("", true);
            cmbSupplier.setItems(FXCollections.observableArrayList(suppliers));

            List<Medicine> medicines = medicineDAO.getMedicines("", "All", "Active");
            cmbMedicine.setItems(FXCollections.observableArrayList(medicines));
        } catch (SQLException e) {
            showError("فشل تحميل الموردين أو الأدوية: " + e.getMessage());
        }
    }

    private void setupNewItemsTable() {
        tblNewItems.setItems(newInvoiceItems);

        colItemName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getMedicineName()));
        colItemBatch.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getBatchNumber()));
        colItemExpiry.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getExpiryDate()));
        colItemQty.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getQuantityReceived()));
        colItemCost.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getUnitCostEgp()));
        colItemTotal.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getSubtotalEgp()));

        colItemCost.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal val, boolean empty) {
                super.updateItem(val, empty);
                setText((empty || val == null) ? null : String.format("%.2f ج.م", val));
            }
        });

        colItemTotal.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal val, boolean empty) {
                super.updateItem(val, empty);
                setText((empty || val == null) ? null : String.format("%.2f ج.م", val));
            }
        });

        colItemAction.setCellFactory(col -> new TableCell<>() {
            private final Button btnRemove = new Button("🗑️ حذف");
            {
                btnRemove.setStyle("-fx-font-size: 11px; -fx-text-fill: #dc2626;");
                btnRemove.setOnAction(e -> {
                    PurchaseItem item = getTableRow().getItem();
                    if (item != null) {
                        newInvoiceItems.remove(item);
                        updateInvoiceTotals();
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnRemove);
            }
        });
    }

    private void setupHistoryTable() {
        colHistId.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getPurchaseId()));
        colHistInvoice.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getInvoiceNumber() != null ? d.getValue().getInvoiceNumber() : "—"
        ));
        colHistSupplier.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSupplierName()));
        colHistDate.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPurchaseDate()));
        colHistTotal.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getTotalAmountEgp()));
        colHistItems.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getItems().size()));

        colHistTotal.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal val, boolean empty) {
                super.updateItem(val, empty);
                setText((empty || val == null) ? null : String.format("%.2f ج.م", val));
            }
        });

        colHistActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnView = new Button("عرض البنود");
            {
                btnView.getStyleClass().add("tbl-btn-edit");
                btnView.setOnAction(e -> {
                    Purchase p = getTableRow().getItem();
                    if (p != null) showPurchaseDetailsModal(p);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnView);
            }
        });

        txtSearchHistory.textProperty().addListener((obs, oldVal, newVal) -> loadHistory());
    }

    @FXML
    public void handleAddItem() {
        Medicine selectedMed = cmbMedicine.getValue();
        if (selectedMed == null) {
            showError("يجب اختيار الدواء أولاً!");
            return;
        }

        String batch = txtBatchNumber.getText().trim();
        if (batch.isEmpty()) {
            showError("رقم التشغيلة (Batch) مطلوب!");
            return;
        }

        LocalDate exp = dpExpiryDate.getValue();
        if (exp == null) {
            showError("تاريخ الصلاحية مطلوب!");
            return;
        }
        if (exp.isBefore(LocalDate.now()) || exp.isEqual(LocalDate.now())) {
            showError("تاريخ الصلاحية يجب أن يكون في المستقبل (غير منتهي الصلاحية)!");
            return;
        }

        int qty;
        try {
            qty = Integer.parseInt(txtQuantity.getText().trim());
            if (qty <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showError("الكمية يجب أن تكون عدداً صحيحاً موجباً أكبر من الصفر!");
            return;
        }

        BigDecimal cost;
        try {
            cost = new BigDecimal(txtCostPrice.getText().trim());
            if (cost.compareTo(BigDecimal.ZERO) < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showError("سعر الشراء يجب أن يكون رقماً صحيحاً أو عشرياً موجباً!");
            return;
        }

        PurchaseItem item = new PurchaseItem();
        item.setMedicineId(selectedMed.getMedicineId());
        item.setMedicineName(selectedMed.getMedicineName());
        item.setBatchNumber(batch);
        item.setExpiryDate(exp.toString());
        item.setQuantityReceived(qty);
        item.setQuantityRemaining(qty);
        item.setUnitCostEgp(cost);

        newInvoiceItems.add(item);
        updateInvoiceTotals();

        // Reset item fields
        cmbMedicine.setValue(null);
        txtBatchNumber.clear();
        dpExpiryDate.setValue(null);
        txtQuantity.clear();
        txtCostPrice.clear();
    }

    private void updateInvoiceTotals() {
        lblItemsCount.setText("عدد الأصناف: " + newInvoiceItems.size());
        BigDecimal total = BigDecimal.ZERO;
        for (PurchaseItem it : newInvoiceItems) {
            total = total.add(it.getSubtotalEgp());
        }
        lblGrandTotal.setText(String.format("%.2f ج.م", total));
    }

    @FXML
    public void handleClearInvoice() {
        newInvoiceItems.clear();
        updateInvoiceTotals();
        txtInvoiceNumber.clear();
        cmbSupplier.setValue(null);
        dpPurchaseDate.setValue(LocalDate.now());
    }

    @FXML
    public void handleSavePurchase() {
        Supplier supplier = cmbSupplier.getValue();
        if (supplier == null) {
            showError("يجب اختيار المورد أو شركة التوريد!");
            return;
        }

        LocalDate pDate = dpPurchaseDate.getValue();
        if (pDate == null) {
            showError("تاريخ الفاتورة مطلوب!");
            return;
        }

        if (newInvoiceItems.isEmpty()) {
            showError("لا يمكن حفظ فاتورة توريد فارغة! أضف صنفاً واحداً على الأقل.");
            return;
        }

        try {
            Purchase purchase = new Purchase();
            purchase.setSupplierId(supplier.getSupplierId());
            purchase.setPurchaseDate(pDate.toString());
            purchase.setInvoiceNumber(txtInvoiceNumber.getText().trim());
            purchase.setItems(newInvoiceItems);

            purchaseService.processPurchase(purchase);

            Alert success = new Alert(Alert.AlertType.INFORMATION);
            success.setTitle("نجاح التوريد");
            success.setHeaderText("تم حفظ فاتورة الشراء وتحديث الأرصدة بنجاح!");
            success.setContentText("رقم العملية المسجلة: " + purchase.getPurchaseId() + "\nتمت إضافة التشغيلات للمخزون وجاهزة للبيع بنظام FEFO.");
            success.showAndWait();

            handleClearInvoice();
            loadHistory();

        } catch (Exception e) {
            showError("فشل حفظ فاتورة الشراء: " + e.getMessage());
        }
    }

    @FXML
    public void loadHistory() {
        try {
            String query = txtSearchHistory.getText() != null ? txtSearchHistory.getText().trim().toLowerCase() : "";
            List<Purchase> all = purchaseService.getHistory(null, null);

            List<Purchase> filtered = all.stream().filter(p -> {
                if (query.isEmpty()) return true;
                boolean matchInv = p.getInvoiceNumber() != null && p.getInvoiceNumber().toLowerCase().contains(query);
                boolean matchSupp = p.getSupplierName() != null && p.getSupplierName().toLowerCase().contains(query);
                return matchInv || matchSupp;
            }).toList();

            tblHistory.setItems(FXCollections.observableArrayList(filtered));
        } catch (SQLException e) {
            showError("فشل تحميل سجل المشتريات: " + e.getMessage());
        }
    }

    private void showPurchaseDetailsModal(Purchase purchase) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("تفاصيل بنود الفاتورة رقم #" + purchase.getPurchaseId());
        dialog.setHeaderText("المورد: " + purchase.getSupplierName() + " | التاريخ: " + purchase.getPurchaseDate() 
            + " | الإجمالي: " + String.format("%.2f ج.م", purchase.getTotalAmountEgp()));
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        TableView<PurchaseItem> table = new TableView<>();
        TableColumn<PurchaseItem, String> colMed = new TableColumn<>("اسم الدواء");
        colMed.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getMedicineName()));
        colMed.setPrefWidth(220);

        TableColumn<PurchaseItem, String> colB = new TableColumn<>("رقم التشغيلة");
        colB.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getBatchNumber()));
        colB.setPrefWidth(120);

        TableColumn<PurchaseItem, String> colE = new TableColumn<>("تاريخ الصلاحية");
        colE.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getExpiryDate()));
        colE.setPrefWidth(110);

        TableColumn<PurchaseItem, Number> colQRec = new TableColumn<>("الكمية الموردة");
        colQRec.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getQuantityReceived()));
        colQRec.setPrefWidth(100);

        TableColumn<PurchaseItem, Number> colQRem = new TableColumn<>("المتبقي بالمخزن");
        colQRem.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getQuantityRemaining()));
        colQRem.setPrefWidth(100);

        TableColumn<PurchaseItem, String> colCost = new TableColumn<>("سعر الشراء");
        colCost.setCellValueFactory(d -> new SimpleStringProperty(String.format("%.2f ج.م", d.getValue().getUnitCostEgp())));
        colCost.setPrefWidth(110);

        table.getColumns().addAll(colMed, colB, colE, colQRec, colQRem, colCost);
        table.setPrefWidth(780);
        table.setPrefHeight(300);

        try {
            List<PurchaseItem> items = purchaseService.getDetails(purchase.getPurchaseId());
            table.setItems(FXCollections.observableArrayList(items));
        } catch (SQLException e) {
            showError("فشل تحميل بنود الفاتورة: " + e.getMessage());
        }

        VBox content = new VBox(10, table);
        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("خطأ");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
