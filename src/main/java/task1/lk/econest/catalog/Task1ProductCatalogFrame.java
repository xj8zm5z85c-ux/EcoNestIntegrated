package lk.econest.catalog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.IOException;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.table.TableRowSorter;

/** Product catalogue screen for EcoNest. */
public final class Task1ProductCatalogFrame extends JFrame {
    private final Task1ProductRepository repository;
    private final Task1ProductTableModel tableModel = new Task1ProductTableModel();
    private final JTable table = new JTable(tableModel);
    private final JTextField searchField = new JTextField(24);
    private final JLabel status = new JLabel(" ");
    private final TableRowSorter<Task1ProductTableModel> sorter = new TableRowSorter<>(tableModel);

    public Task1ProductCatalogFrame(Task1ProductRepository repository) {
        super("EcoNest | Product Catalogue");
        this.repository = repository;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(800, 520));
        setSize(960, 620);
        setLocationRelativeTo(null);

        JLabel title = new JLabel("Product Catalogue");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 25f));
        title.setForeground(new Color(31, 84, 61));
        JLabel subtitle = new JLabel("Manage plants, planters and small-space gardening kits");
        subtitle.setForeground(new Color(94, 108, 99));
        JPanel heading = new JPanel(new BorderLayout(0, 5));
        heading.setBorder(BorderFactory.createEmptyBorder(20, 24, 16, 24));
        heading.add(title, BorderLayout.NORTH);
        heading.add(subtitle, BorderLayout.CENTER);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        toolbar.setBorder(BorderFactory.createEmptyBorder(0, 16, 8, 16));
        toolbar.add(new JLabel("Search"));
        toolbar.add(searchField);
        JButton addButton = new JButton("Add product");
        JButton editButton = new JButton("Edit selected");
        JButton removeButton = new JButton("Remove selected");
        toolbar.add(addButton);
        toolbar.add(editButton);
        toolbar.add(removeButton);

        table.setRowSorter(sorter);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.setRowHeight(30);
        table.getTableHeader().setReorderingAllowed(false);
        table.getColumnModel().getColumn(0).setPreferredWidth(105);
        table.getColumnModel().getColumn(1).setPreferredWidth(260);
        table.getColumnModel().getColumn(2).setPreferredWidth(150);
        table.getColumnModel().getColumn(3).setPreferredWidth(120);
        table.getColumnModel().getColumn(4).setPreferredWidth(100);

        status.setBorder(BorderFactory.createEmptyBorder(8, 24, 12, 24));
        status.setHorizontalAlignment(SwingConstants.LEFT);
        JPanel top = new JPanel(new BorderLayout());
        top.add(heading, BorderLayout.NORTH);
        top.add(toolbar, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);

        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void filter() {
                String query = searchField.getText().trim();
                sorter.setRowFilter(query.isEmpty() ? null : RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(query)));
            }
            @Override public void insertUpdate(javax.swing.event.DocumentEvent event) { filter(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent event) { filter(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent event) { filter(); }
        });
        addButton.addActionListener(event -> addProduct());
        editButton.addActionListener(event -> editProduct());
        removeButton.addActionListener(event -> removeProduct());
        loadProducts();
    }

    private void loadProducts() {
        try {
            tableModel.setProducts(repository.load());
            updateStatus();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "The shared catalogue could not be loaded.\n" + ex.getMessage(),
                    "Database error", JOptionPane.ERROR_MESSAGE);
            status.setText("Catalogue could not be loaded.");
        }
    }

    private void addProduct() {
        Task1ProductDialog dialog = new Task1ProductDialog(this, null);
        dialog.setVisible(true);
        Task1Product product = dialog.getResult();
        if (product == null) return;
        if (findSku(tableModel.getProducts(), product.getSku()) >= 0) {
            JOptionPane.showMessageDialog(this, "A product with this SKU already exists.",
                    "Duplicate SKU", JOptionPane.WARNING_MESSAGE);
            return;
        }
        saveProduct(product);
    }

    private void editProduct() {
        int row = selectedModelRow();
        if (row < 0) return;
        Task1Product existing = tableModel.getProductAt(row);
        Task1ProductDialog dialog = new Task1ProductDialog(this, existing);
        dialog.setVisible(true);
        Task1Product updated = dialog.getResult();
        if (updated == null) return;
        saveProduct(updated);
    }

    private void removeProduct() {
        int row = selectedModelRow();
        if (row < 0) return;
        Task1Product product = tableModel.getProductAt(row);
        int choice = JOptionPane.showConfirmDialog(this,
                "Remove “" + product.getName() + "” (" + product.getSku() + ") from the catalogue?",
                "Confirm removal", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) return;
        try {
            repository.deactivateProduct(product.getSku());
            loadProducts();
        } catch (IOException ex) {
            showSaveError(ex);
        }
    }

    private int selectedModelRow() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Select a product in the table first.",
                    "No product selected", JOptionPane.INFORMATION_MESSAGE);
            return -1;
        }
        return table.convertRowIndexToModel(viewRow);
    }

    private static int findSku(List<Task1Product> products, String sku) {
        for (int i = 0; i < products.size(); i++) {
            if (products.get(i).getSku().equalsIgnoreCase(sku)) return i;
        }
        return -1;
    }

    private void saveProduct(Task1Product product) {
        try {
            repository.saveProduct(product);
            loadProducts();
        } catch (IOException ex) {
            showSaveError(ex);
        }
    }

    private void showSaveError(IOException exception) {
        JOptionPane.showMessageDialog(this, "Changes could not be saved.\n" + exception.getMessage(),
                "Database error", JOptionPane.ERROR_MESSAGE);
    }

    private void updateStatus() {
        int count = tableModel.getRowCount();
        status.setText(count + (count == 1 ? " product in catalogue" : " products in catalogue")
                + "   •   Shared database");
    }
}
