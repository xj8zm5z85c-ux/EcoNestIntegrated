package com.mycompany.econesttask3inventory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/** Stores inventory records in the user's EcoNest data folder. */
public class Task3InventoryRepository {

    private final Path dataFile = Path.of(
            System.getProperty("user.home"), ".econest", "inventory.dat");

    @SuppressWarnings("unchecked")
    public List<Task3InventoryItem> loadAll() throws IOException, ClassNotFoundException {
        if (Files.notExists(dataFile)) {
            return new ArrayList<>();
        }

        try (ObjectInputStream input = new ObjectInputStream(Files.newInputStream(dataFile))) {
            Object stored = input.readObject();
            if (!(stored instanceof List<?>)) {
                throw new IOException("Inventory data file has an unexpected format.");
            }
            for (Object value : (List<?>) stored) {
                if (!(value instanceof Task3InventoryItem)) {
                    throw new IOException("Inventory data file contains an invalid record.");
                }
            }
            return (List<Task3InventoryItem>) stored;
        }
    }

    public void saveAll(List<Task3InventoryItem> inventory) throws IOException {
        Files.createDirectories(dataFile.getParent());
        Path temporaryFile = dataFile.resolveSibling("inventory.tmp");

        try (ObjectOutputStream output = new ObjectOutputStream(
                Files.newOutputStream(temporaryFile))) {
            output.writeObject(new ArrayList<>(inventory));
        }

        try {
            Files.move(temporaryFile, dataFile,
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException atomicMoveUnavailable) {
            Files.move(temporaryFile, dataFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public Path getDataFile() {
        return dataFile;
    }
}
