package com.pharmacy.dao;

import com.pharmacy.database.Database;
import com.pharmacy.model.CartItem;
import com.pharmacy.model.Sale;
import com.pharmacy.model.SaleItem;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class SalesDAO {

    /**
     * Executes a complete sale transaction with FEFO batch allocation.
     * All operations occur in ONE atomic database transaction.
     */
    public boolean processSale(Sale sale, List<CartItem> cart) throws SQLException {
        String today = LocalDate.now().toString();

        String insertSaleSql = """
            INSERT INTO sales (sale_date, total_amount_piasters, payment_method)
            VALUES (?, ?, ?);
        """;

        String findBatchesSql = """
            SELECT purchase_item_id, quantity_remaining, expiry_date
            FROM purchase_items
            WHERE medicine_id = ?
              AND quantity_remaining > 0
              AND expiry_date >= ?
            ORDER BY expiry_date ASC, purchase_item_id ASC;
        """;

        String guardedDeductionSql = """
            UPDATE purchase_items
            SET quantity_remaining = quantity_remaining - ?
            WHERE purchase_item_id = ?
              AND quantity_remaining >= ?
              AND expiry_date >= ?;
        """;

        String insertSaleItemSql = """
            INSERT INTO sale_items (sale_id, purchase_item_id, quantity, unit_price_piasters, line_total_piasters)
            VALUES (?, ?, ?, ?, ?);
        """;

        Connection conn = null;
        try {
            conn = Database.getConnection();
            conn.setAutoCommit(false); // Atomic Transaction

            // 1. Insert Sales Header
            int saleId;
            try (PreparedStatement ps = conn.prepareStatement(insertSaleSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, sale.getSaleDate());
                ps.setLong(2, sale.getTotalAmountPiasters());
                ps.setString(3, sale.getPaymentMethod());
                ps.executeUpdate();

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        saleId = rs.getInt(1);
                        sale.setSaleId(saleId);
                    } else {
                        throw new SQLException("Failed to generate sales ID.");
                    }
                }
            }

            // 2. Process each cart item through FEFO
            for (CartItem cartItem : cart) {
                int requiredQty = cartItem.getQuantity();
                int medicineId = cartItem.getMedicine().getMedicineId();
                long unitPrice = cartItem.getUnitPricePiasters();

                // Fetch unexpired batches ordered by earliest expiry date (FEFO)
                List<BatchCandidate> batches = new ArrayList<>();
                try (PreparedStatement psBatch = conn.prepareStatement(findBatchesSql)) {
                    psBatch.setInt(1, medicineId);
                    psBatch.setString(2, today);
                    try (ResultSet rs = psBatch.executeQuery()) {
                        while (rs.next()) {
                            batches.add(new BatchCandidate(
                                rs.getInt("purchase_item_id"),
                                rs.getInt("quantity_remaining"),
                                rs.getString("expiry_date")
                            ));
                        }
                    }
                }

                // Verify sufficient unexpired stock
                int totalAvailable = batches.stream().mapToInt(b -> b.qtyRemaining).sum();
                if (totalAvailable < requiredQty) {
                    throw new SQLException("Insufficient unexpired stock for medicine: " + cartItem.getMedicineName() 
                        + ". Requested: " + requiredQty + ", Available: " + totalAvailable);
                }

                // Deduct from batches using FEFO order
                int remainingToDeduct = requiredQty;
                for (BatchCandidate batch : batches) {
                    if (remainingToDeduct <= 0) break;

                    int takeQty = Math.min(remainingToDeduct, batch.qtyRemaining);

                    // Execute Guarded Stock Deduction
                    try (PreparedStatement psDeduct = conn.prepareStatement(guardedDeductionSql)) {
                        psDeduct.setInt(1, takeQty);
                        psDeduct.setInt(2, batch.purchaseItemId);
                        psDeduct.setInt(3, takeQty);
                        psDeduct.setString(4, today);

                        int affectedRows = psDeduct.executeUpdate();
                        if (affectedRows != 1) {
                            throw new SQLException("Concurrency stock check failed for batch ID: " + batch.purchaseItemId 
                                + " (affected rows != 1). Sale rolled back.");
                        }
                    }

                    // Insert Sale Item record linking to this specific batch
                    long lineTotal = Math.multiplyExact(unitPrice, (long) takeQty);
                    try (PreparedStatement psSaleItem = conn.prepareStatement(insertSaleItemSql)) {
                        psSaleItem.setInt(1, saleId);
                        psSaleItem.setInt(2, batch.purchaseItemId);
                        psSaleItem.setInt(3, takeQty);
                        psSaleItem.setLong(4, unitPrice);
                        psSaleItem.setLong(5, lineTotal);
                        psSaleItem.executeUpdate();
                    }

                    remainingToDeduct -= takeQty;
                }

                if (remainingToDeduct > 0) {
                    throw new SQLException("FEFO deduction allocation failed for medicine: " + cartItem.getMedicineName());
                }
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    public List<Sale> getSalesHistory(String startDate, String endDate) throws SQLException {
        List<Sale> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
            SELECT sale_id, sale_date, total_amount_piasters, payment_method
            FROM sales
            WHERE 1=1
        """);

        List<Object> params = new ArrayList<>();
        if (startDate != null && !startDate.isBlank()) {
            sql.append(" AND sale_date >= ?");
            params.add(startDate);
        }
        if (endDate != null && !endDate.isBlank()) {
            sql.append(" AND sale_date <= ?");
            params.add(endDate);
        }
        sql.append(" ORDER BY sale_id DESC;");

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Sale(
                        rs.getInt("sale_id"),
                        rs.getString("sale_date"),
                        rs.getLong("total_amount_piasters"),
                        rs.getString("payment_method")
                    ));
                }
            }
        }
        return list;
    }

    public List<SaleItem> getSaleDetails(int saleId) throws SQLException {
        List<SaleItem> list = new ArrayList<>();
        String sql = """
            SELECT si.sale_item_id, si.sale_id, si.purchase_item_id, m.medicine_name, pi.batch_number,
                   si.quantity, si.unit_price_piasters, si.line_total_piasters
            FROM sale_items si
            JOIN purchase_items pi ON si.purchase_item_id = pi.purchase_item_id
            JOIN medicines m ON pi.medicine_id = m.medicine_id
            WHERE si.sale_id = ?
            ORDER BY si.sale_item_id ASC;
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, saleId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    SaleItem item = new SaleItem(
                        rs.getInt("sale_item_id"),
                        rs.getInt("sale_id"),
                        rs.getInt("purchase_item_id"),
                        rs.getInt("quantity"),
                        rs.getLong("unit_price_piasters"),
                        rs.getLong("line_total_piasters")
                    );
                    item.setMedicineName(rs.getString("medicine_name"));
                    item.setBatchNumber(rs.getString("batch_number"));
                    list.add(item);
                }
            }
        }
        return list;
    }

    private static class BatchCandidate {
        int purchaseItemId;
        int qtyRemaining;
        String expiryDate;

        BatchCandidate(int id, int qty, String exp) {
            this.purchaseItemId = id;
            this.qtyRemaining = qty;
            this.expiryDate = exp;
        }
    }
}
