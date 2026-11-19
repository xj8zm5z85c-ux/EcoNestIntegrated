package lk.econest.catalog;

import lk.econest.db.Database;
import lk.econest.db.DatabaseException;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Reads and writes catalogue products in the shared EcoNest database. */
public final class Task1ProductRepository {

    public List<Task1Product> load() throws IOException {
        String sql = "SELECT product_code, product_name, category, unit_price, eco_rating "
                + "FROM products WHERE is_active = TRUE ORDER BY product_name";
        try (Connection connection = Database.openConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet rows = statement.executeQuery()) {
            List<Task1Product> products = new ArrayList<>();
            while (rows.next()) {
                BigDecimal rating = rows.getBigDecimal("eco_rating");
                products.add(new Task1Product(
                        rows.getString("product_code"),
                        rows.getString("product_name"),
                        Task1ProductType.valueOf(rows.getString("category")),
                        rows.getBigDecimal("unit_price"),
                        rating == null ? 1 : rating.intValue()));
            }
            return products;
        } catch (SQLException exception) {
            throw new DatabaseException("loading products", exception);
        }
    }

    public void saveProduct(Task1Product product) throws IOException {
        String upsert = "INSERT INTO products "
                + "(product_code, product_name, category, unit_price, eco_rating, is_active) "
                + "VALUES (?, ?, ?, ?, ?, TRUE) ON DUPLICATE KEY UPDATE "
                + "product_name = VALUES(product_name), category = VALUES(category), "
                + "unit_price = VALUES(unit_price), eco_rating = VALUES(eco_rating), is_active = TRUE";
        try (Connection connection = Database.openConnection();
                PreparedStatement statement = connection.prepareStatement(upsert)) {
            statement.setString(1, product.getSku());
            statement.setString(2, product.getName());
            statement.setString(3, product.getType().name());
            statement.setBigDecimal(4, product.getPrice());
            statement.setBigDecimal(5, BigDecimal.valueOf(product.getEcoRating()));
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new DatabaseException("saving product " + product.getSku(), exception);
        }
    }

    public void deactivateProduct(String productCode) throws IOException {
        String checkSql = "SELECT p.product_id, i.quantity_on_hand FROM products p "
                + "JOIN inventory i ON i.product_id = p.product_id "
                + "WHERE p.product_code = ? AND p.is_active = TRUE FOR UPDATE";
        try (Connection connection = Database.openConnection()) {
            connection.setAutoCommit(false);
            try {
                int productId;
                int quantity;
                try (PreparedStatement statement = connection.prepareStatement(checkSql)) {
                    statement.setString(1, productCode);
                    try (ResultSet row = statement.executeQuery()) {
                        if (!row.next()) {
                            throw new IOException("Active product was not found: " + productCode);
                        }
                        productId = row.getInt("product_id");
                        quantity = row.getInt("quantity_on_hand");
                    }
                }
                boolean hasOpenOrders;
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT EXISTS (SELECT 1 FROM order_items oi "
                                + "JOIN orders o ON o.order_id = oi.order_id "
                                + "WHERE oi.product_id = ? "
                                + "AND o.order_status NOT IN ('DELIVERED', 'CANCELLED'))")) {
                    statement.setInt(1, productId);
                    try (ResultSet row = statement.executeQuery()) {
                        row.next();
                        hasOpenOrders = row.getBoolean(1);
                    }
                }
                if (quantity > 0 || hasOpenOrders) {
                    throw new IOException("Product cannot be removed while it has stock or open orders.");
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE products SET is_active = FALSE WHERE product_code = ? AND is_active = TRUE")) {
                    statement.setString(1, productCode);
                    if (statement.executeUpdate() != 1) {
                        throw new IOException("Active product was not found: " + productCode);
                    }
                }
                connection.commit();
            } catch (SQLException | IOException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw new DatabaseException("removing product " + productCode, exception);
        }
    }
}
