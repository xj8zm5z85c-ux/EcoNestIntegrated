package com.mycompany.econesttask3inventory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lk.econest.db.Database;
import lk.econest.db.DatabaseException;

/** Reads and updates the shared product inventory and stock-in history. */
public class Task3InventoryRepository {
    public List<Task3InventoryItem> loadAll() throws IOException {
        String sql = "SELECT p.product_code, p.product_name, i.quantity_on_hand, i.reorder_level "
                + "FROM products p JOIN inventory i ON i.product_id = p.product_id "
                + "WHERE p.is_active = TRUE ORDER BY p.product_name";
        try (Connection connection = Database.openConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet rows = statement.executeQuery()) {
            Map<String, Task3InventoryItem> inventory = new LinkedHashMap<>();
            while (rows.next()) {
                inventory.put(rows.getString("product_code"),
                        new Task3InventoryItem(rows.getString("product_code"),
                                rows.getString("product_name"), rows.getInt("quantity_on_hand"),
                                rows.getInt("reorder_level")));
            }
            loadHistory(connection, inventory);
            return new ArrayList<>(inventory.values());
        } catch (SQLException exception) {
            throw new DatabaseException("loading inventory", exception);
        }
    }

    public void setReorderLevel(String productCode, int reorderLevel) throws IOException {
        String sql = "UPDATE inventory i JOIN products p ON p.product_id = i.product_id "
                + "SET i.reorder_level = ? WHERE p.product_code = ? AND p.is_active = TRUE";
        try (Connection connection = Database.openConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, reorderLevel);
            statement.setString(2, productCode);
            if (statement.executeUpdate() != 1) {
                throw new IOException("Product was not found in the active catalogue: " + productCode);
            }
        } catch (SQLException exception) {
            throw new DatabaseException("updating the reorder level", exception);
        }
    }

    public void recordStockIn(String productCode, String supplierName, int quantity) throws IOException {
        try (Connection connection = Database.openConnection()) {
            connection.setAutoCommit(false);
            try {
                int productId = findProductId(connection, productCode);
                int supplierId = insertSupplier(connection, supplierName);
                long stockInId;
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO stock_ins (supplier_id) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
                    statement.setInt(1, supplierId);
                    statement.executeUpdate();
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("Database did not return the stock-in identifier.");
                        }
                        stockInId = keys.getLong(1);
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO stock_in_items (stock_in_id, product_id, quantity_received) VALUES (?, ?, ?)")) {
                    statement.setLong(1, stockInId);
                    statement.setInt(2, productId);
                    statement.setInt(3, quantity);
                    statement.executeUpdate();
                }
                connection.commit();
            } catch (SQLException | IOException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw new DatabaseException("recording stock-in", exception);
        }
    }

    private static int findProductId(Connection connection, String productCode) throws SQLException, IOException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT product_id FROM products WHERE product_code = ? AND is_active = TRUE FOR UPDATE")) {
            statement.setString(1, productCode);
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) {
                    throw new IOException("Product was not found in the active catalogue: " + productCode);
                }
                return row.getInt(1);
            }
        }
    }

    private static int insertSupplier(Connection connection, String supplierName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT supplier_id FROM suppliers WHERE supplier_name = ? AND is_active = TRUE "
                        + "ORDER BY supplier_id LIMIT 1 FOR UPDATE")) {
            statement.setString(1, supplierName);
            try (ResultSet row = statement.executeQuery()) {
                if (row.next()) {
                    return row.getInt(1);
                }
            }
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO suppliers (supplier_name) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, supplierName);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Database did not return the supplier identifier.");
                }
                return keys.getInt(1);
            }
        }
    }

    private static void loadHistory(Connection connection, Map<String, Task3InventoryItem> inventory)
            throws SQLException {
        String sql = "SELECT p.product_code, s.supplier_name, si.quantity_received, st.received_at "
                + "FROM stock_in_items si JOIN stock_ins st ON st.stock_in_id = si.stock_in_id "
                + "JOIN suppliers s ON s.supplier_id = st.supplier_id "
                + "JOIN products p ON p.product_id = si.product_id ORDER BY si.stock_in_id";
        try (PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                Task3InventoryItem item = inventory.get(rows.getString("product_code"));
                if (item != null) {
                    Timestamp timestamp = rows.getTimestamp("received_at");
                    LocalDateTime receivedAt = timestamp.toLocalDateTime();
                    item.addHistoricalStock(new Task3StockInRecord(rows.getString("supplier_name"),
                            rows.getInt("quantity_received"), receivedAt));
                }
            }
        }
    }
}
