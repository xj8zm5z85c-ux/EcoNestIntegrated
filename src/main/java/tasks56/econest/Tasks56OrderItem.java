package econest;

import java.io.Serializable;
import java.math.BigDecimal;

public class Tasks56OrderItem implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String productId;
    private final String productName;
    private final BigDecimal unitPrice;
    private final int quantity;

    public Tasks56OrderItem(Tasks56Product product, int quantity) {
        this.productId = product.getId();
        this.productName = product.getName();
        this.unitPrice = product.getPriceAmount();
        this.quantity = quantity;
    }
    public String getProductId() { return productId; }
    public String getProductName() { return productName; }
    public double getUnitPrice() { return unitPrice.doubleValue(); }
    public BigDecimal getUnitPriceAmount() { return unitPrice; }
    public int getQuantity() { return quantity; }
    public double getLineTotal() { return getLineTotalAmount().doubleValue(); }
    public BigDecimal getLineTotalAmount() { return unitPrice.multiply(BigDecimal.valueOf(quantity)); }
}
