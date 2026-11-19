package lk.econest.catalog;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/** Stores catalogue records in a CSV file under the user's home directory. */
public final class Task1CsvProductRepository {
    private static final String HEADER = "sku,name,type,price,ecoRating";
    private final Path file;

    public Task1CsvProductRepository(Path file) {
        this.file = file;
    }

    public List<Task1Product> load() throws IOException {
        if (!Files.exists(file)) {
            return new ArrayList<>();
        }
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        List<Task1Product> products = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            if (lines.get(i).isBlank()) {
                continue;
            }
            try {
                List<String> fields = parseCsvLine(lines.get(i));
                if (fields.size() != 5) {
                    throw new IllegalArgumentException("Expected 5 columns.");
                }
                Task1ProductType type = Task1ProductType.valueOf(fields.get(2));
                products.add(new Task1Product(fields.get(0), fields.get(1), type,
                        new BigDecimal(fields.get(3)), Integer.parseInt(fields.get(4))));
            } catch (RuntimeException ex) {
                throw new IOException("Could not read catalogue row " + (i + 1) + ". "
                        + "Check " + file + ".", ex);
            }
        }
        return products;
    }

    public void save(List<Task1Product> products) throws IOException {
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        for (Task1Product product : products) {
            lines.add(csv(product.getSku()) + "," + csv(product.getName()) + ","
                    + product.getType().name() + "," + product.getPrice().toPlainString()
                    + "," + product.getEcoRating());
        }
        Files.write(temporary, lines, StandardCharsets.UTF_8);
        try {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String csv(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private static List<String> parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    field.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (c == ',' && !quoted) {
                fields.add(field.toString());
                field.setLength(0);
            } else {
                field.append(c);
            }
        }
        fields.add(field.toString());
        return fields;
    }
}
