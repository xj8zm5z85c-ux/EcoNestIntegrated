package com.mycompany.econesttask8and9;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import lk.econest.db.Database;
import lk.econest.db.DatabaseException;

/** Persists email attempts against their corresponding order or delivery. */
public final class Tasks89NotificationRepository {
    public long beginDispatchNotification(String orderCode, String clientName, String recipient)
            throws IOException {
        return createNotification(orderCode, clientName, recipient, null, true);
    }

    public long beginAssignmentNotification(String orderCode, String agentName, String recipient,
            String deliveryAddress) throws IOException {
        return createNotification(orderCode, agentName, recipient, deliveryAddress, false);
    }

    public void markSent(long notificationId) throws IOException {
        updateStatus(notificationId, "SENT", null);
    }

    public void markFailed(long notificationId, String failure) throws IOException {
        updateStatus(notificationId, "FAILED", failure);
    }

    private long createNotification(String orderCode, String contactName, String recipient,
            String deliveryAddress, boolean dispatch)
            throws IOException {
        String targetSql = dispatch
                ? "SELECT o.order_id FROM orders o JOIN clients c ON c.client_id = o.client_id "
                        + "WHERE o.order_code = ? AND o.order_status = 'DISPATCHED' "
                        + "AND LOWER(c.client_name) = LOWER(?) AND LOWER(c.email) = LOWER(?)"
                : "SELECT d.delivery_id FROM deliveries d "
                        + "JOIN orders o ON o.order_id = d.order_id "
                        + "JOIN delivery_agents a ON a.agent_id = d.agent_id "
                        + "WHERE o.order_code = ? AND LOWER(a.full_name) = LOWER(?) "
                        + "AND LOWER(a.email) = LOWER(?) AND d.delivery_address = ?";
        try (Connection connection = Database.openConnection()) {
            long targetId;
            try (PreparedStatement statement = connection.prepareStatement(targetSql)) {
                statement.setString(1, orderCode);
                statement.setString(2, contactName);
                statement.setString(3, recipient);
                if (!dispatch) {
                    statement.setString(4, deliveryAddress);
                }
                try (ResultSet row = statement.executeQuery()) {
                    if (!row.next()) {
                        throw new IOException(dispatch
                                ? "No dispatched order was found with ID " + orderCode + "."
                                : "No delivery was found for order " + orderCode + ".");
                    }
                    targetId = row.getLong(1);
                }
            }
            String insertSql = dispatch
                    ? "INSERT INTO email_notifications "
                            + "(notification_type, order_id, recipient_email) VALUES ('ORDER_DISPATCHED', ?, ?)"
                    : "INSERT INTO email_notifications "
                            + "(notification_type, delivery_id, recipient_email) VALUES ('DELIVERY_ASSIGNED', ?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(
                    insertSql, Statement.RETURN_GENERATED_KEYS)) {
                statement.setLong(1, targetId);
                statement.setString(2, recipient);
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys.next()) {
                        return keys.getLong(1);
                    }
                }
            }
            throw new SQLException("Database did not return the notification identifier.");
        } catch (SQLException exception) {
            throw new DatabaseException("creating email notification audit record", exception);
        }
    }

    private static void updateStatus(long notificationId, String status, String failure)
            throws IOException {
        String sql = "UPDATE email_notifications SET send_status = ?, "
                + "sent_at = CASE WHEN ? = 'SENT' THEN CURRENT_TIMESTAMP ELSE NULL END, "
                + "error_message = ? WHERE notification_id = ?";
        try (Connection connection = Database.openConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            statement.setString(2, status);
            statement.setString(3, failure == null ? null
                    : failure.length() <= 500 ? failure : failure.substring(0, 500));
            statement.setLong(4, notificationId);
            if (statement.executeUpdate() != 1) {
                throw new IOException("Email notification audit record was not found: " + notificationId);
            }
        } catch (SQLException exception) {
            throw new DatabaseException("updating email notification status", exception);
        }
    }
}
