package com.pharmacy.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class Sale {
    private int saleId;
    private String saleDate;
    private long totalAmountPiasters;
    private String paymentMethod; // 'CASH' or 'CARD'
    
    private List<SaleItem> items = new ArrayList<>();

    public Sale() {}

    public Sale(int saleId, String saleDate, long totalAmountPiasters, String paymentMethod) {
        this.saleId = saleId;
        this.saleDate = saleDate;
        this.totalAmountPiasters = totalAmountPiasters;
        this.paymentMethod = paymentMethod;
    }

    public int getSaleId() { return saleId; }
    public void setSaleId(int saleId) { this.saleId = saleId; }

    public String getSaleDate() { return saleDate; }
    public void setSaleDate(String saleDate) { this.saleDate = saleDate; }

    public long getTotalAmountPiasters() { return totalAmountPiasters; }
    public void setTotalAmountPiasters(long totalAmountPiasters) { this.totalAmountPiasters = totalAmountPiasters; }

    public BigDecimal getTotalAmountEgp() {
        return BigDecimal.valueOf(totalAmountPiasters, 2);
    }

    public void setTotalAmountEgp(BigDecimal egp) {
        if (egp != null) {
            this.totalAmountPiasters = egp.movePointRight(2).longValueExact();
        }
    }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public List<SaleItem> getItems() { return items; }
    public void setItems(List<SaleItem> items) { this.items = items; }
}
