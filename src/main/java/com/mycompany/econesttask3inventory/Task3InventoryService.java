package com.mycompany.econesttask3inventory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.time.LocalDateTime;

/** Business rules for product stock, reorder levels, and supplier stock-ins. */
public class Task3InventoryService {

    private final Task3InventoryRepository repository;
    private final List<Task3InventoryItem> inventory;

    public Task3InventoryService(Task3InventoryRepository repository)
            throws IOException, ClassNotFoundException {
        if (repository == null) {
            throw new IllegalArgumentException("Inventory repository is required.");
        }
        this.repository = repository;
        this.inventory = new ArrayList<>(repository.loadAll());
    }

    public List<Task3InventoryItem> getAllItems() {
        List<Task3InventoryItem> result = new ArrayList<>(inventory);
        result.sort(Comparator.comparing(Task3InventoryItem::getProductName,
                String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    public List<Task3InventoryItem> getLowStockItems() {
        List<Task3InventoryItem> result = new ArrayList<>();
        for (Task3InventoryItem item : inventory) {
            if (item.isLowStock()) {
                result.add(item);
            }
        }
        return result;
    }

    public void addProduct(String productId, String productName,
            int initialQuantity, int reorderLevel) throws IOException {
        String id = requireText(productId, "Product ID");
        String name = requireText(productName, "Product name");
        requireNonNegative(initialQuantity, "Initial quantity");
        requireNonNegative(reorderLevel, "Reorder level");

        if (findItem(id) != null) {
            throw new IllegalArgumentException("That product ID already exists.");
        }
        inventory.add(new Task3InventoryItem(id, name, initialQuantity, reorderLevel));
        persist();
    }

    public void setReorderLevel(String productId, int reorderLevel) throws IOException {
        requireNonNegative(reorderLevel, "Reorder level");
        Task3InventoryItem item = requireItem(productId);
        item.setReorderLevel(reorderLevel);
        persist();
    }

    public void recordStockIn(String productId, String supplierName, int quantity)
            throws IOException {
        String supplier = requireText(supplierName, "Supplier name");
        if (quantity <= 0) {
            throw new IllegalArgumentException("Stock-in quantity must be greater than zero.");
        }

        Task3InventoryItem item = requireItem(productId);
        item.receiveStock(new Task3StockInRecord(supplier, quantity, LocalDateTime.now()));
        persist();
    }

    public Task3InventoryItem getItem(String productId) {
        return requireItem(productId);
    }

    private Task3InventoryItem requireItem(String productId) {
        Task3InventoryItem item = findItem(requireText(productId, "Product ID"));
        if (item == null) {
            throw new IllegalArgumentException("Product was not found.");
        }
        return item;
    }

    private Task3InventoryItem findItem(String productId) {
        for (Task3InventoryItem item : inventory) {
            if (item.getProductId().equalsIgnoreCase(productId)) {
                return item;
            }
        }
        return null;
    }

    private void persist() throws IOException {
        repository.saveAll(inventory);
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        return value.trim();
    }

    private static void requireNonNegative(int value, String label) {
        if (value < 0) {
            throw new IllegalArgumentException(label + " cannot be negative.");
        }
    }
}
