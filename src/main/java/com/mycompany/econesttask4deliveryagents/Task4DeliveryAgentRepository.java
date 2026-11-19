package com.mycompany.econesttask4deliveryagents;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/** Loads and saves delivery agents in the user's EcoNest data folder. */
public class Task4DeliveryAgentRepository {

    private final Path dataFile = Path.of(
            System.getProperty("user.home"), ".econest", "delivery-agents.dat");

    @SuppressWarnings("unchecked")
    public List<Task4DeliveryAgent> loadAll() throws IOException, ClassNotFoundException {
        if (Files.notExists(dataFile)) {
            return new ArrayList<>();
        }

        try (ObjectInputStream input = new ObjectInputStream(Files.newInputStream(dataFile))) {
            Object stored = input.readObject();
            if (!(stored instanceof List<?>)) {
                throw new IOException("Delivery-agent data file has an unexpected format.");
            }
            for (Object value : (List<?>) stored) {
                if (!(value instanceof Task4DeliveryAgent)) {
                    throw new IOException("Delivery-agent data contains an invalid record.");
                }
            }
            return (List<Task4DeliveryAgent>) stored;
        }
    }

    public void saveAll(List<Task4DeliveryAgent> agents) throws IOException {
        Files.createDirectories(dataFile.getParent());
        Path temporaryFile = dataFile.resolveSibling("delivery-agents.tmp");
        try (ObjectOutputStream output = new ObjectOutputStream(
                Files.newOutputStream(temporaryFile))) {
            output.writeObject(new ArrayList<>(agents));
        }

        try {
            Files.move(temporaryFile, dataFile,
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException atomicMoveUnavailable) {
            Files.move(temporaryFile, dataFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
