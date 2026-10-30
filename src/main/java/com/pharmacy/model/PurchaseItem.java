package com.pharmacy.model;

import java.math.BigDecimal;

public class PurchaseItem {
    private int purchaseItemId;
    private int purchaseId;
    private int medicineId;
    private String medicineName; // Joined for display
    private String batchNumber;
    private String expiryDate; // YYYY-MM-DD
    private int quantityReceived;
    private int quantityRemaining;
    private long unitCostPiasters;

    public PurchaseItem() {}

    public PurchaseItem(int purchaseItemId, int purchaseId, int medicineId, String batchNumber, 
                        String expiryDate, int quantityReceived, int quantityRemaining, long unitCostPiasters) {
        this.purchaseItemId = purchaseItemId;
        this.purchaseId = purchaseId;
        this.medicineId = medicineId;
        this.batchNumber = batchNumber;
        this.expiryDate = expiryDate;
        this.quantityReceived = quantityReceived;
        this.quantityRemaining = quantityRemaining;
        this.unitCostPiasters = unitCostPiasters;
    }

    public int getPurchaseItemId() { return purchaseItemId; }
    public void setPurchaseItemId(int purchaseItemId) { this.purchaseItemId = purchaseItemId; }

    public int getPurchaseId() { return purchaseId; }
    public void setPurchaseId(int purchaseId) { this.purchaseId = purchaseId; }

    public int getMedicineId() { return medicineId; }
    public void setMedicineId(int medicineId) { this.medicineId = medicineId; }

    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }

    public String getBatchNumber() { return batchNumber; }
    public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }

    public String getExpiryDate() { return expiryDate; }
    public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }

    public int getQuantityReceived() { return quantityReceived; }
    public void setQuantityReceived(int quantityReceived) { this.quantityReceived = quantityReceived; }

    public int getQuantityRemaining() { return quantityRemaining; }
    public void setQuantityRemaining(int quantityRemaining) { this.quantityRemaining = quantityRemaining; }

    public long getUnitCostPiasters() { return unitCostPiasters; }
    public void setUnitCostPiasters(long unitCostPiasters) { this.unitCostPiasters = unitCostPiasters; }

    public BigDecimal getUnitCostEgp() {
        return BigDecimal.valueOf(unitCostPiasters, 2);
    }

    public void setUnitCostEgp(BigDecimal egp) {
        if (egp != null) {
            this.unitCostPiasters = egp.movePointRight(2).longValueExact();
        }
    }

    public BigDecimal getSubtotalEgp() {
        return getUnitCostEgp().multiply(BigDecimal.valueOf(quantityReceived));
    }
}
