package com.econest.task7.ui;

import com.econest.task7.model.Task7ProductStock;
import com.econest.task7.model.Task7Sale;
import com.econest.task7.service.Task7MonthlyReport;
import com.econest.task7.storage.Task7DatabaseRepository;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import lk.econest.money.MoneyFormatter;

public final class Task7MainFrame extends JFrame {
    private final Task7DatabaseRepository repository;
    private final List<Task7ProductStock> inventory = new ArrayList<>();
    private final List<Task7Sale> sales = new ArrayList<>();
    private final DefaultTableModel inventoryModel = model("SKU", "Product", "Quantity", "Reorder level", "Unit price (LKR)");
    private final DefaultTableModel salesModel = model("Order ID", "Date", "Client", "Total (LKR)");
    private final JTable inventoryTable = table(inventoryModel);
    private final JTable salesTable = table(salesModel);
    private final JTextField monthField = new JTextField(7);
    private final JTextArea reportArea = new JTextArea();
    private Task7MonthlyReport latestReport;

    public Task7MainFrame(Task7DatabaseRepository repository) {
        super("EcoNest | Monthly Sales & Inventory Reports");
        this.repository = repository;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1040, 650);
        setMinimumSize(new java.awt.Dimension(820, 520));
        setLocationRelativeTo(null);
        monthField.setText(YearMonth.now().toString());
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Inventory", inventoryPanel());
        tabs.addTab("Sales", salesPanel());
        tabs.addTab("Monthly Reports", reportPanel());
        add(tabs, BorderLayout.CENTER);
        loadData();
    }

    private JPanel inventoryPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        panel.add(new JScrollPane(inventoryTable), BorderLayout.CENTER);
        panel.add(new JLabel("Live shared inventory. Manage products in Product Catalogue and stock in Inventory."),
                BorderLayout.SOUTH);
        return panel;
    }

    private JPanel salesPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        panel.add(new JScrollPane(salesTable), BorderLayout.CENTER);
        panel.add(new JLabel("Sales are derived from persisted orders and cannot be edited separately."),
                BorderLayout.SOUTH);
        return panel;
    }

    private JPanel reportPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.add(new JLabel("Report month (yyyy-MM)")); controls.add(monthField);
        JButton generate = new JButton("Generate report");
        JButton export = new JButton("Export CSV");
        generate.addActionListener(e -> generateReport());
        export.addActionListener(e -> exportReport());
        controls.add(generate); controls.add(export);
        panel.add(controls, BorderLayout.NORTH);
        reportArea.setEditable(false);
        reportArea.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 14));
        reportArea.setText("Choose a month and select Generate report.\n\n"
                + "The revenue summary uses sales recorded for that month.\n"
                + "Low-stock alerts use the current inventory quantities.");
        panel.add(new JScrollPane(reportArea), BorderLayout.CENTER);
        return panel;
    }

    private void loadData() {
        try {
            inventory.addAll(repository.loadInventory());
            sales.addAll(repository.loadSales());
            refreshTables();
        } catch (IOException | RuntimeException ex) {
            showError("Could not load shared database data.\n" + ex.getMessage());
        }
    }

    private void generateReport() {
        try {
            YearMonth month = YearMonth.parse(monthField.getText().trim());
            inventory.clear();
            inventory.addAll(repository.loadInventory());
            sales.clear();
            sales.addAll(repository.loadSales());
            refreshTables();
            latestReport = Task7MonthlyReport.create(month, sales, inventory);
            StringBuilder text = new StringBuilder();
            text.append("ECONEST MONTHLY SALES AND INVENTORY REPORT\n");
            text.append("Report month: ").append(month).append("\n\n");
            text.append("SALES SUMMARY\n");
            text.append("Non-cancelled orders: ").append(latestReport.orderCount()).append('\n');
            text.append("Total revenue:     ").append(MoneyFormatter.format(latestReport.revenue())).append('\n');
            text.append("Average per order: ").append(MoneyFormatter.format(latestReport.averageOrderValue())).append("\n\n");
            text.append("LOW-STOCK ALERTS (current inventory)\n");
            if (latestReport.lowStockProducts().isEmpty()) {
                text.append("No products are at or below their reorder level.\n");
            } else {
                text.append(String.format("%-14s %-28s %10s %14s%n", "SKU", "Product", "On hand", "Reorder at"));
                text.append("-".repeat(72)).append('\n');
                for (Task7ProductStock p : latestReport.lowStockProducts()) {
                    text.append(String.format("%-14s %-28s %10d %14d%n", p.sku(), truncate(p.name(), 28), p.quantity(), p.reorderLevel()));
                }
            }
            reportArea.setText(text.toString()); reportArea.setCaretPosition(0);
        } catch (DateTimeParseException ex) {
            showError("Enter a report month in yyyy-MM format, for example 2026-11.");
        } catch (IOException | RuntimeException ex) {
            showError("Could not refresh report data from the database.\n" + ex.getMessage());
        }
    }

    private void exportReport() {
        if (latestReport == null) { showError("Generate a report before exporting it."); return; }
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new java.io.File("econest-report-" + latestReport.month() + ".csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            List<String> lines = new ArrayList<>();
            lines.add("section,month,metric,value");
            lines.add("Sales," + latestReport.month() + ",Completed orders," + latestReport.orderCount());
            lines.add("Sales," + latestReport.month() + ",Total revenue LKR," + latestReport.revenue().toPlainString());
            lines.add("Sales," + latestReport.month() + ",Average order value LKR," + latestReport.averageOrderValue().toPlainString());
            for (Task7ProductStock p : latestReport.lowStockProducts()) {
                lines.add("Inventory," + latestReport.month() + "," + csv("Low stock SKU " + p.sku()) + ","
                        + csv(p.name() + " | quantity=" + p.quantity() + " | reorder=" + p.reorderLevel()));
            }
            if (latestReport.lowStockProducts().isEmpty()) lines.add("Inventory," + latestReport.month() + ",Low-stock products,0");
            Files.write(chooser.getSelectedFile().toPath(), lines, StandardCharsets.UTF_8);
            JOptionPane.showMessageDialog(this, "Report exported successfully.", "Export complete", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) { showError("Could not export the report: " + ex.getMessage()); }
    }

    private void refreshTables() { refreshInventoryTable(); refreshSalesTable(); }

    private void refreshInventoryTable() {
        inventoryModel.setRowCount(0);
        for (Task7ProductStock p : inventory) inventoryModel.addRow(new Object[]{
            p.sku(), p.name(), p.quantity(), p.reorderLevel(), MoneyFormatter.format(p.unitPrice())
        });
    }

    private void refreshSalesTable() {
        salesModel.setRowCount(0);
        for (Task7Sale s : sales) salesModel.addRow(new Object[]{
            s.orderId(), s.date(), s.clientName(), MoneyFormatter.format(s.total())
        });
    }

    private static DefaultTableModel model(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
    }

    private static JTable table(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        return table;
    }

    private static String truncate(String text, int max) { return text.length() <= max ? text : text.substring(0, max - 3) + "..."; }
    private static String csv(String text) { return "\"" + text.replace("\"", "\"\"") + "\""; }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message == null ? "Please check the entered values." : message,
                "EcoNest", JOptionPane.ERROR_MESSAGE);
    }
}
