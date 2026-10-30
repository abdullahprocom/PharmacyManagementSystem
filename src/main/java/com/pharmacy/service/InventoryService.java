package com.pharmacy.service;

import com.pharmacy.dao.MedicineDAO;
import com.pharmacy.model.Medicine;

import java.sql.SQLException;
import java.util.List;

public class InventoryService {

    public static final List<String> FIXED_CATEGORIES = List.of(
        "Painkillers",
        "Antibiotics",
        "Vitamins",
        "Cold and Flu",
        "Skincare",
        "Other"
    );

    public List<String> getAllowedCategories() {
        return FIXED_CATEGORIES;
    }

    private final MedicineDAO medicineDAO = new MedicineDAO();

    public List<Medicine> getMedicines(String query, String category, String status) throws SQLException {
        return medicineDAO.getMedicines(query, category, status);
    }

    public void saveMedicine(Medicine medicine, boolean isNew) throws Exception {
        if (medicine.getMedicineName() == null || medicine.getMedicineName().trim().isBlank()) {
            throw new IllegalArgumentException("Medicine name is required.");
        }
        if (medicine.getCategory() == null || medicine.getCategory().isBlank()) {
            throw new IllegalArgumentException("Category selection is required.");
        }
        if (medicine.getSalePricePiasters() < 0) {
            throw new IllegalArgumentException("Sale price cannot be negative.");
        }
        if (medicine.getReorderLevel() < 0) {
            throw new IllegalArgumentException("Reorder level cannot be negative.");
        }

        String barcode = medicine.getBarcode();
        if (barcode != null && !barcode.isBlank()) {
            barcode = barcode.trim();
            Integer excludeId = isNew ? null : medicine.getMedicineId();
            if (!medicineDAO.isBarcodeUnique(barcode, excludeId)) {
                throw new IllegalArgumentException("Barcode '" + barcode + "' is already registered to another medicine.");
            }
        }

        if (isNew) {
            medicineDAO.insertMedicine(medicine);
        } else {
            medicineDAO.updateMedicine(medicine);
        }
    }

    public void toggleStatus(int medicineId, boolean newActive) throws SQLException {
        medicineDAO.setStatus(medicineId, newActive);
    }

    public List<Medicine> getActiveSellableMedicines() throws SQLException {
        return medicineDAO.getActiveSellableMedicines();
    }
}
