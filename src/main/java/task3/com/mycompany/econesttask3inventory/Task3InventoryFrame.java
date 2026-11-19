package com.mycompany.econesttask3inventory;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.IOException;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.AbstractTableModel;

/** Swing inventory screen for EcoNest Task 3. */
public class Task3InventoryFrame extends JFrame {

    private final Task3InventoryService service;
    private final InventoryTableModel tableModel = new InventoryTableModel();
    private final JLabel summaryLabel = new JLabel();
    private final JTable inventoryTable = new JTable(tableModel) {
        @Override
        public java.awt.Component prepareRenderer(
                javax.swing.table.TableCellRenderer renderer, int row, int column) {
            java.awt.Component component = super.prepareRenderer(renderer, row, column);
            if (!isRowSelected(row) && tableModel.getItemAt(convertRowIndexToModel(row)).isLowStock()) {
                component.setBackground(new java.awt.Color(255, 232, 232));
            } else if (!isRowSelected(row)) {
                component.setBackground(getBackground());
            }
            return component;
        }
    };

    public Task3InventoryFrame(Task3InventoryService service) {
        super("EcoNest - Inventory Management (Task 3)");
        this.service = service;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(940, 560);
        setLocationRelativeTo(null);

        JPanel topPanel = new JPanel(new BorderLayout(10, 0));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 12, 8, 12));
        summaryLabel.setFont(summaryLabel.getFont().deriveFont(java.awt.Font.BOLD, 14f));
        topPanel.add(summaryLabel, BorderLayout.WEST);
        topPanel.add(createToolbar(), BorderLayout.EAST);

        inventoryTable.setFillsViewportHeight(true);
        inventoryTable.setAutoCreateRowSorter(true);
        inventoryTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(inventoryTable), BorderLayout.CENTER);
        add(new JLabel("Inventory is shared with the product catalogue and order workflow."),
                BorderLayout.SOUTH);

        refreshTable();
    }

    private JPanel createToolbar() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        JButton reorderButton = new JButton("Set Reorder Level");
        JButton stockInButton = new JButton("Record Stock-In");
        JButton historyButton = new JButton("Stock-In History");

        reorderButton.addActionListener(event -> editReorderLevel());
        stockInButton.addActionListener(event -> recordStockIn());
        historyButton.addActionListener(event -> showStockInHistory());

        panel.add(reorderButton);
        panel.add(stockInButton);
        panel.add(historyButton);
        return panel;
    }

    private void editReorderLevel() {
        Task3InventoryItem item = getSelectedItem();
        if (item == null) {
            showError("Select a product first.");
            return;
        }

        String value = JOptionPane.showInputDialog(this,
                "New reorder level for " + item.getProductName() + ":",
                Integer.toString(item.getReorderLevel()));
        if (value == null) {
            return;
        }

        try {
            service.setReorderLevel(item.getProductId(), Integer.parseInt(value.trim()));
            refreshTable();
        } catch (NumberFormatException exception) {
            showError("Enter a whole number for the reorder level.");
        } catch (IllegalArgumentException | IOException exception) {
            showError(exception.getMessage());
        }
    }

    private void recordStockIn() {
        Task3InventoryItem item = getSelectedItem();
        if (item == null) {
            showError("Select a product first.");
            return;
        }

        JTextField supplierField = new JTextField(18);
        JSpinner quantitySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 1_000_000, 1));
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Product:"));
        form.add(new JLabel(item.getProductName() + " (" + item.getProductId() + ")"));
        form.add(new JLabel("Supplier:"));
        form.add(supplierField);
        form.add(new JLabel("Quantity received:"));
        form.add(quantitySpinner);

        int answer = JOptionPane.showConfirmDialog(this, form, "Record Stock-In",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (answer != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            service.recordStockIn(item.getProductId(), supplierField.getText(),
                    (Integer) quantitySpinner.getValue());
            refreshTable();
        } catch (IllegalArgumentException | IOException exception) {
            showError(exception.getMessage());
        }
    }

    private void showStockInHistory() {
        Task3InventoryItem item = getSelectedItem();
        if (item == null) {
            showError("Select a product first.");
            return;
        }

        List<Task3StockInRecord> history = item.getStockInHistory();
        if (history.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No stock-in records for this product yet.",
                    "Stock-In History", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        StringBuilder text = new StringBuilder();
        for (int index = history.size() - 1; index >= 0; index--) {
            text.append(history.get(index)).append('\n');
        }
        JTextArea historyArea = new JTextArea(text.toString(), 10, 48);
        historyArea.setEditable(false);
        JOptionPane.showMessageDialog(this, new JScrollPane(historyArea),
                "Stock-In History - " + item.getProductName(), JOptionPane.INFORMATION_MESSAGE);
    }

    private Task3InventoryItem getSelectedItem() {
        int selectedRow = inventoryTable.getSelectedRow();
        if (selectedRow < 0) {
            return null;
        }
        return tableModel.getItemAt(inventoryTable.convertRowIndexToModel(selectedRow));
    }

    private void refreshTable() {
        try {
            List<Task3InventoryItem> items = service.getAllItems();
            tableModel.setItems(items);
            long lowStockCount = items.stream().filter(Task3InventoryItem::isLowStock).count();
            summaryLabel.setText("Products: " + tableModel.getRowCount()
                    + "     |     Low stock: " + lowStockCount);
        } catch (IOException exception) {
            showError("Could not refresh inventory: " + exception.getMessage());
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Inventory", JOptionPane.ERROR_MESSAGE);
    }

    private static class InventoryTableModel extends AbstractTableModel {

        private final String[] columns = {
            "Product ID", "Product", "Quantity on Hand", "Reorder Level", "Last Supplier", "Status"
        };
        private List<Task3InventoryItem> items = List.of();

        void setItems(List<Task3InventoryItem> items) {
            this.items = List.copyOf(items);
            fireTableDataChanged();
        }

        Task3InventoryItem getItemAt(int row) {
            return items.get(row);
        }

        @Override
        public int getRowCount() {
            return items.size();
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
            Task3InventoryItem item = items.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> item.getProductId();
                case 1 -> item.getProductName();
                case 2 -> item.getQuantityOnHand();
                case 3 -> item.getReorderLevel();
                case 4 -> item.getLastSupplier();
                case 5 -> item.isLowStock() ? "LOW STOCK" : "In stock";
                default -> "";
            };
        }
    }
}
