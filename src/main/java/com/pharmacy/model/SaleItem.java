package com.pharmacy.model;

import java.math.BigDecimal;

public class SaleItem {
    private int saleItemId;
    private int saleId;
    private int purchaseItemId;
    private String medicineName; // Joined for display
    private String batchNumber;  // Joined for display
    private int quantity;
    private long unitPricePiasters;
    private long lineTotalPiasters;

    public SaleItem() {}

    public SaleItem(int saleItemId, int saleId, int purchaseItemId, int quantity, 
                    long unitPricePiasters, long lineTotalPiasters) {
        this.saleItemId = saleItemId;
        this.saleId = saleId;
        this.purchaseItemId = purchaseItemId;
        this.quantity = quantity;
        this.unitPricePiasters = unitPricePiasters;
        this.lineTotalPiasters = lineTotalPiasters;
    }

    public int getSaleItemId() { return saleItemId; }
    public void setSaleItemId(int saleItemId) { this.saleItemId = saleItemId; }

    public int getSaleId() { return saleId; }
    public void setSaleId(int saleId) { this.saleId = saleId; }

    public int getPurchaseItemId() { return purchaseItemId; }
    public void setPurchaseItemId(int purchaseItemId) { this.purchaseItemId = purchaseItemId; }

    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }

    public String getBatchNumber() { return batchNumber; }
    public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public long getUnitPricePiasters() { return unitPricePiasters; }
    public void setUnitPricePiasters(long unitPricePiasters) { this.unitPricePiasters = unitPricePiasters; }

    public long getLineTotalPiasters() { return lineTotalPiasters; }
    public void setLineTotalPiasters(long lineTotalPiasters) { this.lineTotalPiasters = lineTotalPiasters; }

    public BigDecimal getUnitPriceEgp() {
        return BigDecimal.valueOf(unitPricePiasters, 2);
    }

    public BigDecimal getLineTotalEgp() {
        return BigDecimal.valueOf(lineTotalPiasters, 2);
    }
}
