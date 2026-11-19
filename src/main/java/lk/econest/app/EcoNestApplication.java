package lk.econest.app;

import com.econest.task7.storage.Task7CsvRepository;
import com.econest.task7.ui.Task7MainFrame;
import com.mycompany.econesttask3inventory.Task3InventoryFrame;
import com.mycompany.econesttask3inventory.Task3InventoryRepository;
import com.mycompany.econesttask3inventory.Task3InventoryService;
import com.mycompany.econesttask4deliveryagents.Task4DeliveryAgentFrame;
import com.mycompany.econesttask4deliveryagents.Task4DeliveryAgentRepository;
import com.mycompany.econesttask4deliveryagents.Task4DeliveryAgentService;
import com.mycompany.econesttask8and9.Tasks89NotificationDemoFrame;
import econest.Tasks56EcoNestApp;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import lk.econest.catalog.Task1CsvProductRepository;
import lk.econest.catalog.Task1ProductCatalogFrame;
import views.Task2ClientManagerApp;

/** Single entry point for the task modules combined into the EcoNest desktop project. */
public final class EcoNestApplication extends JFrame {
    private static final Color FOREST = new Color(34, 91, 63);
    private final Map<String, JFrame> openWindows = new LinkedHashMap<>();

    private EcoNestApplication() {
        super("EcoNest | Plant Store Management");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(760, 560));
        setSize(900, 640);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        add(buildHeader(), BorderLayout.NORTH);
        add(buildTaskMenu(), BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(0, 6));
        header.setBackground(FOREST);
        header.setBorder(BorderFactory.createEmptyBorder(24, 28, 23, 28));
        JLabel title = new JLabel("EcoNest");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("SansSerif", Font.BOLD, 30));
        JLabel subtitle = new JLabel("Plant store management system");
        subtitle.setForeground(new Color(220, 238, 225));
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 15));
        JPanel labels = new JPanel(new BorderLayout(0, 5));
        labels.setOpaque(false);
        labels.add(title, BorderLayout.NORTH);
        labels.add(subtitle, BorderLayout.CENTER);
        header.add(labels, BorderLayout.WEST);
        return header;
    }

    private JPanel buildTaskMenu() {
        JPanel area = new JPanel(new BorderLayout(12, 12));
        area.setBackground(new Color(245, 248, 245));
        area.setBorder(BorderFactory.createEmptyBorder(24, 28, 18, 28));
        JLabel instruction = new JLabel("Choose a feature to open");
        instruction.setFont(new Font("SansSerif", Font.BOLD, 18));
        instruction.setForeground(new Color(47, 65, 52));
        area.add(instruction, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(0, 2, 14, 14));
        grid.setOpaque(false);
        addModuleButton(grid, "Product Catalogue", "Product Catalogue", () -> new Task1ProductCatalogFrame(
                new Task1CsvProductRepository(Path.of(System.getProperty("user.home"), ".econest", "products.csv"))));
        addModuleButton(grid, "Clients", "Clients", Task2ClientManagerApp::new);
        addModuleButton(grid, "Inventory", "Inventory", EcoNestApplication::createInventoryFrame);
        addModuleButton(grid, "Delivery Agents", "Delivery Agents", EcoNestApplication::createDeliveryAgentFrame);
        addModuleButton(grid, "Orders and Deliveries", "Orders and Deliveries", Tasks56EcoNestApp::new);
        addModuleButton(grid, "Monthly Reports", "Monthly Reports", () -> new Task7MainFrame(new Task7CsvRepository()));
        addModuleButton(grid, "Email Notifications", "Email Notifications", Tasks89NotificationDemoFrame::new);
        area.add(grid, BorderLayout.CENTER);
        return area;
    }

    private static JFrame createInventoryFrame() {
        try {
            return new Task3InventoryFrame(new Task3InventoryService(new Task3InventoryRepository()));
        } catch (java.io.IOException | ClassNotFoundException exception) {
            throw new IllegalStateException("Could not initialize inventory storage.", exception);
        }
    }

    private static JFrame createDeliveryAgentFrame() {
        try {
            return new Task4DeliveryAgentFrame(new Task4DeliveryAgentService(new Task4DeliveryAgentRepository()));
        } catch (java.io.IOException | ClassNotFoundException exception) {
            throw new IllegalStateException("Could not initialize delivery-agent storage.", exception);
        }
    }

    private void addModuleButton(JPanel panel, String text, String key, Supplier<JFrame> factory) {
        JButton button = new JButton("<html><div style='text-align:left'><b>" + text
                + "</b><br><span style='color:#536357'>Open module</span></div></html>");
        button.setHorizontalAlignment(JButton.LEFT);
        button.setFocusPainted(false);
        button.setBackground(Color.WHITE);
        button.setForeground(FOREST);
        button.setFont(new Font("SansSerif", Font.PLAIN, 15));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(212, 225, 215)),
                BorderFactory.createEmptyBorder(15, 18, 15, 18)));
        button.addActionListener(event -> openModule(key, factory));
        panel.add(button);
    }

    private void openModule(String key, Supplier<JFrame> factory) {
        try {
            JFrame window = openWindows.get(key);
            if (window == null || !window.isDisplayable()) {
                window = factory.get();
                window.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
                openWindows.put(key, window);
            }
            window.setVisible(true);
            window.setState(JFrame.NORMAL);
            window.toFront();
        } catch (Exception exception) {
            JOptionPane.showMessageDialog(this,
                    "Could not open " + key + ": " + exception.getMessage(),
                    "EcoNest", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // Swing's default look and feel remains available.
            }
            new EcoNestApplication().setVisible(true);
        });
    }
}
