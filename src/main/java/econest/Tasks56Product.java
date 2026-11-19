package econest;

import java.io.Serializable;

public class Tasks56Product implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String id;
    private final String name;
    private final double price;
    private int quantityOnHand;

    public Tasks56Product(String id, String name, double price, int quantityOnHand) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.quantityOnHand = quantityOnHand;
    }
    public String getId() { return id; }
    public String getName() { return name; }
    public double getPrice() { return price; }
    public int getQuantityOnHand() { return quantityOnHand; }
    public void reduceStock(int amount) {
        if (amount < 1 || amount > quantityOnHand) throw new IllegalArgumentException("Not enough stock.");
        quantityOnHand -= amount;
    }
    @Override public String toString() { return name + " — LKR " + String.format("%,.2f", price) + " (stock: " + quantityOnHand + ")"; }
}
