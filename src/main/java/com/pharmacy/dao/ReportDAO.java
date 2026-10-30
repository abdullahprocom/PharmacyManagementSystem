package com.pharmacy.dao;

import com.pharmacy.database.Database;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ReportDAO {

    public static class DashboardKPIs {
        private final int totalMedicines;
        private final int totalSellableStock;
        private final BigDecimal todaySalesEgp;
        private final int lowStockCount;

        public DashboardKPIs(int totalMedicines, int totalSellableStock, BigDecimal todaySalesEgp, int lowStockCount) {
            this.totalMedicines = totalMedicines;
            this.totalSellableStock = totalSellableStock;
            this.todaySalesEgp = todaySalesEgp;
            this.lowStockCount = lowStockCount;
        }

        public int totalMedicines() { return totalMedicines; }
        public int totalSellableStock() { return totalSellableStock; }
        public BigDecimal todaySalesEgp() { return todaySalesEgp; }
        public int lowStockCount() { return lowStockCount; }
    }

    public static class BatchAlert {
        private final String medicineName;
        private final String batchNumber;
        private final String expiryDate;
        private final int remainingQuantity;
        private final String status;

        public BatchAlert(String medicineName, String batchNumber, String expiryDate, int remainingQuantity, String status) {
            this.medicineName = medicineName;
            this.batchNumber = batchNumber;
            this.expiryDate = expiryDate;
            this.remainingQuantity = remainingQuantity;
            this.status = status;
        }

        public String medicineName() { return medicineName; }
        public String batchNumber() { return batchNumber; }
        public String expiryDate() { return expiryDate; }
        public int remainingQuantity() { return remainingQuantity; }
        public String status() { return status; }
    }

    public static class LowStockItem {
        private final String medicineName;
        private final int sellableStock;
        private final int reorderLevel;

        public LowStockItem(String medicineName, int sellableStock, int reorderLevel) {
            this.medicineName = medicineName;
            this.sellableStock = sellableStock;
            this.reorderLevel = reorderLevel;
        }

        public String medicineName() { return medicineName; }
        public int sellableStock() { return sellableStock; }
        public int reorderLevel() { return reorderLevel; }
    }

    public DashboardKPIs getDashboardKPIs() throws SQLException {
        String today = LocalDate.now().toString();
        int totalMedicines = 0;
        int totalSellableStock = 0;
        long todaySalesPiasters = 0;
        int lowStockCount = 0;

        try (Connection conn = Database.getConnection()) {
            // Total Active Medicines
            try (Statement s = conn.createStatement();
                 ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM medicines WHERE is_active = 1;")) {
                if (rs.next()) totalMedicines = rs.getInt(1);
            }

            // Total Sellable Stock across all active unexpired batches
            try (PreparedStatement ps = conn.prepareStatement("""
                SELECT COALESCE(SUM(pi.quantity_remaining), 0)
                FROM purchase_items pi
                JOIN medicines m ON pi.medicine_id = m.medicine_id
                WHERE m.is_active = 1
                  AND pi.expiry_date >= ?
                  AND pi.quantity_remaining > 0;
            """)) {
                ps.setString(1, today);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) totalSellableStock = rs.getInt(1);
                }
            }

            // Today's Sales Total
            try (PreparedStatement ps = conn.prepareStatement("""
                SELECT COALESCE(SUM(total_amount_piasters), 0)
                FROM sales
                WHERE substr(sale_date, 1, 10) = ?;
            """)) {
                ps.setString(1, today);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) todaySalesPiasters = rs.getLong(1);
                }
            }

            // Low Stock Medicines Count
            try (PreparedStatement ps = conn.prepareStatement("""
                SELECT COUNT(*)
                FROM medicines m
                WHERE m.is_active = 1
                  AND m.reorder_level >= COALESCE((
                      SELECT SUM(pi.quantity_remaining)
                      FROM purchase_items pi
                      WHERE pi.medicine_id = m.medicine_id
                        AND pi.expiry_date >= ?
                        AND pi.quantity_remaining > 0
                  ), 0);
            """)) {
                ps.setString(1, today);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) lowStockCount = rs.getInt(1);
                }
            }
        }

        return new DashboardKPIs(
            totalMedicines,
            totalSellableStock,
            BigDecimal.valueOf(todaySalesPiasters, 2),
            lowStockCount
        );
    }

    public List<LowStockItem> getLowStockMedicines() throws SQLException {
        List<LowStockItem> list = new ArrayList<>();
        String today = LocalDate.now().toString();

        String sql = """
            SELECT m.medicine_name, m.reorder_level,
                   COALESCE((
                       SELECT SUM(pi.quantity_remaining)
                       FROM purchase_items pi
                       WHERE pi.medicine_id = m.medicine_id
                         AND pi.expiry_date >= ?
                         AND pi.quantity_remaining > 0
                   ), 0) AS stock
            FROM medicines m
            WHERE m.is_active = 1
              AND m.reorder_level >= COALESCE((
                  SELECT SUM(pi.quantity_remaining)
                  FROM purchase_items pi
                  WHERE pi.medicine_id = m.medicine_id
                    AND pi.expiry_date >= ?
                    AND pi.quantity_remaining > 0
              ), 0)
            ORDER BY stock ASC;
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, today);
            ps.setString(2, today);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new LowStockItem(
                        rs.getString("medicine_name"),
                        rs.getInt("stock"),
                        rs.getInt("reorder_level")
                    ));
                }
            }
        }
        return list;
    }

    public List<BatchAlert> getExpirationAlerts(int maxDaysAhead) throws SQLException {
        List<BatchAlert> list = new ArrayList<>();
        String today = LocalDate.now().toString();
        String limitDate = LocalDate.now().plusDays(maxDaysAhead).toString();

        String sql = """
            SELECT m.medicine_name, pi.batch_number, pi.expiry_date, pi.quantity_remaining
            FROM purchase_items pi
            JOIN medicines m ON pi.medicine_id = m.medicine_id
            WHERE pi.quantity_remaining > 0
              AND pi.expiry_date <= ?
            ORDER BY pi.expiry_date ASC;
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, limitDate);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String exp = rs.getString("expiry_date");
                    String status = exp.compareTo(today) < 0 ? "EXPIRED" : "EXPIRING_SOON";
                    list.add(new BatchAlert(
                        rs.getString("medicine_name"),
                        rs.getString("batch_number"),
                        exp,
                        rs.getInt("quantity_remaining"),
                        status
                    ));
                }
            }
        }
        return list;
    }

    public Map<String, BigDecimal> getLast7DaysSalesChartData() throws SQLException {
        Map<String, BigDecimal> chartMap = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();

        // Initialize 7 days with zero
        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            chartMap.put(d.toString(), BigDecimal.ZERO);
        }

        String sql = """
            SELECT substr(sale_date, 1, 10) as sdate, SUM(total_amount_piasters) as daily_total
            FROM sales
            WHERE substr(sale_date, 1, 10) >= ?
            GROUP BY substr(sale_date, 1, 10)
            ORDER BY sdate ASC;
        """;

        String sevenDaysAgo = today.minusDays(6).toString();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sevenDaysAgo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String d = rs.getString("sdate");
                    long piasters = rs.getLong("daily_total");
                    if (chartMap.containsKey(d)) {
                        chartMap.put(d, BigDecimal.valueOf(piasters, 2));
                    }
                }
            }
        }
        return chartMap;
    }
}
