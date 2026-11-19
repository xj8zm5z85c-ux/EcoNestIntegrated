package lk.econest.catalog;

import java.nio.file.Path;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/** Application entry point. */
public final class Task1Main {
    private Task1Main() { }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // Keep Swing's default look and feel if the system theme is unavailable.
            }
            Path dataFile = Path.of(System.getProperty("user.home"), ".econest", "products.csv");
            new Task1ProductCatalogFrame(new Task1CsvProductRepository(dataFile)).setVisible(true);
        });
    }
}
