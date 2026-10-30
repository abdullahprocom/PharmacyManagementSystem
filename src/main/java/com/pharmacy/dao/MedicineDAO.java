package com.pharmacy.dao;

import com.pharmacy.database.Database;
import com.pharmacy.model.Medicine;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MedicineDAO {

    public List<Medicine> getMedicines(String searchQuery, String categoryFilter, String statusFilter) throws SQLException {
        List<Medicine> list = new ArrayList<>();
        String today = LocalDate.now().toString();

        StringBuilder sql = new StringBuilder("""
            SELECT m.medicine_id, m.medicine_name, m.category, m.barcode, 
                   m.sale_price_piasters, m.reorder_level, m.is_active,
                   COALESCE((
                       SELECT SUM(pi.quantity_remaining) 
                       FROM purchase_items pi 
                       WHERE pi.medicine_id = m.medicine_id 
                         AND pi.expiry_date >= ? 
                         AND pi.quantity_remaining > 0
                   ), 0) AS sellable_stock
            FROM medicines m
            WHERE 1=1
        """);

        List<Object> params = new ArrayList<>();
        params.add(today);

        if (searchQuery != null && !searchQuery.isBlank()) {
            sql.append(" AND (m.medicine_name LIKE ? OR m.barcode LIKE ?)");
            String pattern = "%" + searchQuery.trim() + "%";
            params.add(pattern);
            params.add(pattern);
        }

        if (categoryFilter != null && !categoryFilter.isBlank() && !categoryFilter.equalsIgnoreCase("All")) {
            sql.append(" AND m.category = ?");
            params.add(categoryFilter);
        }

        if (statusFilter != null && !statusFilter.isBlank()) {
            if (statusFilter.equalsIgnoreCase("Active")) {
                sql.append(" AND m.is_active = 1");
            } else if (statusFilter.equalsIgnoreCase("Inactive")) {
                sql.append(" AND m.is_active = 0");
            }
        }

        sql.append(" ORDER BY m.medicine_name ASC;");

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Medicine m = new Medicine(
                        rs.getInt("medicine_id"),
                        rs.getString("medicine_name"),
                        rs.getString("category"),
                        rs.getString("barcode"),
                        rs.getLong("sale_price_piasters"),
                        rs.getInt("reorder_level"),
                        rs.getInt("is_active") == 1
                    );
                    m.setSellableStock(rs.getInt("sellable_stock"));
                    list.add(m);
                }
            }
        }
        return list;
    }

    public boolean insertMedicine(Medicine m) throws SQLException {
        String sql = """
            INSERT INTO medicines (medicine_name, category, barcode, sale_price_piasters, reorder_level, is_active)
            VALUES (?, ?, ?, ?, ?, ?);
        """;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, m.getMedicineName().trim());
            ps.setString(2, m.getCategory());
            
            String barcode = (m.getBarcode() == null || m.getBarcode().isBlank()) ? null : m.getBarcode().trim();
            if (barcode == null) {
                ps.setNull(3, Types.VARCHAR);
            } else {
                ps.setString(3, barcode);
            }

            ps.setLong(4, m.getSalePricePiasters());
            ps.setInt(5, m.getReorderLevel());
            ps.setInt(6, m.isActive() ? 1 : 0);

            return ps.executeUpdate() > 0;
        }
    }

    public boolean updateMedicine(Medicine m) throws SQLException {
        String sql = """
            UPDATE medicines 
            SET medicine_name = ?, category = ?, barcode = ?, sale_price_piasters = ?, reorder_level = ?
            WHERE medicine_id = ?;
        """;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, m.getMedicineName().trim());
            ps.setString(2, m.getCategory());
            
            String barcode = (m.getBarcode() == null || m.getBarcode().isBlank()) ? null : m.getBarcode().trim();
            if (barcode == null) {
                ps.setNull(3, Types.VARCHAR);
            } else {
                ps.setString(3, barcode);
            }

            ps.setLong(4, m.getSalePricePiasters());
            ps.setInt(5, m.getReorderLevel());
            ps.setInt(6, m.getMedicineId());

            return ps.executeUpdate() > 0;
        }
    }

    public boolean setStatus(int medicineId, boolean active) throws SQLException {
        String sql = "UPDATE medicines SET is_active = ? WHERE medicine_id = ?;";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, active ? 1 : 0);
            ps.setInt(2, medicineId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean isBarcodeUnique(String barcode, Integer excludeMedicineId) throws SQLException {
        if (barcode == null || barcode.isBlank()) return true;
        
        String sql = "SELECT COUNT(*) FROM medicines WHERE barcode = ?" +
                     (excludeMedicineId != null ? " AND medicine_id != ?" : "") + ";";
        
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, barcode.trim());
            if (excludeMedicineId != null) {
                ps.setInt(2, excludeMedicineId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) == 0;
                }
            }
        }
        return true;
    }

    public List<Medicine> getActiveSellableMedicines() throws SQLException {
        List<Medicine> all = getMedicines("", "All", "Active");
        List<Medicine> sellable = new ArrayList<>();
        for (Medicine m : all) {
            if (m.getSellableStock() > 0) {
                sellable.add(m);
            }
        }
        return sellable;
    }
}
