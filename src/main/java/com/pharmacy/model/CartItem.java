package com.pharmacy.model;

import java.math.BigDecimal;

public class CartItem {
    private Medicine medicine;
    private int quantity;
    private long unitPricePiasters;

    public CartItem(Medicine medicine, int quantity) {
        this.medicine = medicine;
        this.quantity = quantity;
        this.unitPricePiasters = medicine.getSalePricePiasters();
    }

    public Medicine getMedicine() { return medicine; }
    public void setMedicine(Medicine medicine) { this.medicine = medicine; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public long getUnitPricePiasters() { return unitPricePiasters; }
    public void setUnitPricePiasters(long unitPricePiasters) { this.unitPricePiasters = unitPricePiasters; }

    public long getSubtotalPiasters() {
        return Math.multiplyExact(unitPricePiasters, (long) quantity);
    }

    public BigDecimal getUnitPriceEgp() {
        return BigDecimal.valueOf(unitPricePiasters, 2);
    }

    public BigDecimal getSubtotalEgp() {
        return BigDecimal.valueOf(getSubtotalPiasters(), 2);
    }

    public String getMedicineName() {
        return medicine.getMedicineName();
    }
}
