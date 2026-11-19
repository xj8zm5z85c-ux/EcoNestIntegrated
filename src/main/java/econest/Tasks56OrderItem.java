package econest;

import java.io.Serializable;

public class Tasks56OrderItem implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String productId;
    private final String productName;
    private final double unitPrice;
    private final int quantity;

    public Tasks56OrderItem(Tasks56Product product, int quantity) {
        this.productId = product.getId();
        this.productName = product.getName();
        this.unitPrice = product.getPrice();
        this.quantity = quantity;
    }
    public String getProductId() { return productId; }
    public String getProductName() { return productName; }
    public double getUnitPrice() { return unitPrice; }
    public int getQuantity() { return quantity; }
    public double getLineTotal() { return unitPrice * quantity; }
}
