package com.mycompany.econesttask3inventory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Product stock, reorder threshold, and its supplier stock-in history. */
public class Task3InventoryItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String productId;
    private final String productName;
    private int quantityOnHand;
    private int reorderLevel;
    private final List<Task3StockInRecord> stockInHistory = new ArrayList<>();

    public Task3InventoryItem(String productId, String productName,
            int quantityOnHand, int reorderLevel) {
        this.productId = productId;
        this.productName = productName;
        this.quantityOnHand = quantityOnHand;
        this.reorderLevel = reorderLevel;
    }

    public String getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public int getQuantityOnHand() {
        return quantityOnHand;
    }

    public int getReorderLevel() {
        return reorderLevel;
    }

    public List<Task3StockInRecord> getStockInHistory() {
        return Collections.unmodifiableList(stockInHistory);
    }

    public boolean isLowStock() {
        return quantityOnHand <= reorderLevel;
    }

    public String getLastSupplier() {
        if (stockInHistory.isEmpty()) {
            return "—";
        }
        return stockInHistory.get(stockInHistory.size() - 1).getSupplierName();
    }

    void setReorderLevel(int reorderLevel) {
        this.reorderLevel = reorderLevel;
    }

    void receiveStock(Task3StockInRecord record) {
        quantityOnHand += record.getQuantityReceived();
        stockInHistory.add(record);
    }

    void addHistoricalStock(Task3StockInRecord record) {
        stockInHistory.add(record);
    }

    void setQuantityOnHand(int quantityOnHand) {
        this.quantityOnHand = quantityOnHand;
    }

    void setReorderLevelFromStorage(int reorderLevel) {
        this.reorderLevel = reorderLevel;
    }
}
