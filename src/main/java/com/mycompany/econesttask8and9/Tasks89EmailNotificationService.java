package com.mycompany.econesttask8and9;

/** Implements the email notifications required by EcoNest Tasks 8 and 9. */
public class Tasks89EmailNotificationService {

    private final Tasks89EmailSender emailSender;

    public Tasks89EmailNotificationService(Tasks89EmailSender emailSender) {
        if (emailSender == null) {
            throw new IllegalArgumentException("Email sender is required.");
        }
        this.emailSender = emailSender;
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

        emailSender.send(requireText(clientEmail, "Client email address"), subject, body);
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

        emailSender.send(requireText(agentEmail, "Delivery agent email address"), subject, body);
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        return value.trim();
    }
}
