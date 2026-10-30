package com.pharmacy.service;

import com.pharmacy.dao.PurchaseDAO;
import com.pharmacy.model.Purchase;
import com.pharmacy.model.PurchaseItem;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class PurchaseService {

    private final PurchaseDAO purchaseDAO = new PurchaseDAO();

    public void processPurchase(Purchase purchase) throws Exception {
        if (purchase.getSupplierId() <= 0) {
            throw new IllegalArgumentException("A valid supplier must be selected.");
        }
        if (purchase.getPurchaseDate() == null || purchase.getPurchaseDate().isBlank()) {
            throw new IllegalArgumentException("Purchase date is required.");
        }
        if (purchase.getItems() == null || purchase.getItems().isEmpty()) {
            throw new IllegalArgumentException("Purchase must contain at least one medicine item.");
        }

        String today = LocalDate.now().toString();
        long totalPiasters = 0;

        for (PurchaseItem item : purchase.getItems()) {
            if (item.getMedicineId() <= 0) {
                throw new IllegalArgumentException("All purchase items must have a valid medicine selected.");
            }
            if (item.getBatchNumber() == null || item.getBatchNumber().trim().isBlank()) {
                throw new IllegalArgumentException("Batch number is required for all received items.");
            }
            if (item.getExpiryDate() == null || item.getExpiryDate().isBlank()) {
                throw new IllegalArgumentException("Expiration date is required for batch: " + item.getBatchNumber());
            }
            if (item.getExpiryDate().compareTo(today) < 0) {
                throw new IllegalArgumentException("Expiration date cannot be in the past for batch: " 
                    + item.getBatchNumber() + " (" + item.getExpiryDate() + ").");
            }
            if (item.getQuantityReceived() <= 0) {
                throw new IllegalArgumentException("Quantity must be greater than zero for batch: " + item.getBatchNumber());
            }
            if (item.getUnitCostPiasters() < 0) {
                throw new IllegalArgumentException("Unit cost cannot be negative for batch: " + item.getBatchNumber());
            }

            long lineTotal = Math.multiplyExact(item.getUnitCostPiasters(), (long) item.getQuantityReceived());
            totalPiasters = Math.addExact(totalPiasters, lineTotal);
        }

        purchase.setTotalAmountPiasters(totalPiasters);
        purchaseDAO.savePurchase(purchase);
    }

    public List<Purchase> getHistory(String start, String end) throws SQLException {
        return purchaseDAO.getPurchasesHistory(start, end);
    }

    public List<PurchaseItem> getDetails(int purchaseId) throws SQLException {
        return purchaseDAO.getPurchaseDetails(purchaseId);
    }
}
