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
    public void notifyClientOrderDispatched(String orderId) throws Tasks89EmailDeliveryException {
        String safeOrderId = requireText(orderId, "Order ID");
        Tasks89NotificationRepository.NotificationTarget target;
        try {
            target = notificationRepository.beginDispatchNotification(safeOrderId);
        } catch (IOException exception) {
            throw auditCreationFailure(exception);
        }
        String subject = "Your EcoNest order " + safeOrderId + " has been dispatched";
        String body = "Hello " + target.contactName() + ",\n\n"
                + "Your EcoNest order " + safeOrderId + " has been dispatched.\n"
                + "Thank you for choosing EcoNest.\n\n"
                + "EcoNest Home & Garden";

        sendTracked(target, subject, body);
    }

    /** Task 9: notify a delivery agent when a new delivery is assigned to them. */
    public void notifyAgentDeliveryAssigned(String orderId) throws Tasks89EmailDeliveryException {
        String safeOrderId = requireText(orderId, "Order ID");
        Tasks89NotificationRepository.NotificationTarget target;
        try {
            target = notificationRepository.beginAssignmentNotification(safeOrderId);
        } catch (IOException exception) {
            throw auditCreationFailure(exception);
        }
        String subject = "New EcoNest delivery assignment: order " + safeOrderId;
        String body = "Hello " + target.contactName() + ",\n\n"
                + "A new delivery has been assigned to you.\n"
                + "Order: " + safeOrderId + "\n"
                + "Delivery address: " + target.deliveryAddress() + "\n\n"
                + "Please sign in to EcoNest to review the delivery details.\n\n"
                + "EcoNest Home & Garden";

        sendTracked(target, subject, body);
    }

    private void sendTracked(Tasks89NotificationRepository.NotificationTarget target,
            String subject, String body) throws Tasks89EmailDeliveryException {
        try {
            emailSender.send(target.recipientEmail(), subject, body);
        } catch (Tasks89EmailDeliveryException exception) {
            try {
                notificationRepository.markFailed(target.notificationId(), exception.getMessage());
            } catch (IOException auditException) {
                exception.addSuppressed(auditException);
            }
            throw exception;
        }
        try {
            notificationRepository.markSent(target.notificationId());
        } catch (IOException exception) {
            throw new Tasks89EmailDeliveryException(
                    "Email was sent, but its database audit status could not be updated: "
                            + exception.getMessage(), exception);
        }
    }

    private static Tasks89EmailDeliveryException auditCreationFailure(IOException exception) {
        return new Tasks89EmailDeliveryException(
                "Email was not sent because its database audit record could not be created: "
                        + exception.getMessage(), exception);
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        return value.trim();
    }
}
