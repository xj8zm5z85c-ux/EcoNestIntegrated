package com.mycompany.econesttask4deliveryagents;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.IOException;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.AbstractTableModel;

/** Swing screen for managing delivery personnel and vehicle details. */
public class Task4DeliveryAgentFrame extends javax.swing.JFrame {

    private final Task4DeliveryAgentService service;
    private final AgentTableModel tableModel = new AgentTableModel();
    private final JTable agentTable = new JTable(tableModel);
    private final JLabel countLabel = new JLabel();

    public Task4DeliveryAgentFrame(Task4DeliveryAgentService service) {
        super("EcoNest - Delivery Agent Management (Task 4)");
        this.service = service;
        setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
        setSize(1020, 520);
        setLocationRelativeTo(null);

        JPanel topPanel = new JPanel(new BorderLayout(10, 0));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 12, 8, 12));
        countLabel.setFont(countLabel.getFont().deriveFont(java.awt.Font.BOLD, 14f));
        topPanel.add(countLabel, BorderLayout.WEST);
        topPanel.add(createToolbar(), BorderLayout.EAST);

        agentTable.setFillsViewportHeight(true);
        agentTable.setAutoCreateRowSorter(true);
        agentTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(agentTable), BorderLayout.CENTER);
        add(new JLabel("Delivery agents are shared with the order and delivery workflow."),
                BorderLayout.SOUTH);
        refreshTable();
    }

    private JPanel createToolbar() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 7, 0));
        JButton addButton = new JButton("Add Agent");
        JButton editButton = new JButton("Edit Agent");
        JButton removeButton = new JButton("Remove Agent");

        addButton.addActionListener(event -> showAgentEditor(null));
        editButton.addActionListener(event -> editSelectedAgent());
        removeButton.addActionListener(event -> removeSelectedAgent());

        panel.add(addButton);
        panel.add(editButton);
        panel.add(removeButton);
        return panel;
    }

    private void showAgentEditor(Task4DeliveryAgent existingAgent) {
        boolean editing = existingAgent != null;
        JTextField idField = new JTextField(editing ? existingAgent.getAgentId() : "", 18);
        JTextField nameField = new JTextField(editing ? existingAgent.getName() : "", 18);
        JTextField phoneField = new JTextField(editing ? existingAgent.getPhone() : "", 18);
        JTextField emailField = new JTextField(editing ? existingAgent.getEmail() : "", 18);
        JTextField registrationField = new JTextField(
                editing ? existingAgent.getVehicleRegistrationNo() : "", 18);
        JTextField vehicleTypeField = new JTextField(
                editing ? existingAgent.getVehicleType() : "", 18);
        JTextField vehicleModelField = new JTextField(
                editing ? existingAgent.getVehicleModel() : "", 18);
        idField.setEditable(!editing);

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 7));
        form.add(new JLabel("Agent ID:"));
        form.add(idField);
        form.add(new JLabel("Full name:"));
        form.add(nameField);
        form.add(new JLabel("Phone:"));
        form.add(phoneField);
        form.add(new JLabel("Email:"));
        form.add(emailField);
        form.add(new JLabel("Vehicle registration:"));
        form.add(registrationField);
        form.add(new JLabel("Vehicle type:"));
        form.add(vehicleTypeField);
        form.add(new JLabel("Vehicle model:"));
        form.add(vehicleModelField);

        int answer = JOptionPane.showConfirmDialog(this, form,
                editing ? "Edit Delivery Agent" : "Add Delivery Agent",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (answer != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            if (editing) {
                service.updateAgent(idField.getText(), nameField.getText(), phoneField.getText(),
                        emailField.getText(), registrationField.getText(),
                        vehicleTypeField.getText(), vehicleModelField.getText());
            } else {
                service.addAgent(idField.getText(), nameField.getText(), phoneField.getText(),
                        emailField.getText(), registrationField.getText(),
                        vehicleTypeField.getText(), vehicleModelField.getText());
            }
            refreshTable();
        } catch (IllegalArgumentException | IOException exception) {
            showError(exception.getMessage());
        }
    }

    private void editSelectedAgent() {
        Task4DeliveryAgent agent = getSelectedAgent();
        if (agent == null) {
            showError("Select a delivery agent first.");
            return;
        }
        showAgentEditor(agent);
    }

    private void removeSelectedAgent() {
        Task4DeliveryAgent agent = getSelectedAgent();
        if (agent == null) {
            showError("Select a delivery agent first.");
            return;
        }

        int answer = JOptionPane.showConfirmDialog(this,
                "Remove " + agent.getName() + " (" + agent.getAgentId() + ")?",
                "Confirm Remove", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            service.removeAgent(agent.getAgentId());
            refreshTable();
        } catch (IllegalArgumentException | IOException exception) {
            showError(exception.getMessage());
        }
    }

    private Task4DeliveryAgent getSelectedAgent() {
        int row = agentTable.getSelectedRow();
        if (row < 0) {
            return null;
        }
        return tableModel.getAgentAt(agentTable.convertRowIndexToModel(row));
    }

    private void refreshTable() {
        tableModel.setAgents(service.getAllAgents());
        countLabel.setText("Delivery agents: " + tableModel.getRowCount());
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Delivery Agent Management",
                JOptionPane.ERROR_MESSAGE);
    }

    private static class AgentTableModel extends AbstractTableModel {

        private final String[] columns = {
            "Agent ID", "Name", "Phone", "Email", "Vehicle Registration", "Vehicle Type", "Vehicle Model"
        };
        private List<Task4DeliveryAgent> agents = List.of();

        void setAgents(List<Task4DeliveryAgent> agents) {
            this.agents = List.copyOf(agents);
            fireTableDataChanged();
        }

        Task4DeliveryAgent getAgentAt(int row) {
            return agents.get(row);
        }

        @Override
        public int getRowCount() {
            return agents.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int column) {
            return columns[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Task4DeliveryAgent agent = agents.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> agent.getAgentId();
                case 1 -> agent.getName();
                case 2 -> agent.getPhone();
                case 3 -> agent.getEmail();
                case 4 -> agent.getVehicleRegistrationNo();
                case 5 -> agent.getVehicleType();
                case 6 -> agent.getVehicleModel();
                default -> "";
            };
        }
    }
}
