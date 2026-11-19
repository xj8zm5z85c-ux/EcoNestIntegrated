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
    public record NotificationTarget(long notificationId, String contactName,
            String recipientEmail, String deliveryAddress) {
    }

    public NotificationTarget beginDispatchNotification(String orderCode) throws IOException {
        return createNotification(orderCode, true);
    }

    public NotificationTarget beginAssignmentNotification(String orderCode) throws IOException {
        return createNotification(orderCode, false);
    }

    public void markSent(long notificationId) throws IOException {
        updateStatus(notificationId, "SENT", null);
    }

    public void markFailed(long notificationId, String failure) throws IOException {
        updateStatus(notificationId, "FAILED", failure);
    }

    private NotificationTarget createNotification(String orderCode, boolean dispatch) throws IOException {
        String targetSql = dispatch
                ? "SELECT o.order_id AS target_id, c.client_name AS contact_name, "
                        + "c.email AS recipient_email, NULL AS delivery_address "
                        + "FROM orders o JOIN clients c ON c.client_id = o.client_id "
                        + "WHERE o.order_code = ? AND o.order_status = 'DISPATCHED'"
                : "SELECT d.delivery_id AS target_id, a.full_name AS contact_name, "
                        + "a.email AS recipient_email, d.delivery_address "
                        + "FROM deliveries d "
                        + "JOIN orders o ON o.order_id = d.order_id "
                        + "JOIN delivery_agents a ON a.agent_id = d.agent_id "
                        + "WHERE o.order_code = ?";
        try (Connection connection = Database.openConnection()) {
            long targetId;
            String contactName;
            String recipient;
            String deliveryAddress;
            try (PreparedStatement statement = connection.prepareStatement(targetSql)) {
                statement.setString(1, orderCode);
                try (ResultSet row = statement.executeQuery()) {
                    if (!row.next()) {
                        throw new IOException(dispatch
                                ? "Order " + orderCode + " was not found or is not in DISPATCHED status."
                                : "Order " + orderCode + " was not found or has no assigned delivery agent.");
                    }
                    targetId = row.getLong("target_id");
                    contactName = row.getString("contact_name");
                    recipient = row.getString("recipient_email");
                    deliveryAddress = row.getString("delivery_address");
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
                        return new NotificationTarget(keys.getLong(1), contactName,
                                recipient, deliveryAddress);
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
