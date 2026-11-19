package com.econest.task7.storage;

import com.econest.task7.model.Task7ProductStock;
import com.econest.task7.model.Task7Sale;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import lk.econest.db.Database;
import lk.econest.db.DatabaseException;

/** Provides report data from the shared EcoNest database. */
public final class Task7DatabaseRepository {
    public List<Task7ProductStock> loadInventory() throws IOException {
        String sql = "SELECT p.product_code, p.product_name, i.quantity_on_hand, "
                + "i.reorder_level, p.unit_price FROM products p "
                + "JOIN inventory i ON i.product_id = p.product_id "
                + "WHERE p.is_active = TRUE ORDER BY p.product_name";
        try (Connection connection = Database.openConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet rows = statement.executeQuery()) {
            List<Task7ProductStock> products = new ArrayList<>();
            while (rows.next()) {
                products.add(new Task7ProductStock(rows.getString("product_code"),
                        rows.getString("product_name"), rows.getInt("quantity_on_hand"),
                        rows.getInt("reorder_level"), rows.getBigDecimal("unit_price")));
            }
            return products;
        } catch (SQLException exception) {
            throw new DatabaseException("loading report inventory", exception);
        }
    }

    public List<Task7Sale> loadSales() throws IOException {
        String sql = "SELECT o.order_code, DATE(o.order_date) AS sale_date, c.client_name, "
                + "SUM(oi.quantity * oi.unit_price) AS total FROM orders o "
                + "JOIN clients c ON c.client_id = o.client_id "
                + "JOIN order_items oi ON oi.order_id = o.order_id "
                + "WHERE o.order_status <> 'CANCELLED' "
                + "GROUP BY o.order_id, o.order_code, DATE(o.order_date), c.client_name "
                + "ORDER BY o.order_date DESC";
        try (Connection connection = Database.openConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet rows = statement.executeQuery()) {
            List<Task7Sale> sales = new ArrayList<>();
            while (rows.next()) {
                sales.add(new Task7Sale(rows.getString("order_code"),
                        rows.getDate("sale_date").toLocalDate(), rows.getString("client_name"),
                        rows.getBigDecimal("total")));
            }
            return sales;
        } catch (SQLException exception) {
            throw new DatabaseException("loading report sales", exception);
        }
    }

}
