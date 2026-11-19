package econest;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lk.econest.db.Database;

/** Database-backed shared store for the order and delivery workflow. */
public class Tasks56EcoNestStore {
    private List<Tasks56Client> clients = new ArrayList<>();
    private List<Tasks56Product> products = new ArrayList<>();
    private List<Tasks56DeliveryAgent> agents = new ArrayList<>();
    private List<Tasks56ClientOrder> orders = new ArrayList<>();

    public static Tasks56EcoNestStore load() {
        Tasks56EcoNestStore store = new Tasks56EcoNestStore();
        try {
            store.reload();
            return store;
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not load shared EcoNest data: "
                    + exception.getMessage(), exception);
        }
    }

    public void save() {
        try (Connection connection = Database.openConnection()) {
            connection.setAutoCommit(false);
            try {
                for (Tasks56ClientOrder order : orders) {
                    persistOrder(connection, order);
                }
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                try {
                    reload();
                } catch (SQLException reloadException) {
                    exception.addSuppressed(reloadException);
                }
                throw exception;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not save order and delivery data: "
                    + exception.getMessage(), exception);
        }
        try {
            reload();
        } catch (SQLException exception) {
            throw new IllegalStateException("Changes were saved, but the latest shared data could not be reloaded: "
                    + exception.getMessage(), exception);
        }
    }

    private void persistOrder(Connection connection, Tasks56ClientOrder order) throws SQLException {
        Long orderId = findOrderId(connection, order.getId());
        if (orderId == null) {
            int clientId = findId(connection, "SELECT client_id FROM clients WHERE client_code = ?",
                    order.getClient().getId(), "Client");
            String insert = "INSERT INTO orders (order_code, client_id, order_date, order_status) "
                    + "VALUES (?, ?, ?, 'PENDING')";
            try (PreparedStatement statement = connection.prepareStatement(
                    insert, Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, order.getId());
                statement.setInt(2, clientId);
                statement.setDate(3, Date.valueOf(order.getCreatedDate()));
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (!keys.next()) {
                        throw new SQLException("Database did not return the order identifier.");
                    }
                    orderId = keys.getLong(1);
                }
            }
            insertOrderItems(connection, orderId, order.getItems());
        }
        persistOrderStatus(connection, orderId, order);
    }

    private static void insertOrderItems(Connection connection, long orderId,
            List<Tasks56OrderItem> items) throws SQLException {
        String productSql = "SELECT product_id FROM products WHERE product_code = ? AND is_active = TRUE";
        String itemSql = "INSERT INTO order_items (order_id, product_id, quantity, unit_price) "
                + "VALUES (?, ?, ?, ?)";
        try (PreparedStatement productStatement = connection.prepareStatement(productSql);
                PreparedStatement itemStatement = connection.prepareStatement(itemSql)) {
            for (Tasks56OrderItem item : items) {
                productStatement.setString(1, item.getProductId());
                try (ResultSet product = productStatement.executeQuery()) {
                    if (!product.next()) {
                        throw new SQLException("Active product was not found: " + item.getProductId());
                    }
                    itemStatement.setLong(1, orderId);
                    itemStatement.setInt(2, product.getInt("product_id"));
                    itemStatement.setInt(3, item.getQuantity());
                    itemStatement.setBigDecimal(4, item.getUnitPriceAmount());
                    itemStatement.executeUpdate();
                }
            }
        }
    }

    private static void persistOrderStatus(Connection connection, long orderId,
            Tasks56ClientOrder order) throws SQLException {
        Tasks56DeliveryAgent agent = order.getDeliveryAgent();
        String orderStatus = agent == null ? "PENDING" : switch (order.getDeliveryStatus()) {
            case DISPATCHED -> "DISPATCHED";
            case DELIVERED -> "DELIVERED";
            case ASSIGNED -> "PROCESSING";
            case NOT_ASSIGNED -> "PENDING";
        };
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE orders SET order_status = ?, "
                        + "dispatched_at = CASE WHEN ? IN ('DISPATCHED', 'DELIVERED') AND dispatched_at IS NULL "
                        + "THEN CURRENT_TIMESTAMP ELSE dispatched_at END WHERE order_id = ?")) {
            statement.setString(1, orderStatus);
            statement.setString(2, orderStatus);
            statement.setLong(3, orderId);
            statement.executeUpdate();
        }
        if (agent == null) {
            return;
        }

        int agentId = findDeliveryAgentId(connection, agent.getId(), orderId);
        String address = findClientAddress(connection, order.getClient().getId());
        String deliveryStatus = switch (order.getDeliveryStatus()) {
            case DELIVERED -> "DELIVERED";
            case DISPATCHED -> "DISPATCHED";
            case ASSIGNED, NOT_ASSIGNED -> "ASSIGNED";
        };
        String deliverySql = "INSERT INTO deliveries "
                + "(order_id, agent_id, delivery_address, scheduled_for, delivery_status, assigned_at, dispatched_at, delivered_at) "
                + "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, "
                + "CASE WHEN ? IN ('DISPATCHED', 'DELIVERED') THEN CURRENT_TIMESTAMP ELSE NULL END, "
                + "CASE WHEN ? = 'DELIVERED' THEN CURRENT_TIMESTAMP ELSE NULL END) "
                + "ON DUPLICATE KEY UPDATE agent_id = VALUES(agent_id), "
                + "scheduled_for = VALUES(scheduled_for), "
                + "delivery_status = VALUES(delivery_status), "
                + "dispatched_at = COALESCE(VALUES(dispatched_at), dispatched_at), "
                + "delivered_at = COALESCE(VALUES(delivered_at), delivered_at)";
        try (PreparedStatement statement = connection.prepareStatement(deliverySql)) {
            statement.setLong(1, orderId);
            statement.setInt(2, agentId);
            statement.setString(3, address);
            statement.setDate(4, Date.valueOf(order.getDeliveryDate()));
            statement.setString(5, deliveryStatus);
            statement.setString(6, deliveryStatus);
            statement.setString(7, deliveryStatus);
            statement.executeUpdate();
        }
    }

    private void reload() throws SQLException {
        List<Tasks56Client> loadedClients = loadClients();
        List<Tasks56Product> loadedProducts = loadProducts();
        List<Tasks56DeliveryAgent> loadedAgents = loadAgents();
        List<Tasks56ClientOrder> loadedOrders = loadOrders();
        clients = loadedClients;
        products = loadedProducts;
        agents = loadedAgents;
        orders = loadedOrders;
    }

    private static List<Tasks56Client> loadClients() throws SQLException {
        List<Tasks56Client> result = new ArrayList<>();
        try (Connection connection = Database.openConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT client_code, client_name, email, phone FROM clients ORDER BY client_name");
                ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                result.add(new Tasks56Client(rows.getString("client_code"), rows.getString("client_name"),
                        rows.getString("email"), rows.getString("phone")));
            }
        }
        return result;
    }

    private static List<Tasks56Product> loadProducts() throws SQLException {
        List<Tasks56Product> result = new ArrayList<>();
        String sql = "SELECT p.product_code, p.product_name, p.unit_price, i.quantity_on_hand "
                + "FROM products p JOIN inventory i ON i.product_id = p.product_id "
                + "WHERE p.is_active = TRUE ORDER BY p.product_name";
        try (Connection connection = Database.openConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                result.add(new Tasks56Product(rows.getString("product_code"),
                        rows.getString("product_name"), rows.getBigDecimal("unit_price"),
                        rows.getInt("quantity_on_hand")));
            }
        }
        return result;
    }

    private static List<Tasks56DeliveryAgent> loadAgents() throws SQLException {
        List<Tasks56DeliveryAgent> result = new ArrayList<>();
        try (Connection connection = Database.openConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT agent_code, full_name, phone, vehicle_type, vehicle_model "
                                + "FROM delivery_agents WHERE is_active = TRUE ORDER BY full_name");
                ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                result.add(new Tasks56DeliveryAgent(rows.getString("agent_code"),
                        rows.getString("full_name"), rows.getString("phone"),
                        rows.getString("vehicle_type") + " " + rows.getString("vehicle_model")));
            }
        }
        return result;
    }

    private static List<Tasks56ClientOrder> loadOrders() throws SQLException {
        String sql = "SELECT o.order_code, DATE(o.order_date) AS order_date, o.order_status, "
                + "c.client_code, c.client_name, c.email, c.phone, d.scheduled_for, d.delivery_status, "
                + "a.agent_code, a.full_name, a.phone AS agent_phone, a.vehicle_type, a.vehicle_model, "
                + "p.product_code, p.product_name, "
                + "oi.unit_price, oi.quantity FROM orders o "
                + "JOIN clients c ON c.client_id = o.client_id "
                + "LEFT JOIN deliveries d ON d.order_id = o.order_id "
                + "LEFT JOIN delivery_agents a ON a.agent_id = d.agent_id "
                + "JOIN order_items oi ON oi.order_id = o.order_id "
                + "LEFT JOIN products p ON p.product_id = oi.product_id "
                + "WHERE o.order_status <> 'CANCELLED' "
                + "ORDER BY o.order_date DESC, oi.product_id";
        Map<String, Tasks56ClientOrder> loaded = new LinkedHashMap<>();
        try (Connection connection = Database.openConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                String code = rows.getString("order_code");
                Tasks56ClientOrder order = loaded.get(code);
                if (order == null) {
                    Tasks56Client client = new Tasks56Client(rows.getString("client_code"),
                            rows.getString("client_name"), rows.getString("email"), rows.getString("phone"));
                    order = new Tasks56ClientOrder(code, client,
                            rows.getDate("order_date").toLocalDate(), new ArrayList<>());
                    String deliveryStatus = rows.getString("delivery_status");
                    if (deliveryStatus != null) {
                        String agentCode = rows.getString("agent_code");
                        Tasks56DeliveryAgent agent = new Tasks56DeliveryAgent(agentCode,
                                rows.getString("full_name"), rows.getString("agent_phone"),
                                rows.getString("vehicle_type") + " " + rows.getString("vehicle_model"));
                        LocalDate scheduled = rows.getDate("scheduled_for") == null
                                ? rows.getDate("order_date").toLocalDate()
                                : rows.getDate("scheduled_for").toLocalDate();
                        order.restoreDelivery(agent, scheduled, toDeliveryStatus(deliveryStatus));
                    }
                    loaded.put(code, order);
                }
                Tasks56Product snapshot = new Tasks56Product(rows.getString("product_code"),
                        rows.getString("product_name"), rows.getBigDecimal("unit_price"), 0);
                order.addRestoredItem(new Tasks56OrderItem(snapshot, rows.getInt("quantity")));
            }
        }
        return new ArrayList<>(loaded.values());
    }

    private static Tasks56DeliveryStatus toDeliveryStatus(String status) {
        return switch (status) {
            case "DELIVERED" -> Tasks56DeliveryStatus.DELIVERED;
            case "DISPATCHED", "IN_TRANSIT" -> Tasks56DeliveryStatus.DISPATCHED;
            default -> Tasks56DeliveryStatus.ASSIGNED;
        };
    }

    private static Long findOrderId(Connection connection, String orderCode) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT order_id FROM orders WHERE order_code = ?")) {
            statement.setString(1, orderCode);
            try (ResultSet row = statement.executeQuery()) {
                return row.next() ? row.getLong(1) : null;
            }
        }
    }

    public void cancelOrder(String orderCode) {
        try (Connection connection = Database.openConnection()) {
            connection.setAutoCommit(false);
            try {
                Long orderId = findOrderId(connection, orderCode);
                if (orderId == null) {
                    throw new SQLException("Order was not found: " + orderCode);
                }

                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE orders SET order_status = 'CANCELLED' "
                                + "WHERE order_id = ? AND order_status <> 'CANCELLED'")) {
                    statement.setLong(1, orderId);
                    if (statement.executeUpdate() != 1) {
                        throw new SQLException("Order is already cancelled: " + orderCode);
                    }
                }

                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE deliveries SET delivery_status = 'CANCELLED' "
                                + "WHERE order_id = ?")) {
                    statement.setLong(1, orderId);
                    statement.executeUpdate();
                }

                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not cancel order " + orderCode + ": "
                    + exception.getMessage(), exception);
        }

        try {
            reload();
        } catch (SQLException exception) {
            throw new IllegalStateException("The order was cancelled, but the latest data could not be reloaded: "
                    + exception.getMessage(), exception);
        }
    }

    public void removeDeliveredOrder(String orderCode) {
        try (Connection connection = Database.openConnection()) {
            connection.setAutoCommit(false);
            try {
                Long orderId = findOrderId(connection, orderCode);
                if (orderId == null) {
                    throw new SQLException("Order was not found: " + orderCode);
                }

                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT order_status FROM orders WHERE order_id = ? FOR UPDATE")) {
                    statement.setLong(1, orderId);
                    try (ResultSet row = statement.executeQuery()) {
                        if (!row.next() || !"DELIVERED".equals(row.getString("order_status"))) {
                            throw new SQLException("Only delivered orders can be permanently removed.");
                        }
                    }
                }

                try (PreparedStatement statement = connection.prepareStatement(
                        "DELETE FROM email_notifications "
                                + "WHERE order_id = ? OR delivery_id IN "
                                + "(SELECT delivery_id FROM deliveries WHERE order_id = ?)")) {
                    statement.setLong(1, orderId);
                    statement.setLong(2, orderId);
                    statement.executeUpdate();
                }

                try (PreparedStatement statement = connection.prepareStatement(
                        "DELETE FROM deliveries WHERE order_id = ?")) {
                    statement.setLong(1, orderId);
                    statement.executeUpdate();
                }

                try (PreparedStatement statement = connection.prepareStatement(
                        "DELETE FROM order_items WHERE order_id = ?")) {
                    statement.setLong(1, orderId);
                    statement.executeUpdate();
                }

                try (PreparedStatement statement = connection.prepareStatement(
                        "DELETE FROM orders WHERE order_id = ?")) {
                    statement.setLong(1, orderId);
                    if (statement.executeUpdate() != 1) {
                        throw new SQLException("Order could not be removed: " + orderCode);
                    }
                }

                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not remove order " + orderCode + ": "
                    + exception.getMessage(), exception);
        }

        try {
            reload();
        } catch (SQLException exception) {
            throw new IllegalStateException("The order was removed, but the latest data could not be reloaded: "
                    + exception.getMessage(), exception);
        }
    }

    private static int findId(Connection connection, String sql, String code, String label)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, code);
            try (ResultSet row = statement.executeQuery()) {
                if (row.next()) {
                    return row.getInt(1);
                }
            }
        }
        throw new SQLException(label + " was not found or is inactive: " + code);
    }

    private static int findDeliveryAgentId(Connection connection, String agentCode, long orderId)
            throws SQLException {
        String sql = "SELECT a.agent_id FROM delivery_agents a WHERE a.agent_code = ? "
                + "AND (a.is_active = TRUE OR EXISTS (SELECT 1 FROM deliveries d "
                + "WHERE d.order_id = ? AND d.agent_id = a.agent_id)) FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, agentCode);
            statement.setLong(2, orderId);
            try (ResultSet row = statement.executeQuery()) {
                if (row.next()) {
                    return row.getInt(1);
                }
            }
        }
        throw new SQLException("Delivery agent was not found or is inactive: " + agentCode);
    }

    private static String findClientAddress(Connection connection, String clientCode)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT address_line FROM clients WHERE client_code = ?")) {
            statement.setString(1, clientCode);
            try (ResultSet row = statement.executeQuery()) {
                if (row.next()) {
                    return row.getString(1);
                }
            }
        }
        throw new SQLException("Client was not found: " + clientCode);
    }

    public List<Tasks56Client> getClients() { return clients; }
    public List<Tasks56Product> getProducts() { return products; }
    public List<Tasks56DeliveryAgent> getAgents() { return agents; }
    public List<Tasks56ClientOrder> getOrders() { return orders; }
}
