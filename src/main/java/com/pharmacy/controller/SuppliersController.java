package com.pharmacy.controller;

import com.pharmacy.dao.SupplierDAO;
import com.pharmacy.model.Supplier;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class SuppliersController {

    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cmbStatusFilter;

    @FXML private TableView<Supplier> tblSuppliers;
    @FXML private TableColumn<Supplier, Number> colId;
    @FXML private TableColumn<Supplier, String> colName;
    @FXML private TableColumn<Supplier, String> colPhone;
    @FXML private TableColumn<Supplier, String> colAddress;
    @FXML private TableColumn<Supplier, Boolean> colStatus;
    @FXML private TableColumn<Supplier, Void> colActions;

    private final SupplierDAO supplierDAO = new SupplierDAO();

    @FXML
    public void initialize() {
        setupFilters();
        setupTable();
        loadSuppliers();
    }

    private void setupFilters() {
        cmbStatusFilter.setItems(FXCollections.observableArrayList("الكل (All)", "نشط (Active)", "معطل (Inactive)"));
        cmbStatusFilter.setValue("الكل (All)");

        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> loadSuppliers());
        cmbStatusFilter.valueProperty().addListener((obs, oldVal, newVal) -> loadSuppliers());
    }

    private void setupTable() {
        colId.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getSupplierId()));
        colName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSupplierName()));
        colPhone.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getPhone() != null ? d.getValue().getPhone() : "—"
        ));
        colAddress.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getAddress() != null ? d.getValue().getAddress() : "—"
        ));
        colStatus.setCellValueFactory(d -> new SimpleBooleanProperty(d.getValue().isActive()));

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

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit = new Button("تعديل");
            private final Button btnToggle = new Button("تعطيل/تفعيل");
            private final HBox pane = new HBox(6, btnEdit, btnToggle);

            {
                btnEdit.getStyleClass().add("tbl-btn-edit");
                btnToggle.getStyleClass().add("tbl-btn-toggle");

                btnEdit.setOnAction(e -> {
                    Supplier s = getTableRow().getItem();
                    if (s != null) showSupplierDialog(s);
                });

                btnToggle.setOnAction(e -> {
                    Supplier s = getTableRow().getItem();
                    if (s != null) {
                        try {
                            supplierDAO.setStatus(s.getSupplierId(), !s.isActive());
                            loadSuppliers();
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

    @FXML
    public void loadSuppliers() {
        try {
            String query = txtSearch.getText();
            String status = cmbStatusFilter.getValue();
            Boolean activeOnly = null;
            if ("نشط (Active)".equals(status)) activeOnly = true;
            else if ("معطل (Inactive)".equals(status)) activeOnly = false;

            List<Supplier> list = supplierDAO.getSuppliers(query, activeOnly);
            tblSuppliers.setItems(FXCollections.observableArrayList(list));
        } catch (SQLException e) {
            showError("فشل تحميل الموردين: " + e.getMessage());
        }
    }

    @FXML
    public void handleNewSupplier() {
        showSupplierDialog(null);
    }

    private void showSupplierDialog(Supplier existing) {
        Dialog<Supplier> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "إضافة مورد جديد" : "تعديل بيانات المورد");
        dialog.setHeaderText(existing == null ? "تسجيل شركة توريد أدوية جديدة" : "تعديل بيانات: " + existing.getSupplierName());

        ButtonType btnSaveType = new ButtonType("حفظ البيانات", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnSaveType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 10));

        TextField nameField = new TextField(existing != null ? existing.getSupplierName() : "");
        nameField.setPromptText("اسم الشركة أو الموزع");

        TextField phoneField = new TextField(existing != null && existing.getPhone() != null ? existing.getPhone() : "");
        phoneField.setPromptText("رقم الهاتف أو الجوال");

        TextField addressField = new TextField(existing != null && existing.getAddress() != null ? existing.getAddress() : "");
        addressField.setPromptText("العنوان أو المدينة");

        grid.add(new Label("اسم الشركة / المورد: *"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("رقم الهاتف:"), 0, 1);
        grid.add(phoneField, 1, 1);
        grid.add(new Label("العنوان / المقر:"), 0, 2);
        grid.add(addressField, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnSaveType) {
                String name = nameField.getText().trim();
                if (name.isEmpty()) {
                    showError("اسم المورد مطلوب!");
                    return null;
                }
                Supplier s = existing != null ? existing : new Supplier();
                s.setSupplierName(name);
                s.setPhone(phoneField.getText().trim());
                s.setAddress(addressField.getText().trim());
                if (existing == null) s.setActive(true);
                return s;
            }
            return null;
        });

        Optional<Supplier> result = dialog.showAndWait();
        result.ifPresent(s -> {
            try {
                if (existing == null) {
                    supplierDAO.insertSupplier(s);
                } else {
                    supplierDAO.updateSupplier(s);
                }
                loadSuppliers();
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
