package com.mycompany.econesttask3inventory;

import javax.swing.SwingUtilities;

public class Task3EcoNestTask3Inventory {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                Task3InventoryService service = new Task3InventoryService(new Task3InventoryRepository());
                new Task3InventoryFrame(service).setVisible(true);
            } catch (Exception exception) {
                javax.swing.JOptionPane.showMessageDialog(null,
                        "Could not open the inventory data: " + exception.getMessage(),
                        "EcoNest Inventory", javax.swing.JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
