package com.mycompany.econesttask8and9;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

/** Small Swing screen for trying the Task 8 and Task 9 email notifications. */
public class Tasks89NotificationDemoFrame extends JFrame {

    private final JTextField smtpHostField = new JTextField("smtp.gmail.com", 24);
    private final JTextField smtpPortField = new JTextField("587", 8);
    private final JTextField fromEmailField = new JTextField(24);
    private final JTextField smtpUsernameField = new JTextField(24);
    private final JPasswordField smtpPasswordField = new JPasswordField(24);

    private final JTextField clientNameField = new JTextField(24);
    private final JTextField clientEmailField = new JTextField(24);
    private final JTextField dispatchOrderIdField = new JTextField(24);

    private final JTextField agentNameField = new JTextField(24);
    private final JTextField agentEmailField = new JTextField(24);
    private final JTextField assignmentOrderIdField = new JTextField(24);
    private final JTextField deliveryAddressField = new JTextField(24);

    public Tasks89NotificationDemoFrame() {
        super("EcoNest - Tasks 8 and 9 Email Notifications");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(620, 590);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        JPanel smtpPanel = createSmtpPanel();
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Order Dispatched", createClientPanel());
        tabs.addTab("Delivery Assigned", createAgentPanel());

        add(smtpPanel, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
        add(new JLabel("SMTP credentials are used only while this application is running."),
                BorderLayout.SOUTH);
    }

    private JPanel createSmtpPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("SMTP Email Settings"));
        addRow(panel, 0, "SMTP host:", smtpHostField);
        addRow(panel, 1, "SMTP port:", smtpPortField);
        addRow(panel, 2, "From email:", fromEmailField);
        addRow(panel, 3, "SMTP username:", smtpUsernameField);
        addRow(panel, 4, "SMTP password:", smtpPasswordField);
        return panel;
    }

    private JPanel createClientPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        addRow(panel, 0, "Client name:", clientNameField);
        addRow(panel, 1, "Client email:", clientEmailField);
        addRow(panel, 2, "Order ID:", dispatchOrderIdField);

        JButton sendButton = new JButton("Send dispatch email");
        addButton(panel, 3, sendButton);
        sendButton.addActionListener(event -> sendClientDispatchEmail(sendButton));
        return panel;
    }

    private JPanel createAgentPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        addRow(panel, 0, "Delivery agent name:", agentNameField);
        addRow(panel, 1, "Delivery agent email:", agentEmailField);
        addRow(panel, 2, "Order ID:", assignmentOrderIdField);
        addRow(panel, 3, "Delivery address:", deliveryAddressField);

        JButton sendButton = new JButton("Send assignment email");
        addButton(panel, 4, sendButton);
        sendButton.addActionListener(event -> sendAgentAssignmentEmail(sendButton));
        return panel;
    }

    private void sendClientDispatchEmail(JButton sendButton) {
        try {
            Tasks89EmailNotificationService service = createNotificationService();
            String clientName = clientNameField.getText().trim();
            String clientEmail = clientEmailField.getText().trim();
            String orderId = dispatchOrderIdField.getText().trim();
            runEmailTask(sendButton, "Send dispatch email", () ->
                    service.notifyClientOrderDispatched(clientName, clientEmail, orderId));
        } catch (IllegalArgumentException exception) {
            showError(exception.getMessage());
        }
    }

    private void sendAgentAssignmentEmail(JButton sendButton) {
        try {
            Tasks89EmailNotificationService service = createNotificationService();
            String agentName = agentNameField.getText().trim();
            String agentEmail = agentEmailField.getText().trim();
            String orderId = assignmentOrderIdField.getText().trim();
            String deliveryAddress = deliveryAddressField.getText().trim();
            runEmailTask(sendButton, "Send assignment email", () ->
                    service.notifyAgentDeliveryAssigned(agentName, agentEmail,
                            orderId, deliveryAddress));
        } catch (IllegalArgumentException exception) {
            showError(exception.getMessage());
        }
    }

    private Tasks89EmailNotificationService createNotificationService() {
        final int port;
        try {
            port = Integer.parseInt(smtpPortField.getText().trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("SMTP port must be a number.");
        }

        String password = new String(smtpPasswordField.getPassword());
        Tasks89EmailSender sender = new Tasks89SmtpEmailSender(
                smtpHostField.getText(), port, fromEmailField.getText(),
                smtpUsernameField.getText(), password);
        return new Tasks89EmailNotificationService(sender);
    }

    private void runEmailTask(JButton sendButton, String readyText, EmailAction action) {
        sendButton.setEnabled(false);
        sendButton.setText("Sending...");

        new SwingWorker<Void, Void>() {
            private Exception failure;

            @Override
            protected Void doInBackground() {
                try {
                    action.send();
                } catch (Exception exception) {
                    failure = exception;
                }
                return null;
            }

            @Override
            protected void done() {
                sendButton.setEnabled(true);
                sendButton.setText(readyText);
                if (failure == null) {
                    JOptionPane.showMessageDialog(Tasks89NotificationDemoFrame.this,
                            "Email sent successfully.", "Email sent",
                            JOptionPane.INFORMATION_MESSAGE);
                } else {
                    showError(failure.getMessage());
                }
            }
        }.execute();
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Could not send email",
                JOptionPane.ERROR_MESSAGE);
    }

    private static void addRow(JPanel panel, int row, String label, JTextField field) {
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = row;
        labelConstraints.anchor = GridBagConstraints.WEST;
        labelConstraints.insets = new Insets(4, 6, 4, 8);
        panel.add(new JLabel(label), labelConstraints);

        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.gridx = 1;
        fieldConstraints.gridy = row;
        fieldConstraints.weightx = 1.0;
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.insets = new Insets(4, 0, 4, 6);
        panel.add(field, fieldConstraints);
    }

    private static void addButton(JPanel panel, int row, JButton button) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = row;
        constraints.anchor = GridBagConstraints.EAST;
        constraints.insets = new Insets(12, 0, 4, 6);
        panel.add(button, constraints);
    }

    @FunctionalInterface
    private interface EmailAction {
        void send() throws Exception;
    }
}
