package econest;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Tasks56EcoNestStore implements Serializable {
    private static final long serialVersionUID = 1L;
    private final List<Tasks56Client> clients = new ArrayList<Tasks56Client>();
    private final List<Tasks56Product> products = new ArrayList<Tasks56Product>();
    private final List<Tasks56DeliveryAgent> agents = new ArrayList<Tasks56DeliveryAgent>();
    private final List<Tasks56ClientOrder> orders = new ArrayList<Tasks56ClientOrder>();
    private transient Path dataFile;

    public static Tasks56EcoNestStore load() {
        Path file = Paths.get(System.getProperty("user.home"), ".econest", "data.ser");
        if (Files.exists(file)) {
            try (ObjectInputStream input = new ObjectInputStream(Files.newInputStream(file))) {
                Tasks56EcoNestStore store = (Tasks56EcoNestStore) input.readObject();
                store.dataFile = file;
                return store;
            } catch (IOException | ClassNotFoundException | ClassCastException ex) {
                System.err.println("Could not load saved EcoNest data: " + ex.getMessage());
            }
        }
        Tasks56EcoNestStore store = new Tasks56EcoNestStore();
        store.dataFile = file;
        store.addSampleData();
        store.save();
        return store;
    }
    private void addSampleData() {
        clients.add(new Tasks56Client("CL-001", "Green Leaf Home", "greenleaf@example.com", "077 123 4567"));
        clients.add(new Tasks56Client("CL-002", "Bright Office", "orders@brightoffice.example", "011 234 5678"));
        clients.add(new Tasks56Client("CL-003", "Nimali Perera", "nimali@example.com", "071 345 6789"));
        products.add(new Tasks56Product("PR-001", "Peace Lily", 1850.00, 24));
        products.add(new Tasks56Product("PR-002", "Terracotta Planter", 950.00, 40));
        products.add(new Tasks56Product("PR-003", "Balcony Herb Kit", 3200.00, 12));
        products.add(new Tasks56Product("PR-004", "Snake Plant", 2400.00, 18));
        agents.add(new Tasks56DeliveryAgent("DA-001", "Kasun Silva", "071 456 1234", "Van WP-CA 1234"));
        agents.add(new Tasks56DeliveryAgent("DA-002", "Ishara Fernando", "077 567 2345", "Three-wheeler WP-QA 5678"));
    }
    public void save() {
        try {
            Files.createDirectories(dataFile.getParent());
            try (ObjectOutputStream output = new ObjectOutputStream(Files.newOutputStream(dataFile))) { output.writeObject(this); }
        } catch (IOException ex) {
            throw new IllegalStateException("Could not save data to " + dataFile + ": " + ex.getMessage(), ex);
        }
    }
    public List<Tasks56Client> getClients() { return clients; }
    public List<Tasks56Product> getProducts() { return products; }
    public List<Tasks56DeliveryAgent> getAgents() { return agents; }
    public List<Tasks56ClientOrder> getOrders() { return orders; }
}
