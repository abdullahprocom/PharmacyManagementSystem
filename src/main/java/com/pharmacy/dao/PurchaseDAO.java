package com.pharmacy.dao;

import com.pharmacy.database.Database;
import com.pharmacy.model.Purchase;
import com.pharmacy.model.PurchaseItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PurchaseDAO {

    public boolean savePurchase(Purchase purchase) throws SQLException {
        String insertPurchaseSql = """
            INSERT INTO purchases (supplier_id, purchase_date, invoice_number, total_amount_piasters)
            VALUES (?, ?, ?, ?);
        """;

        String insertItemSql = """
            INSERT INTO purchase_items (purchase_id, medicine_id, batch_number, expiry_date, quantity_received, quantity_remaining, unit_cost_piasters)
            VALUES (?, ?, ?, ?, ?, ?, ?);
        """;

        Connection conn = null;
        try {
            conn = Database.getConnection();
            conn.setAutoCommit(false); // Atomic Transaction

            int purchaseId;
            try (PreparedStatement ps = conn.prepareStatement(insertPurchaseSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, purchase.getSupplierId());
                ps.setString(2, purchase.getPurchaseDate());
                ps.setString(3, purchase.getInvoiceNumber());
                ps.setLong(4, purchase.getTotalAmountPiasters());
                ps.executeUpdate();

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        purchaseId = rs.getInt(1);
                        purchase.setPurchaseId(purchaseId);
                    } else {
                        throw new SQLException("Failed to generate purchase ID.");
                    }
                }
            }

            try (PreparedStatement psItem = conn.prepareStatement(insertItemSql)) {
                for (PurchaseItem item : purchase.getItems()) {
                    psItem.setInt(1, purchaseId);
                    psItem.setInt(2, item.getMedicineId());
                    psItem.setString(3, item.getBatchNumber().trim());
                    psItem.setString(4, item.getExpiryDate());
                    psItem.setInt(5, item.getQuantityReceived());
                    psItem.setInt(6, item.getQuantityReceived()); // quantity_remaining = quantity_received initially
                    psItem.setLong(7, item.getUnitCostPiasters());
                    psItem.addBatch();
                }
                psItem.executeBatch();
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

    public List<Purchase> getPurchasesHistory(String startDate, String endDate) throws SQLException {
        List<Purchase> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
            SELECT p.purchase_id, p.supplier_id, s.supplier_name, p.purchase_date, p.invoice_number, p.total_amount_piasters
            FROM purchases p
            JOIN suppliers s ON p.supplier_id = s.supplier_id
            WHERE 1=1
        """);

        List<Object> params = new ArrayList<>();
        if (startDate != null && !startDate.isBlank()) {
            sql.append(" AND p.purchase_date >= ?");
            params.add(startDate);
        }
        if (endDate != null && !endDate.isBlank()) {
            sql.append(" AND p.purchase_date <= ?");
            params.add(endDate);
        }
        sql.append(" ORDER BY p.purchase_id DESC;");

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Purchase p = new Purchase(
                        rs.getInt("purchase_id"),
                        rs.getInt("supplier_id"),
                        rs.getString("purchase_date"),
                        rs.getString("invoice_number"),
                        rs.getLong("total_amount_piasters")
                    );
                    p.setSupplierName(rs.getString("supplier_name"));
                    list.add(p);
                }
            }
        }
        return list;
    }

    public List<PurchaseItem> getPurchaseDetails(int purchaseId) throws SQLException {
        List<PurchaseItem> list = new ArrayList<>();
        String sql = """
            SELECT pi.purchase_item_id, pi.purchase_id, pi.medicine_id, m.medicine_name, 
                   pi.batch_number, pi.expiry_date, pi.quantity_received, pi.quantity_remaining, pi.unit_cost_piasters
            FROM purchase_items pi
            JOIN medicines m ON pi.medicine_id = m.medicine_id
            WHERE pi.purchase_id = ?
            ORDER BY pi.purchase_item_id ASC;
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, purchaseId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PurchaseItem item = new PurchaseItem(
                        rs.getInt("purchase_item_id"),
                        rs.getInt("purchase_id"),
                        rs.getInt("medicine_id"),
                        rs.getString("batch_number"),
                        rs.getString("expiry_date"),
                        rs.getInt("quantity_received"),
                        rs.getInt("quantity_remaining"),
                        rs.getLong("unit_cost_piasters")
                    );
                    item.setMedicineName(rs.getString("medicine_name"));
                    list.add(item);
                }
            }
        }
        return list;
    }
}
