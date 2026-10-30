package com.pharmacy.model;

import java.math.BigDecimal;

public class Medicine {
    private int medicineId;
    private String medicineName;
    private String category;
    private String barcode;
    private long salePricePiasters;
    private int reorderLevel;
    private boolean active;
    
    // Calculated field: sellable stock (sum of unexpired batches)
    private int sellableStock;

    public Medicine() {}

    public Medicine(int medicineId, String medicineName, String category, String barcode, 
                    long salePricePiasters, int reorderLevel, boolean active) {
        this.medicineId = medicineId;
        this.medicineName = medicineName;
        this.category = category;
        this.barcode = barcode;
        this.salePricePiasters = salePricePiasters;
        this.reorderLevel = reorderLevel;
        this.active = active;
    }

    public int getMedicineId() { return medicineId; }
    public void setMedicineId(int medicineId) { this.medicineId = medicineId; }

    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public long getSalePricePiasters() { return salePricePiasters; }
    public void setSalePricePiasters(long salePricePiasters) { this.salePricePiasters = salePricePiasters; }

    // Helper: returns sale price formatted as EGP with 2 decimals
    public BigDecimal getSalePriceEgp() {
        return BigDecimal.valueOf(salePricePiasters, 2);
    }

    public void setSalePriceEgp(BigDecimal egp) {
        if (egp != null) {
            this.salePricePiasters = egp.movePointRight(2).longValueExact();
        }
    }

    public int getReorderLevel() { return reorderLevel; }
    public void setReorderLevel(int reorderLevel) { this.reorderLevel = reorderLevel; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public int getSellableStock() { return sellableStock; }
    public void setSellableStock(int sellableStock) { this.sellableStock = sellableStock; }

    public boolean isLowStock() {
        return sellableStock <= reorderLevel;
    }

    @Override
    public String toString() {
        return medicineName + " (" + getSalePriceEgp() + " EGP)";
    }
}
