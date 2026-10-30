package com.pharmacy.dao;

import com.pharmacy.database.Database;
import com.pharmacy.model.Supplier;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SupplierDAO {

    public List<Supplier> getSuppliers(String searchQuery, Boolean activeOnly) throws SQLException {
        List<Supplier> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM suppliers WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (searchQuery != null && !searchQuery.isBlank()) {
            sql.append(" AND (supplier_name LIKE ? OR phone LIKE ?)");
            String pattern = "%" + searchQuery.trim() + "%";
            params.add(pattern);
            params.add(pattern);
        }

        if (activeOnly != null) {
            sql.append(" AND is_active = ?");
            params.add(activeOnly ? 1 : 0);
        }

        sql.append(" ORDER BY supplier_name ASC;");

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Supplier(
                        rs.getInt("supplier_id"),
                        rs.getString("supplier_name"),
                        rs.getString("phone"),
                        rs.getString("address"),
                        rs.getInt("is_active") == 1
                    ));
                }
            }
        }
        return list;
    }

    public boolean insertSupplier(Supplier s) throws SQLException {
        String sql = "INSERT INTO suppliers (supplier_name, phone, address, is_active) VALUES (?, ?, ?, ?);";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, s.getSupplierName().trim());
            ps.setString(2, s.getPhone());
            ps.setString(3, s.getAddress());
            ps.setInt(4, s.isActive() ? 1 : 0);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean updateSupplier(Supplier s) throws SQLException {
        String sql = "UPDATE suppliers SET supplier_name = ?, phone = ?, address = ? WHERE supplier_id = ?;";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, s.getSupplierName().trim());
            ps.setString(2, s.getPhone());
            ps.setString(3, s.getAddress());
            ps.setInt(4, s.getSupplierId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean setStatus(int supplierId, boolean active) throws SQLException {
        String sql = "UPDATE suppliers SET is_active = ? WHERE supplier_id = ?;";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, active ? 1 : 0);
            ps.setInt(2, supplierId);
            return ps.executeUpdate() > 0;
        }
    }
}
