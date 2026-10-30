package com.pharmacy.controller;

import com.pharmacy.dao.MedicineDAO;
import com.pharmacy.model.Medicine;
import com.pharmacy.service.InventoryService;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class MedicinesController {

    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cmbCategoryFilter;
    @FXML private ComboBox<String> cmbStatusFilter;

    @FXML private TableView<Medicine> tblMedicines;
    @FXML private TableColumn<Medicine, Number> colId;
    @FXML private TableColumn<Medicine, String> colName;
    @FXML private TableColumn<Medicine, String> colCategory;
    @FXML private TableColumn<Medicine, String> colBarcode;
    @FXML private TableColumn<Medicine, BigDecimal> colPrice;
    @FXML private TableColumn<Medicine, Number> colStock;
    @FXML private TableColumn<Medicine, Number> colReorder;
    @FXML private TableColumn<Medicine, Boolean> colStatus;
    @FXML private TableColumn<Medicine, Void> colActions;

    private final MedicineDAO medicineDAO = new MedicineDAO();
    private final InventoryService inventoryService = new InventoryService();

    @FXML
    public void initialize() {
        setupFilters();
        setupTable();
        loadMedicines();
    }

    private void setupFilters() {
        cmbCategoryFilter.getItems().add("الكل (All)");
        cmbCategoryFilter.getItems().addAll(inventoryService.getAllowedCategories());
        cmbCategoryFilter.setValue("الكل (All)");

        cmbStatusFilter.setItems(FXCollections.observableArrayList("الكل (All)", "نشط (Active)", "معطل (Inactive)"));
        cmbStatusFilter.setValue("الكل (All)");

        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> loadMedicines());
        cmbCategoryFilter.valueProperty().addListener((obs, oldVal, newVal) -> loadMedicines());
        cmbStatusFilter.valueProperty().addListener((obs, oldVal, newVal) -> loadMedicines());
    }

    private void setupTable() {
        colId.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getMedicineId()));
        colName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getMedicineName()));
        colCategory.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCategory()));
        colBarcode.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getBarcode() != null ? d.getValue().getBarcode() : "—"
        ));
        colPrice.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getSalePriceEgp()));
        colStock.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getSellableStock()));
        colReorder.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getReorderLevel()));
        colStatus.setCellValueFactory(d -> new SimpleBooleanProperty(d.getValue().isActive()));

        // Format Price
        colPrice.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f ج.م", price));
                }
            }
        });

        // Highlight Stock
        colStock.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Number stock, boolean empty) {
                super.updateItem(stock, empty);
                if (empty || stock == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(stock.toString());
                    Medicine rowMed = getTableRow() != null ? getTableRow().getItem() : null;
                    if (rowMed != null && stock.intValue() <= rowMed.getReorderLevel()) {
                        setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #15803d; -fx-font-weight: bold;");
                    }
                }
            }
        });

        // Status Badge
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean active, boolean empty) {
                super.updateItem(active, empty);
                if (empty || active == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label(active ? "نشط" : "معطل");
                    badge.getStyleClass().add(active ? "badge-active" : "badge-inactive");
                    setGraphic(badge);
                }
            }
        });

        // Action Buttons (Edit & Toggle)
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit = new Button("تعديل");
            private final Button btnToggle = new Button("تعطيل/تفعيل");
            private final HBox pane = new HBox(6, btnEdit, btnToggle);

            {
                btnEdit.getStyleClass().add("tbl-btn-edit");
                btnToggle.getStyleClass().add("tbl-btn-toggle");

                btnEdit.setOnAction(e -> {
                    Medicine med = getTableRow().getItem();
                    if (med != null) showMedicineDialog(med);
                });

                btnToggle.setOnAction(e -> {
                    Medicine med = getTableRow().getItem();
                    if (med != null) {
                        try {
                            medicineDAO.setStatus(med.getMedicineId(), !med.isActive());
                            loadMedicines();
                        } catch (SQLException ex) {
                            showError("خطأ في تحديث الحالة: " + ex.getMessage());
                        }
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    private void loadMedicines() {
        try {
            String query = txtSearch.getText();
            String cat = cmbCategoryFilter.getValue();
            if ("الكل (All)".equals(cat)) cat = "All";

            String status = cmbStatusFilter.getValue();
            String statusParam = null;
            if ("نشط (Active)".equals(status)) statusParam = "Active";
            else if ("معطل (Inactive)".equals(status)) statusParam = "Inactive";

            List<Medicine> list = medicineDAO.getMedicines(query, cat, statusParam);
            tblMedicines.setItems(FXCollections.observableArrayList(list));
        } catch (SQLException e) {
            showError("فشل تحميل قائمة الأدوية: " + e.getMessage());
        }
    }

    @FXML
    public void handleClearFilters() {
        txtSearch.clear();
        cmbCategoryFilter.setValue("الكل (All)");
        cmbStatusFilter.setValue("الكل (All)");
        loadMedicines();
    }

    @FXML
    public void handleNewMedicine() {
        showMedicineDialog(null);
    }

    private void showMedicineDialog(Medicine existing) {
        Dialog<Medicine> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "إضافة دواء جديد للكتالوج" : "تعديل بيانات الدواء");
        dialog.setHeaderText(existing == null ? "أدخل بيانات الصنف الدوائي الجديد" : "تعديل صنف: " + existing.getMedicineName());

        ButtonType btnSaveType = new ButtonType("حفظ البيانات", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnSaveType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 10));

        TextField nameField = new TextField(existing != null ? existing.getMedicineName() : "");
        nameField.setPromptText("اسم الدواء بالعربية أو الإنجليزية");

        ComboBox<String> catBox = new ComboBox<>(FXCollections.observableArrayList(inventoryService.getAllowedCategories()));
        catBox.setValue(existing != null ? existing.getCategory() : inventoryService.getAllowedCategories().get(0));

        TextField barcodeField = new TextField(existing != null && existing.getBarcode() != null ? existing.getBarcode() : "");
        barcodeField.setPromptText("الباركود الدولي (اختياري)");

        TextField priceField = new TextField(existing != null ? existing.getSalePriceEgp().toString() : "0.00");
        priceField.setPromptText("سعر البيع بالجنيه");

        TextField reorderField = new TextField(existing != null ? String.valueOf(existing.getReorderLevel()) : "10");
        reorderField.setPromptText("الحد الأدنى للتنبيه");

        grid.add(new Label("اسم الدواء: *"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("التصنيف: *"), 0, 1);
        grid.add(catBox, 1, 1);
        grid.add(new Label("الباركود:"), 0, 2);
        grid.add(barcodeField, 1, 2);
        grid.add(new Label("سعر البيع (ج.م): *"), 0, 3);
        grid.add(priceField, 1, 3);
        grid.add(new Label("حد إعادة الطلب: *"), 0, 4);
        grid.add(reorderField, 1, 4);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnSaveType) {
                try {
                    String name = nameField.getText().trim();
                    if (name.isEmpty()) throw new IllegalArgumentException("اسم الدواء مطلوب");

                    String barcode = barcodeField.getText().trim();
                    if (barcode.isEmpty()) barcode = null;

                    BigDecimal price = new BigDecimal(priceField.getText().trim());
                    if (price.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("السعر لا يمكن أن يكون سالباً");

                    int reorder = Integer.parseInt(reorderField.getText().trim());
                    if (reorder < 0) throw new IllegalArgumentException("حد الطلب لا يمكن أن يكون سالباً");

                    Medicine med = existing != null ? existing : new Medicine();
                    med.setMedicineName(name);
                    med.setCategory(catBox.getValue());
                    med.setBarcode(barcode);
                    med.setSalePriceEgp(price);
                    med.setReorderLevel(reorder);
                    if (existing == null) med.setActive(true);

                    return med;
                } catch (Exception e) {
                    showError("خطأ في المدخلات: " + e.getMessage());
                    return null;
                }
            }
            return null;
        });

        Optional<Medicine> result = dialog.showAndWait();
        result.ifPresent(med -> {
            try {
                if (existing == null) {
                    // Check barcode uniqueness
                    if (med.getBarcode() != null && !medicineDAO.isBarcodeUnique(med.getBarcode(), null)) {
                        showError("الباركود مسجل مسبقاً لدواء آخر!");
                        return;
                    }
                    medicineDAO.insertMedicine(med);
                } else {
                    if (med.getBarcode() != null && !medicineDAO.isBarcodeUnique(med.getBarcode(), med.getMedicineId())) {
                        showError("الباركود مسجل مسبقاً لدواء آخر!");
                        return;
                    }
                    medicineDAO.updateMedicine(med);
                }
                loadMedicines();
            } catch (SQLException e) {
                showError("خطأ بقاعدة البيانات: " + e.getMessage());
            }
        });
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("تنبيه");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
