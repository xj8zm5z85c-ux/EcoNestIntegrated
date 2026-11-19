package com.mycompany.econesttask8and9;

import java.io.IOException;

/** Implements the email notifications required by EcoNest Tasks 8 and 9. */
public class Tasks89EmailNotificationService {

    private final Tasks89EmailSender emailSender;
    private final Tasks89NotificationRepository notificationRepository;

    public Tasks89EmailNotificationService(Tasks89EmailSender emailSender) {
        if (emailSender == null) {
            throw new IllegalArgumentException("Email sender is required.");
        }
        this.emailSender = emailSender;
        this.notificationRepository = new Tasks89NotificationRepository();
    }

    /** Task 8: notify a client after their order has been marked as dispatched. */
    public void notifyClientOrderDispatched(String clientName, String clientEmail,
            String orderId) throws Tasks89EmailDeliveryException {
        String safeClientName = requireText(clientName, "Client name");
        String safeOrderId = requireText(orderId, "Order ID");

        String subject = "Your EcoNest order " + safeOrderId + " has been dispatched";
        String body = "Hello " + safeClientName + ",\n\n"
                + "Your EcoNest order " + safeOrderId + " has been dispatched.\n"
                + "Thank you for choosing EcoNest.\n\n"
                + "EcoNest Home & Garden";

        sendTracked(true, safeOrderId, safeClientName,
                requireText(clientEmail, "Client email address"), null, subject, body);
    }

    /** Task 9: notify a delivery agent when a new delivery is assigned to them. */
    public void notifyAgentDeliveryAssigned(String agentName, String agentEmail,
            String orderId, String deliveryAddress) throws Tasks89EmailDeliveryException {
        String safeAgentName = requireText(agentName, "Delivery agent name");
        String safeOrderId = requireText(orderId, "Order ID");
        String safeAddress = requireText(deliveryAddress, "Delivery address");

        String subject = "New EcoNest delivery assignment: order " + safeOrderId;
        String body = "Hello " + safeAgentName + ",\n\n"
                + "A new delivery has been assigned to you.\n"
                + "Order: " + safeOrderId + "\n"
                + "Delivery address: " + safeAddress + "\n\n"
                + "Please sign in to EcoNest to review the delivery details.\n\n"
                + "EcoNest Home & Garden";

        sendTracked(false, safeOrderId, safeAgentName,
                requireText(agentEmail, "Delivery agent email address"), safeAddress, subject, body);
    }

    private void sendTracked(boolean dispatch, String orderCode, String contactName, String recipient,
            String deliveryAddress, String subject, String body) throws Tasks89EmailDeliveryException {
        final long notificationId;
        try {
            notificationId = dispatch
                    ? notificationRepository.beginDispatchNotification(orderCode, contactName, recipient)
                    : notificationRepository.beginAssignmentNotification(
                            orderCode, contactName, recipient, deliveryAddress);
        } catch (IOException exception) {
            throw new Tasks89EmailDeliveryException(
                    "Email was not sent because its database audit record could not be created: "
                            + exception.getMessage(), exception);
        }
        try {
            emailSender.send(recipient, subject, body);
        } catch (Tasks89EmailDeliveryException exception) {
            try {
                notificationRepository.markFailed(notificationId, exception.getMessage());
            } catch (IOException auditException) {
                exception.addSuppressed(auditException);
            }
            throw exception;
        }
        try {
            notificationRepository.markSent(notificationId);
        } catch (IOException exception) {
            throw new Tasks89EmailDeliveryException(
                    "Email was sent, but its database audit status could not be updated: "
                            + exception.getMessage(), exception);
        }
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        return value.trim();
    }
}
