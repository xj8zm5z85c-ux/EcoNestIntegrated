package com.mycompany.econesttask3inventory;

import java.io.Serializable;
import java.time.LocalDateTime;

/** A record of stock received from a supplier for one product. */
public class Task3StockInRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String supplierName;
    private final int quantityReceived;
    private final LocalDateTime receivedAt;

    public Task3StockInRecord(String supplierName, int quantityReceived, LocalDateTime receivedAt) {
        this.supplierName = supplierName;
        this.quantityReceived = quantityReceived;
        this.receivedAt = receivedAt;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public int getQuantityReceived() {
        return quantityReceived;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    @Override
    public String toString() {
        return receivedAt.toString().replace('T', ' ')
                + " | " + supplierName + " | +" + quantityReceived + " units";
    }
}
