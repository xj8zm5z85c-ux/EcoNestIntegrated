package econest;

import java.io.Serializable;
import java.math.BigDecimal;
import lk.econest.money.MoneyFormatter;

public class Tasks56Product implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String id;
    private final String name;
    private final BigDecimal price;
    private int quantityOnHand;

    public Tasks56Product(String id, String name, double price, int quantityOnHand) {
        this(id, name, BigDecimal.valueOf(price), quantityOnHand);
    }

    public Tasks56Product(String id, String name, BigDecimal price, int quantityOnHand) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.quantityOnHand = quantityOnHand;
    }
    public String getId() { return id; }
    public String getName() { return name; }
    public double getPrice() { return price.doubleValue(); }
    public BigDecimal getPriceAmount() { return price; }
    public int getQuantityOnHand() { return quantityOnHand; }
    public void reduceStock(int amount) {
        if (amount < 1 || amount > quantityOnHand) throw new IllegalArgumentException("Not enough stock.");
        quantityOnHand -= amount;
    }
    @Override public String toString() { return name + " — " + MoneyFormatter.format(price) + " (stock: " + quantityOnHand + ")"; }
}
