package lk.econest.catalog;

import java.math.BigDecimal;
import java.util.Objects;

/** One item in the EcoNest product catalogue. */
public final class Task1Product {
    private final String sku;
    private final String name;
    private final Task1ProductType type;
    private final BigDecimal price;
    private final int ecoRating;

    public Task1Product(String sku, String name, Task1ProductType type, BigDecimal price, int ecoRating) {
        this.sku = requireText(sku, "SKU").toUpperCase();
        this.name = requireText(name, "Product name");
        this.type = Objects.requireNonNull(type, "Product type is required.");
        this.price = Objects.requireNonNull(price, "Price is required.");
        if (price.signum() <= 0) {
            throw new IllegalArgumentException("Price must be greater than zero.");
        }
        if (ecoRating < 1 || ecoRating > 5) {
            throw new IllegalArgumentException("Eco-rating must be from 1 to 5.");
        }
        this.ecoRating = ecoRating;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required.");
        }
        return value.trim();
    }

    public String getSku() { return sku; }
    public String getName() { return name; }
    public Task1ProductType getType() { return type; }
    public BigDecimal getPrice() { return price; }
    public int getEcoRating() { return ecoRating; }
}
