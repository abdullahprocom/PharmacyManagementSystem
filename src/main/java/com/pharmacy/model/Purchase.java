package com.pharmacy.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class Purchase {
    private int purchaseId;
    private int supplierId;
    private String supplierName; // Joined for display
    private String purchaseDate;
    private String invoiceNumber;
    private long totalAmountPiasters;
    
    private List<PurchaseItem> items = new ArrayList<>();

    public Purchase() {}

    public Purchase(int purchaseId, int supplierId, String purchaseDate, String invoiceNumber, long totalAmountPiasters) {
        this.purchaseId = purchaseId;
        this.supplierId = supplierId;
        this.purchaseDate = purchaseDate;
        this.invoiceNumber = invoiceNumber;
        this.totalAmountPiasters = totalAmountPiasters;
    }

    public int getPurchaseId() { return purchaseId; }
    public void setPurchaseId(int purchaseId) { this.purchaseId = purchaseId; }

    public int getSupplierId() { return supplierId; }
    public void setSupplierId(int supplierId) { this.supplierId = supplierId; }

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

    public String getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(String purchaseDate) { this.purchaseDate = purchaseDate; }

    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }

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

    public List<PurchaseItem> getItems() { return items; }
    public void setItems(List<PurchaseItem> items) { this.items = items; }
}
