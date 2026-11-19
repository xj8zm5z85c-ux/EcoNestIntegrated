package lk.econest.catalog;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;

/** Read-only table view of catalogue products. */
public final class Task1ProductTableModel extends AbstractTableModel {
    private static final String[] COLUMNS = {"SKU", "Product name", "Category", "Price", "Eco-rating"};
    private final List<Task1Product> products = new ArrayList<>();
    private final NumberFormat currency = NumberFormat.getCurrencyInstance();

    @Override
    public int getRowCount() { return products.size(); }

    @Override
    public int getColumnCount() { return COLUMNS.length; }

    @Override
    public String getColumnName(int column) { return COLUMNS[column]; }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Task1Product product = products.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> product.getSku();
            case 1 -> product.getName();
            case 2 -> product.getType();
            case 3 -> currency.format(product.getPrice());
            case 4 -> product.getEcoRating() + " / 5";
            default -> throw new IndexOutOfBoundsException("Unknown column: " + columnIndex);
        };
    }

    public Task1Product getProductAt(int row) { return products.get(row); }

    public List<Task1Product> getProducts() { return new ArrayList<>(products); }

    public void setProducts(List<Task1Product> values) {
        products.clear();
        products.addAll(values);
        fireTableDataChanged();
    }
}
