package econest;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SpinnerDateModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;

/** EcoNest desktop workflow for processing client orders and assigning deliveries. */
public class Tasks56EcoNestApp extends JFrame {
    private static final Color FOREST = new Color(37, 92, 67);
    private static final Color PALE = new Color(242, 248, 243);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final Tasks56EcoNestStore store;
    private final Map<Tasks56Product, Integer> cart = new LinkedHashMap<Tasks56Product, Integer>();
    private JComboBox<Tasks56Client> clientBox;
    private JComboBox<Tasks56Product> productBox;
    private JSpinner quantitySpinner;
    private DefaultTableModel cartModel;
    private JLabel totalLabel;
    private JTable ordersTable;
    private DefaultTableModel ordersModel;
    private JTable deliveryTable;
    private DefaultTableModel deliveryModel;
    private JComboBox<Tasks56DeliveryAgent> agentBox;
    private JSpinner deliveryDateSpinner;
    private JComboBox<Tasks56DeliveryStatus> statusBox;

    public Tasks56EcoNestApp() {
        super("EcoNest | Orders & Deliveries");
        store = Tasks56EcoNestStore.load();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(960, 650));
        setSize(1120, 730);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        add(buildHeader(), BorderLayout.NORTH);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Process Orders", buildOrdersTab());
        tabs.addTab("Delivery Assignments", buildDeliveriesTab());
        tabs.setFont(new Font("SansSerif", Font.BOLD, 13));
        add(tabs, BorderLayout.CENTER);
        refreshOrders();
        refreshDeliveries();
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(10, 4));
        header.setBackground(FOREST);
        header.setBorder(BorderFactory.createEmptyBorder(17, 24, 17, 24));
        JLabel title = new JLabel("EcoNest");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("SansSerif", Font.BOLD, 25));
        JLabel subtitle = new JLabel("Client order processing  ·  Delivery scheduling and tracking");
        subtitle.setForeground(new Color(224, 239, 227));
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        JPanel text = new JPanel(new BorderLayout(0, 3));
        text.setOpaque(false);
        text.add(title, BorderLayout.NORTH);
        text.add(subtitle, BorderLayout.CENTER);
        header.add(text, BorderLayout.WEST);
        return header;
    }

    private JPanel buildOrdersTab() {
        JPanel page = pagePanel();
        JPanel create = cardPanel("Create a client order");
        clientBox = new JComboBox<Tasks56Client>(store.getClients().toArray(new Tasks56Client[0]));
        productBox = new JComboBox<Tasks56Product>(store.getProducts().toArray(new Tasks56Product[0]));
        quantitySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 999, 1));
        styleCombo(clientBox); styleCombo(productBox); quantitySpinner.setPreferredSize(new Dimension(78, 34));
        JButton addItem = primaryButton("Add to order");
        addItem.addActionListener(e -> addItemToCart());
        JPanel fields = new JPanel(new GridBagLayout()); fields.setOpaque(false);
        GridBagConstraints c = constraints();
        addField(fields, c, 0, "Client", clientBox, 2);
        addField(fields, c, 2, "Product", productBox, 2);
        addField(fields, c, 4, "Quantity", quantitySpinner, 1);
        c.gridx = 5; c.gridy = 1; c.gridwidth = 1; c.weightx = 0; c.anchor = GridBagConstraints.SOUTHWEST;
        fields.add(addItem, c);
        create.add(fields, BorderLayout.NORTH);

        cartModel = readOnlyModel(new String[]{"Product", "Unit price", "Qty", "Line total"});
        JTable cartTable = makeTable(cartModel);
        JScrollPane cartScroll = new JScrollPane(cartTable); cartScroll.setPreferredSize(new Dimension(400, 155));
        create.add(cartScroll, BorderLayout.CENTER);
        JPanel cartFooter = new JPanel(new BorderLayout()); cartFooter.setOpaque(false);
        totalLabel = new JLabel("Order total: " + formatMoney(0));
        totalLabel.setFont(new Font("SansSerif", Font.BOLD, 17)); totalLabel.setForeground(FOREST);
        JButton removeItem = secondaryButton("Remove selected item");
        removeItem.addActionListener(e -> removeCartItem(cartTable.getSelectedRow()));
        JButton placeOrder = primaryButton("Place order");
        placeOrder.addActionListener(e -> placeOrder());
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0)); right.setOpaque(false);
        right.add(removeItem); right.add(placeOrder);
        cartFooter.add(totalLabel, BorderLayout.WEST); cartFooter.add(right, BorderLayout.EAST);
        cartFooter.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        create.add(cartFooter, BorderLayout.SOUTH);

        JPanel register = cardPanel("Recent orders");
        ordersModel = readOnlyModel(new String[]{"Order ID", "Date", "Client", "Items", "Total", "Delivery status"});
        ordersTable = makeTable(ordersModel);
        register.add(new JScrollPane(ordersTable), BorderLayout.CENTER);
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0)); bottom.setOpaque(false);
        JButton details = secondaryButton("View order details");
        details.addActionListener(e -> showOrderDetails(ordersTable.getSelectedRow()));
        bottom.add(details); register.add(bottom, BorderLayout.SOUTH);
        page.add(create, BorderLayout.NORTH); page.add(register, BorderLayout.CENTER);
        return page;
    }

    private JPanel buildDeliveriesTab() {
        JPanel page = pagePanel();
        JPanel assignment = cardPanel("Assign agent and schedule delivery");
        agentBox = new JComboBox<Tasks56DeliveryAgent>(store.getAgents().toArray(new Tasks56DeliveryAgent[0]));
        styleCombo(agentBox);
        deliveryDateSpinner = new JSpinner(new SpinnerDateModel(new Date(), null, null, java.util.Calendar.DAY_OF_MONTH));
        deliveryDateSpinner.setEditor(new JSpinner.DateEditor(deliveryDateSpinner, "yyyy-MM-dd"));
        deliveryDateSpinner.setPreferredSize(new Dimension(130, 34));
        statusBox = new JComboBox<Tasks56DeliveryStatus>(new Tasks56DeliveryStatus[]{Tasks56DeliveryStatus.ASSIGNED, Tasks56DeliveryStatus.DISPATCHED, Tasks56DeliveryStatus.DELIVERED});
        styleCombo(statusBox);
        JPanel controls = new JPanel(new GridBagLayout()); controls.setOpaque(false);
        GridBagConstraints c = constraints();
        addField(controls, c, 0, "Delivery agent", agentBox, 1);
        addField(controls, c, 1, "Scheduled date", deliveryDateSpinner, 1);
        addField(controls, c, 2, "Update status", statusBox, 1);
        JButton assign = primaryButton("Assign to selected order");
        assign.addActionListener(e -> assignDelivery());
        JButton update = secondaryButton("Update status");
        update.addActionListener(e -> updateStatus());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); actions.setOpaque(false);
        actions.add(assign); actions.add(update);
        c.gridx = 3; c.gridy = 1; c.gridwidth = 2; c.weightx = 1; c.anchor = GridBagConstraints.SOUTHWEST;
        controls.add(actions, c);
        assignment.add(controls, BorderLayout.CENTER);

        JPanel list = cardPanel("Orders and delivery progress");
        deliveryModel = readOnlyModel(new String[]{"Order ID", "Client", "Delivery agent", "Vehicle", "Scheduled date", "Status"});
        deliveryTable = makeTable(deliveryModel);
        list.add(new JScrollPane(deliveryTable), BorderLayout.CENTER);
        JLabel hint = new JLabel("Select an order before assigning an agent or changing its delivery status.");
        hint.setForeground(new Color(95, 105, 99)); hint.setBorder(BorderFactory.createEmptyBorder(9, 2, 0, 2));
        list.add(hint, BorderLayout.SOUTH);
        page.add(assignment, BorderLayout.NORTH); page.add(list, BorderLayout.CENTER);
        return page;
    }

    private void addItemToCart() {
        Tasks56Product product = (Tasks56Product) productBox.getSelectedItem();
        if (product == null) return;
        int quantity = (Integer) quantitySpinner.getValue();
        int alreadyAdded = cart.containsKey(product) ? cart.get(product) : 0;
        if (alreadyAdded + quantity > product.getQuantityOnHand()) {
            showError("Only " + product.getQuantityOnHand() + " unit(s) are currently in stock."); return;
        }
        cart.put(product, alreadyAdded + quantity);
        refreshCart();
    }

    private void removeCartItem(int row) {
        if (row < 0 || row >= cart.size()) return;
        Tasks56Product selected = new ArrayList<Tasks56Product>(cart.keySet()).get(row);
        cart.remove(selected); refreshCart();
    }

    private void refreshCart() {
        cartModel.setRowCount(0);
        double total = 0;
        for (Map.Entry<Tasks56Product, Integer> entry : cart.entrySet()) {
            double line = entry.getKey().getPrice() * entry.getValue(); total += line;
            cartModel.addRow(new Object[]{entry.getKey().getName(), formatMoney(entry.getKey().getPrice()), entry.getValue(), formatMoney(line)});
        }
        totalLabel.setText("Order total: " + formatMoney(total));
    }

    private void placeOrder() {
        Tasks56Client client = (Tasks56Client) clientBox.getSelectedItem();
        if (client == null) { showError("Add or select a client before placing an order."); return; }
        if (cart.isEmpty()) { showError("Add at least one product to the order."); return; }
        for (Map.Entry<Tasks56Product, Integer> entry : cart.entrySet()) {
            if (entry.getValue() > entry.getKey().getQuantityOnHand()) { showError("Stock changed for " + entry.getKey().getName() + ". Review the order quantities."); return; }
        }
        List<Tasks56OrderItem> items = new ArrayList<Tasks56OrderItem>();
        for (Map.Entry<Tasks56Product, Integer> entry : cart.entrySet()) {
            items.add(new Tasks56OrderItem(entry.getKey(), entry.getValue()));
            entry.getKey().reduceStock(entry.getValue());
        }
        String id = String.format("ORD-%04d", store.getOrders().size() + 1);
        Tasks56ClientOrder order = new Tasks56ClientOrder(id, client, items);
        store.getOrders().add(order);
        try { store.save(); }
        catch (IllegalStateException ex) { showError(ex.getMessage()); return; }
        cart.clear(); refreshCart(); refreshOrders(); refreshDeliveries(); refreshProductChoices();
        JOptionPane.showMessageDialog(this, "Order " + id + " has been placed.\nTotal: " + formatMoney(order.getTotal()), "Order placed", JOptionPane.INFORMATION_MESSAGE);
    }

    private void assignDelivery() {
        Tasks56ClientOrder order = selectedOrder(deliveryTable);
        if (order == null) { showError("Select an order from the delivery list first."); return; }
        Tasks56DeliveryAgent agent = (Tasks56DeliveryAgent) agentBox.getSelectedItem();
        Date selectedDate = (Date) deliveryDateSpinner.getValue();
        LocalDate date = selectedDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        try {
            order.assignDelivery(agent, date);
            store.save(); refreshOrders(); refreshDeliveries();
            JOptionPane.showMessageDialog(this, "" + agent.getName() + " is assigned to " + order.getId() + " for " + date.format(DATE_FORMAT) + ".", "Delivery assigned", JOptionPane.INFORMATION_MESSAGE);
        } catch (IllegalArgumentException | IllegalStateException ex) { showError(ex.getMessage()); }
    }

    private void updateStatus() {
        Tasks56ClientOrder order = selectedOrder(deliveryTable);
        if (order == null) { showError("Select an order from the delivery list first."); return; }
        Tasks56DeliveryStatus status = (Tasks56DeliveryStatus) statusBox.getSelectedItem();
        try {
            order.setDeliveryStatus(status);
            store.save(); refreshOrders(); refreshDeliveries();
        } catch (IllegalArgumentException | IllegalStateException ex) { showError(ex.getMessage()); }
    }

    private Tasks56ClientOrder selectedOrder(JTable table) {
        int row = table.getSelectedRow();
        if (row < 0) return null;
        int modelRow = table.convertRowIndexToModel(row);
        String orderId = String.valueOf(table.getModel().getValueAt(modelRow, 0));
        for (Tasks56ClientOrder order : store.getOrders()) if (order.getId().equals(orderId)) return order;
        return null;
    }

    private void showOrderDetails(int viewRow) {
        if (viewRow < 0) { showError("Select an order to view its details."); return; }
        int row = ordersTable.convertRowIndexToModel(viewRow);
        String id = String.valueOf(ordersModel.getValueAt(row, 0));
        for (Tasks56ClientOrder order : store.getOrders()) if (order.getId().equals(id)) {
            StringBuilder details = new StringBuilder("Order ").append(order.getId()).append("\nClient: ").append(order.getClient().getName()).append("\nCreated: ").append(order.getCreatedDate()).append("\n\nItems:\n");
            for (Tasks56OrderItem item : order.getItems()) details.append("• ").append(item.getProductName()).append("  × ").append(item.getQuantity()).append("  —  ").append(formatMoney(item.getLineTotal())).append("\n");
            details.append("\nOrder total: ").append(formatMoney(order.getTotal()));
            if (order.getDeliveryAgent() != null) details.append("\nDelivery: ").append(order.getDeliveryAgent().getName()).append(" on ").append(order.getDeliveryDate()).append(" ( ").append(order.getDeliveryStatus()).append(" )");
            JOptionPane.showMessageDialog(this, details.toString(), "Order details", JOptionPane.INFORMATION_MESSAGE); return;
        }
    }

    private void refreshOrders() {
        if (ordersModel == null) return;
        ordersModel.setRowCount(0);
        for (int i = store.getOrders().size() - 1; i >= 0; i--) {
            Tasks56ClientOrder order = store.getOrders().get(i);
            ordersModel.addRow(new Object[]{order.getId(), order.getCreatedDate().format(DATE_FORMAT), order.getClient().getName(), order.getItems().size(), formatMoney(order.getTotal()), order.getDeliveryStatus()});
        }
    }

    private void refreshDeliveries() {
        if (deliveryModel == null) return;
        deliveryModel.setRowCount(0);
        for (int i = store.getOrders().size() - 1; i >= 0; i--) {
            Tasks56ClientOrder order = store.getOrders().get(i);
            Tasks56DeliveryAgent agent = order.getDeliveryAgent();
            deliveryModel.addRow(new Object[]{order.getId(), order.getClient().getName(), agent == null ? "—" : agent.getName(), agent == null ? "—" : agent.getVehicle(), order.getDeliveryDate() == null ? "—" : order.getDeliveryDate().format(DATE_FORMAT), order.getDeliveryStatus()});
        }
    }

    private void refreshProductChoices() {
        Tasks56Product selected = (Tasks56Product) productBox.getSelectedItem();
        productBox.removeAllItems();
        for (Tasks56Product product : store.getProducts()) productBox.addItem(product);
        if (selected != null) productBox.setSelectedItem(selected);
    }

    private JPanel pagePanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 12)); panel.setBackground(PALE);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18)); return panel;
    }

    private JPanel cardPanel(String title) {
        JPanel panel = new JPanel(new BorderLayout(0, 12)); panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(220, 230, 222)), BorderFactory.createEmptyBorder(14, 16, 14, 16)));
        JLabel label = new JLabel(title); label.setForeground(FOREST); label.setFont(new Font("SansSerif", Font.BOLD, 16));
        panel.add(label, BorderLayout.NORTH); return panel;
    }

    private GridBagConstraints constraints() {
        GridBagConstraints c = new GridBagConstraints(); c.insets = new Insets(4, 5, 7, 10); c.fill = GridBagConstraints.HORIZONTAL; c.anchor = GridBagConstraints.NORTHWEST; return c;
    }

    private void addField(JPanel panel, GridBagConstraints c, int column, String label, java.awt.Component input, int width) {
        JLabel caption = new JLabel(label); caption.setFont(new Font("SansSerif", Font.BOLD, 12)); caption.setForeground(new Color(66, 78, 70));
        c.gridx = column; c.gridy = 0; c.gridwidth = width; c.weightx = width; c.insets = new Insets(3, 5, 2, 10); panel.add(caption, c);
        c.gridy = 1; c.insets = new Insets(0, 5, 7, 10); panel.add(input, c);
    }

    private void styleCombo(JComboBox<?> combo) { combo.setPreferredSize(new Dimension(190, 34)); }
    private JButton primaryButton(String text) {
        JButton button = new JButton(text); button.setBackground(FOREST); button.setForeground(Color.WHITE); button.setFocusPainted(false); button.setFont(new Font("SansSerif", Font.BOLD, 12)); button.setBorder(BorderFactory.createEmptyBorder(9, 15, 9, 15)); return button;
    }
    private JButton secondaryButton(String text) {
        JButton button = new JButton(text); button.setForeground(FOREST); button.setFocusPainted(false); button.setFont(new Font("SansSerif", Font.BOLD, 12)); button.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(174, 198, 179)), BorderFactory.createEmptyBorder(8, 13, 8, 13))); return button;
    }
    private DefaultTableModel readOnlyModel(String[] columns) {
        return new DefaultTableModel(columns, 0) { @Override public boolean isCellEditable(int row, int column) { return false; } };
    }
    private JTable makeTable(DefaultTableModel model) {
        JTable table = new JTable(model); table.setRowHeight(29); table.setFont(new Font("SansSerif", Font.PLAIN, 12));
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12)); table.getTableHeader().setBackground(new Color(231, 241, 233)); table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION); return table;
    }
    private String formatMoney(double value) { return "LKR " + String.format("%,.2f", value); }
    private void showError(String message) { JOptionPane.showMessageDialog(this, message, "Please check", JOptionPane.WARNING_MESSAGE); }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) { }
        SwingUtilities.invokeLater(() -> new Tasks56EcoNestApp().setVisible(true));
    }
}
