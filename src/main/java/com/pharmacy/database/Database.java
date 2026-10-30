package com.pharmacy.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {

    private static String dbUrl = null;

    public static synchronized String getDbUrl() {
        if (dbUrl == null) {
            String localAppData = System.getenv("LOCALAPPDATA");
            if (localAppData == null || localAppData.isBlank()) {
                localAppData = System.getProperty("user.home") + File.separator + "AppData" + File.separator + "Local";
            }
            File appDir = new File(localAppData, "PharmacyManagementSystem");
            if (!appDir.exists()) {
                appDir.mkdirs();
            }
            File dbFile = new File(appDir, "pharmacy.db");
            dbUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();
        }
        return dbUrl;
    }

    /**
     * Obtains a new database connection with foreign keys strictly enabled.
     */
    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(getDbUrl());
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
        }
        return conn;
    }

    /**
     * Initializes all six core database tables and indexes on application startup.
     */
    public static void initializeDatabase() {
        String[] ddlStatements = {
            """
            CREATE TABLE IF NOT EXISTS medicines (
                medicine_id INTEGER PRIMARY KEY AUTOINCREMENT,
                medicine_name TEXT NOT NULL,
                category TEXT NOT NULL,
                barcode TEXT UNIQUE,
                sale_price_piasters INTEGER NOT NULL CHECK (sale_price_piasters >= 0),
                reorder_level INTEGER NOT NULL DEFAULT 10 CHECK (reorder_level >= 0),
                is_active INTEGER NOT NULL DEFAULT 1 CHECK (is_active IN (0, 1))
            );
            """,
            """
            CREATE TABLE IF NOT EXISTS suppliers (
                supplier_id INTEGER PRIMARY KEY AUTOINCREMENT,
                supplier_name TEXT NOT NULL,
                phone TEXT,
                address TEXT,
                is_active INTEGER NOT NULL DEFAULT 1 CHECK (is_active IN (0, 1))
            );
            """,
            """
            CREATE TABLE IF NOT EXISTS purchases (
                purchase_id INTEGER PRIMARY KEY AUTOINCREMENT,
                supplier_id INTEGER NOT NULL,
                purchase_date TEXT NOT NULL,
                invoice_number TEXT,
                total_amount_piasters INTEGER NOT NULL DEFAULT 0 CHECK (total_amount_piasters >= 0),
                FOREIGN KEY (supplier_id) REFERENCES suppliers(supplier_id)
            );
            """,
            """
            CREATE TABLE IF NOT EXISTS purchase_items (
                purchase_item_id INTEGER PRIMARY KEY AUTOINCREMENT,
                purchase_id INTEGER NOT NULL,
                medicine_id INTEGER NOT NULL,
                batch_number TEXT NOT NULL,
                expiry_date TEXT NOT NULL,
                quantity_received INTEGER NOT NULL CHECK (quantity_received > 0),
                quantity_remaining INTEGER NOT NULL
                    CHECK (quantity_remaining >= 0 AND quantity_remaining <= quantity_received),
                unit_cost_piasters INTEGER NOT NULL CHECK (unit_cost_piasters >= 0),
                FOREIGN KEY (purchase_id) REFERENCES purchases(purchase_id),
                FOREIGN KEY (medicine_id) REFERENCES medicines(medicine_id)
            );
            """,
            """
            CREATE TABLE IF NOT EXISTS sales (
                sale_id INTEGER PRIMARY KEY AUTOINCREMENT,
                sale_date TEXT NOT NULL,
                total_amount_piasters INTEGER NOT NULL DEFAULT 0 CHECK (total_amount_piasters >= 0),
                payment_method TEXT NOT NULL CHECK (payment_method IN ('CASH', 'CARD'))
            );
            """,
            """
            CREATE TABLE IF NOT EXISTS sale_items (
                sale_item_id INTEGER PRIMARY KEY AUTOINCREMENT,
                sale_id INTEGER NOT NULL,
                purchase_item_id INTEGER NOT NULL,
                quantity INTEGER NOT NULL CHECK (quantity > 0),
                unit_price_piasters INTEGER NOT NULL CHECK (unit_price_piasters >= 0),
                line_total_piasters INTEGER NOT NULL CHECK (line_total_piasters >= 0),
                FOREIGN KEY (sale_id) REFERENCES sales(sale_id),
                FOREIGN KEY (purchase_item_id) REFERENCES purchase_items(purchase_item_id)
            );
            """,
            "CREATE INDEX IF NOT EXISTS idx_medicines_name ON medicines(medicine_name);",
            "CREATE INDEX IF NOT EXISTS idx_purchase_items_medicine ON purchase_items(medicine_id);",
            "CREATE INDEX IF NOT EXISTS idx_purchase_items_expiry ON purchase_items(expiry_date);",
            "CREATE INDEX IF NOT EXISTS idx_sales_date ON sales(sale_date);",
            "CREATE INDEX IF NOT EXISTS idx_purchases_date ON purchases(purchase_date);"
        };

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            for (String sql : ddlStatements) {
                stmt.execute(sql);
            }
            seedInitialDataIfEmpty(conn);
        } catch (SQLException e) {
            System.err.println("Database initialization error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Seeds initial practical sample data on fresh installs so the professor sees live data immediately.
     */
    private static void seedInitialDataIfEmpty(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            var rs = stmt.executeQuery("SELECT COUNT(*) FROM medicines;");
            if (rs.next() && rs.getInt(1) == 0) {
                // Insert Suppliers
                stmt.execute("""
                    INSERT INTO suppliers (supplier_name, phone, address, is_active) VALUES
                    ('الشركة المتحدة للصيادلة (United Pharmacists)', '01012345678', 'برج العرب، الإسكندرية', 1),
                    ('شركة ابن سينا فارما (Ibnsina Pharma)', '01298765432', 'سموحة، الإسكندرية', 1);
                """);

                // Insert Medicines
                stmt.execute("""
                    INSERT INTO medicines (medicine_name, category, barcode, sale_price_piasters, reorder_level, is_active) VALUES
                    ('بانادول إكسترا (Panadol Extra)', 'Painkillers', '6221234567890', 4500, 15, 1),
                    ('أوجمنتين 1 جم (Augmentin 1g)', 'Antibiotics', '6229876543210', 9500, 10, 1),
                    ('فيتامين سي 1000 (C-Retard 1000)', 'Vitamins', '6224567890123', 3200, 20, 1),
                    ('كونجستال أقراص (Congestal Tablets)', 'Cold and Flu', '6227890123456', 2800, 12, 1),
                    ('بانثينول كريم (Panthenol Cream)', 'Skincare', '6223456789012', 2000, 8, 1);
                """);

                // Insert Initial Purchase Shipment with Batches
                stmt.execute("""
                    INSERT INTO purchases (supplier_id, purchase_date, invoice_number, total_amount_piasters)
                    VALUES (1, date('now'), 'INV-2026-001', 85000);
                """);

                // Batches with different expiry dates (demonstrates FEFO)
                stmt.execute("""
                    INSERT INTO purchase_items (purchase_id, medicine_id, batch_number, expiry_date, quantity_received, quantity_remaining, unit_cost_piasters) VALUES
                    (1, 1, 'BATCH-P1', date('now', '+60 days'), 20, 20, 3500),
                    (1, 1, 'BATCH-P2', date('now', '+365 days'), 30, 30, 3500),
                    (1, 2, 'BATCH-A1', date('now', '+180 days'), 15, 15, 7500),
                    (1, 3, 'BATCH-V1', date('now', '+400 days'), 25, 25, 2400),
                    (1, 4, 'BATCH-C1', date('now', '+90 days'), 15, 15, 2100),
                    (1, 5, 'BATCH-S1', date('now', '+500 days'), 10, 10, 1500);
                """);
            }
        } catch (SQLException e) {
            System.err.println("Seeding error (non-fatal): " + e.getMessage());
        }
    }
}
