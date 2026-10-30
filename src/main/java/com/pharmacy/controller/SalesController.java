package com.pharmacy.controller;

import com.pharmacy.dao.MedicineDAO;
import com.pharmacy.model.CartItem;
import com.pharmacy.model.Medicine;
import com.pharmacy.model.Sale;
import com.pharmacy.model.SaleItem;
import com.pharmacy.service.SalesService;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class SalesController {

    @FXML private Label lblClock;

    // Search and Cart
    @FXML private TextField txtBarcodeSearch;
    @FXML private Spinner<Integer> spnQuantity;
    @FXML private Label lblAvailableStockPrompt;
    @FXML private ComboBox<Medicine> cmbQuickMedicine;

    @FXML private TableView<CartItem> tblCart;
    @FXML private TableColumn<CartItem, String> colCartName;
    @FXML private TableColumn<CartItem, BigDecimal> colCartPrice;
    @FXML private TableColumn<CartItem, Number> colCartQty;
    @FXML private TableColumn<CartItem, BigDecimal> colCartTotal;
    @FXML private TableColumn<CartItem, Void> colCartAction;

    // Totals & Checkout
    @FXML private ComboBox<String> cmbPaymentMethod;
    @FXML private Label lblSubtotal;
    @FXML private TextField txtDiscountPercent;
    @FXML private Label lblDiscountAmount;
    @FXML private Label lblNetTotal;
    @FXML private TextField txtPaidAmount;
    @FXML private Label lblChangeDue;

    // Tab 2: Sales History
    @FXML private TextField txtSearchSalesHistory;
    @FXML private TableView<Sale> tblSalesHistory;
    @FXML private TableColumn<Sale, Number> colHistSaleId;
    @FXML private TableColumn<Sale, String> colHistSaleDate;
    @FXML private TableColumn<Sale, String> colHistSaleMethod;
    @FXML private TableColumn<Sale, BigDecimal> colHistSaleTotal;
    @FXML private TableColumn<Sale, Void> colHistSaleActions;

    private final ObservableList<CartItem> cartItems = FXCollections.observableArrayList();
    private final SalesService salesService = new SalesService();
    private final MedicineDAO medicineDAO = new MedicineDAO();

    private List<Medicine> cachedSellableMedicines;
    private BigDecimal currentSubtotal = BigDecimal.ZERO;
    private BigDecimal currentNetTotal = BigDecimal.ZERO;

    @FXML
    public void initialize() {
        lblClock.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, yyyy-MM-dd")));

        spnQuantity.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 500, 1));
        spnQuantity.setEditable(true);

        cmbPaymentMethod.setItems(FXCollections.observableArrayList("CASH", "CARD"));
        cmbPaymentMethod.setValue("CASH");

        setupCartTable();
        setupHistoryTable();
        setupCalculations();
        loadSellableMedicines();
        loadSalesHistory();
    }

    private void setupCartTable() {
        tblCart.setItems(cartItems);

        colCartName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getMedicineName()));
        colCartPrice.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getUnitPriceEgp()));
        colCartQty.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getQuantity()));
        colCartTotal.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getSubtotalEgp()));

        colCartPrice.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal val, boolean empty) {
                super.updateItem(val, empty);
                setText((empty || val == null) ? null : String.format("%.2f ج.م", val));
            }
        });

        colCartTotal.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal val, boolean empty) {
                super.updateItem(val, empty);
                setText((empty || val == null) ? null : String.format("%.2f ج.م", val));
            }
        });

        colCartAction.setCellFactory(col -> new TableCell<>() {
            private final Button btnRemove = new Button("🗑️");
            {
                btnRemove.setStyle("-fx-font-size: 11px; -fx-text-fill: #dc2626;");
                btnRemove.setOnAction(e -> {
                    CartItem item = getTableRow().getItem();
                    if (item != null) {
                        cartItems.remove(item);
                        calculateTotals();
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
        colHistSaleId.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getSaleId()));
        colHistSaleDate.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSaleDate()));
        colHistSaleMethod.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPaymentMethod()));
        colHistSaleTotal.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getTotalAmountEgp()));

        colHistSaleTotal.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal val, boolean empty) {
                super.updateItem(val, empty);
                setText((empty || val == null) ? null : String.format("%.2f ج.م", val));
            }
        });

        colHistSaleActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnDetails = new Button("عرض بنود الفاتورة");
            {
                btnDetails.getStyleClass().add("tbl-btn-edit");
                btnDetails.setOnAction(e -> {
                    Sale sale = getTableRow().getItem();
                    if (sale != null) showSaleDetailsModal(sale);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnDetails);
            }
        });

        txtSearchSalesHistory.textProperty().addListener((obs, oldVal, newVal) -> loadSalesHistory());
    }

    private void setupCalculations() {
        txtDiscountPercent.textProperty().addListener((obs, oldVal, newVal) -> calculateTotals());
        txtPaidAmount.textProperty().addListener((obs, oldVal, newVal) -> calculateChange());

        cmbQuickMedicine.valueProperty().addListener((obs, oldVal, selected) -> {
            if (selected != null) {
                lblAvailableStockPrompt.setText("الرصيد المتاح: " + selected.getSellableStock() + " علبة");
            } else {
                lblAvailableStockPrompt.setText("");
            }
        });
    }

    @FXML
    public void loadSellableMedicines() {
        try {
            cachedSellableMedicines = medicineDAO.getActiveSellableMedicines();
            cmbQuickMedicine.setItems(FXCollections.observableArrayList(cachedSellableMedicines));
        } catch (SQLException e) {
            showError("فشل تحميل قائمة الأدوية: " + e.getMessage());
        }
    }

    @FXML
    public void handleSearchEnter() {
        String query = txtBarcodeSearch.getText().trim();
        if (query.isEmpty()) return;

        // Find medicine matching barcode or name
        Medicine found = null;
        for (Medicine m : cachedSellableMedicines) {
            if (query.equalsIgnoreCase(m.getBarcode()) || m.getMedicineName().toLowerCase().contains(query.toLowerCase())) {
                found = m;
                break;
            }
        }

        if (found == null) {
            showError("لم يتم العثور على دواء مطابق أو ليس لديه رصيد غير منتهي الصلاحية.");
            return;
        }

        addMedicineToCart(found, spnQuantity.getValue());
        txtBarcodeSearch.clear();
        spnQuantity.getValueFactory().setValue(1);
    }

    @FXML
    public void handleQuickSelect() {
        Medicine selected = cmbQuickMedicine.getValue();
        if (selected == null) {
            showError("اختر دواء من القائمة أولاً.");
            return;
        }
        addMedicineToCart(selected, spnQuantity.getValue());
        spnQuantity.getValueFactory().setValue(1);
    }

    @FXML
    public void handleAddToCart() {
        if (!txtBarcodeSearch.getText().trim().isEmpty()) {
            handleSearchEnter();
        } else if (cmbQuickMedicine.getValue() != null) {
            handleQuickSelect();
        } else {
            showError("ادخل الباركود أو اختر دواء لإضافته إلى السلة.");
        }
    }

    private void addMedicineToCart(Medicine med, int qty) {
        if (qty <= 0) {
            showError("الكمية يجب أن تكون أكبر من الصفر.");
            return;
        }

        // Check existing cart quantity
        int existingInCart = 0;
        CartItem existingCartItem = null;
        for (CartItem item : cartItems) {
            if (item.getMedicine().getMedicineId() == med.getMedicineId()) {
                existingInCart = item.getQuantity();
                existingCartItem = item;
                break;
            }
        }

        int requestedTotal = existingInCart + qty;
        if (requestedTotal > med.getSellableStock()) {
            showError("الكمية المطلوبة (" + requestedTotal + ") تتجاوز الرصيد الصالح للبيع (" + med.getSellableStock() + ") لهذا الدواء!");
            return;
        }

        if (existingCartItem != null) {
            existingCartItem.setQuantity(requestedTotal);
            tblCart.refresh();
        } else {
            cartItems.add(new CartItem(med, qty));
        }

        calculateTotals();
    }

    private void calculateTotals() {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem item : cartItems) {
            subtotal = subtotal.add(item.getSubtotalEgp());
        }
        currentSubtotal = subtotal;
        lblSubtotal.setText(String.format("%.2f ج.م", currentSubtotal));

        BigDecimal discountPct = BigDecimal.ZERO;
        try {
            String pctText = txtDiscountPercent.getText().trim();
            if (!pctText.isEmpty()) {
                discountPct = new BigDecimal(pctText);
                if (discountPct.compareTo(BigDecimal.ZERO) < 0 || discountPct.compareTo(BigDecimal.valueOf(100)) > 0) {
                    discountPct = BigDecimal.ZERO;
                }
            }
        } catch (NumberFormatException ignored) {}

        BigDecimal discountAmt = currentSubtotal.multiply(discountPct)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        lblDiscountAmount.setText(String.format("%.2f ج.م", discountAmt));

        currentNetTotal = currentSubtotal.subtract(discountAmt).setScale(2, RoundingMode.HALF_UP);
        lblNetTotal.setText(String.format("%.2f ج.م", currentNetTotal));

        calculateChange();
    }

    private void calculateChange() {
        BigDecimal paid = BigDecimal.ZERO;
        try {
            String paidText = txtPaidAmount.getText().trim();
            if (!paidText.isEmpty()) {
                paid = new BigDecimal(paidText);
            }
        } catch (NumberFormatException ignored) {}

        if (paid.compareTo(currentNetTotal) >= 0 && currentNetTotal.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal change = paid.subtract(currentNetTotal);
            lblChangeDue.setText(String.format("%.2f ج.م", change));
        } else {
            lblChangeDue.setText("0.00 ج.م");
        }
    }

    @FXML
    public void handleClearCart() {
        cartItems.clear();
        calculateTotals();
        txtPaidAmount.clear();
        txtDiscountPercent.setText("0");
    }

    @FXML
    public void handleCompleteSale() {
        if (cartItems.isEmpty()) {
            showError("سلة المشتريات فارغة! أضف أدوية أولاً.");
            return;
        }

        String paymentMethod = cmbPaymentMethod.getValue();
        if (paymentMethod == null || paymentMethod.isEmpty()) {
            paymentMethod = "CASH";
        }

        // Validate Cash payment
        BigDecimal paid = currentNetTotal;
        if ("CASH".equalsIgnoreCase(paymentMethod)) {
            String paidText = txtPaidAmount.getText().trim();
            if (!paidText.isEmpty()) {
                try {
                    paid = new BigDecimal(paidText);
                } catch (NumberFormatException e) {
                    showError("المبلغ المدفوع غير صحيح!");
                    return;
                }
            }
            if (paid.compareTo(currentNetTotal) < 0) {
                showError("المبلغ المدفوع (" + String.format("%.2f", paid) + " ج.م) أقل من إجمالي الفاتورة المطلوب (" + String.format("%.2f", currentNetTotal) + " ج.م)!");
                return;
            }
        }

        try {
            Sale sale = new Sale();
            sale.setSaleDate(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            sale.setPaymentMethod(paymentMethod);
            sale.setTotalAmountEgp(currentNetTotal);

            salesService.completeSale(sale, cartItems);

            BigDecimal change = paid.subtract(currentNetTotal);

            // Show Receipt Dialog
            Alert receipt = new Alert(Alert.AlertType.INFORMATION);
            receipt.setTitle("تم إتمام عملية البيع بنجاح");
            receipt.setHeaderText("فاتورة بيع رقم #" + sale.getSaleId() + " (FEFO صرف آلي)");
            receipt.setContentText(String.format("""
                التاريخ: %s
                طريقة الدفع: %s
                المجموع الفرعي: %.2f ج.م
                الخصم: %s
                الصافي المحصل: %.2f ج.م
                المدفوع: %.2f ج.م
                الباقي للعميل: %.2f ج.م
                
                تم تحديث الأرصدة المخزنية بنجاح.
                """,
                sale.getSaleDate(),
                paymentMethod,
                currentSubtotal,
                lblDiscountAmount.getText(),
                currentNetTotal,
                paid,
                change
            ));
            receipt.showAndWait();

            handleClearCart();
            loadSellableMedicines();
            loadSalesHistory();

        } catch (Exception e) {
            showError("فشل تنفيذ عملية البيع: " + e.getMessage());
        }
    }

    @FXML
    public void loadSalesHistory() {
        try {
            String query = txtSearchSalesHistory.getText() != null ? txtSearchSalesHistory.getText().trim().toLowerCase() : "";
            List<Sale> all = salesService.getHistory(null, null);

            List<Sale> filtered = all.stream().filter(s -> {
                if (query.isEmpty()) return true;
                boolean matchId = String.valueOf(s.getSaleId()).contains(query);
                boolean matchDate = s.getSaleDate() != null && s.getSaleDate().toLowerCase().contains(query);
                return matchId || matchDate;
            }).toList();

            tblSalesHistory.setItems(FXCollections.observableArrayList(filtered));
        } catch (SQLException e) {
            showError("فشل تحميل سجل المبيعات: " + e.getMessage());
        }
    }

    private void showSaleDetailsModal(Sale sale) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("تفاصيل فاتورة البيع رقم #" + sale.getSaleId());
        dialog.setHeaderText("تاريخ الصرف: " + sale.getSaleDate() + " | الدفع: " + sale.getPaymentMethod() 
            + " | الإجمالي: " + String.format("%.2f ج.م", sale.getTotalAmountEgp()));
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        TableView<SaleItem> table = new TableView<>();
        TableColumn<SaleItem, String> colMed = new TableColumn<>("اسم الدواء");
        colMed.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getMedicineName()));
        colMed.setPrefWidth(220);

        TableColumn<SaleItem, String> colBatch = new TableColumn<>("التشغيلة المخصومة (FEFO Batch)");
        colBatch.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getBatchNumber()));
        colBatch.setPrefWidth(180);

        TableColumn<SaleItem, Number> colQty = new TableColumn<>("الكمية المصروفة");
        colQty.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getQuantity()));
        colQty.setPrefWidth(110);

        TableColumn<SaleItem, String> colPrice = new TableColumn<>("سعر الوحدة");
        colPrice.setCellValueFactory(d -> new SimpleStringProperty(String.format("%.2f ج.م", d.getValue().getUnitPriceEgp())));
        colPrice.setPrefWidth(110);

        TableColumn<SaleItem, String> colLine = new TableColumn<>("إجمالي البند");
        colLine.setCellValueFactory(d -> new SimpleStringProperty(String.format("%.2f ج.م", d.getValue().getLineTotalEgp())));
        colLine.setPrefWidth(120);

        table.getColumns().addAll(colMed, colBatch, colQty, colPrice, colLine);
        table.setPrefWidth(780);
        table.setPrefHeight(300);

        try {
            List<SaleItem> items = salesService.getDetails(sale.getSaleId());
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
        alert.setTitle("تنبيه");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
