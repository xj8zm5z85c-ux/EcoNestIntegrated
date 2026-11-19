package com.mycompany.econesttask4deliveryagents;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class Task4EcoNestTask4DeliveryAgents {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                Task4DeliveryAgentService service =
                        new Task4DeliveryAgentService(new Task4DeliveryAgentRepository());
                new Task4DeliveryAgentFrame(service).setVisible(true);
            } catch (Exception exception) {
                JOptionPane.showMessageDialog(null,
                        "Could not open delivery-agent data: " + exception.getMessage(),
                        "EcoNest Delivery Agents", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
