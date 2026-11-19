package com.mycompany.econesttask4deliveryagents;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Business rules for adding, editing, and removing delivery agents. */
public class Task4DeliveryAgentService {

    private final Task4DeliveryAgentRepository repository;
    private final List<Task4DeliveryAgent> agents;

    public Task4DeliveryAgentService(Task4DeliveryAgentRepository repository)
            throws IOException, ClassNotFoundException {
        if (repository == null) {
            throw new IllegalArgumentException("Delivery-agent repository is required.");
        }
        this.repository = repository;
        this.agents = new ArrayList<>(repository.loadAll());
    }

    public List<Task4DeliveryAgent> getAllAgents() {
        List<Task4DeliveryAgent> result = new ArrayList<>(agents);
        result.sort(Comparator.comparing(Task4DeliveryAgent::getName,
                String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    public void addAgent(String agentId, String name, String phone, String email,
            String vehicleRegistrationNo, String vehicleType, String vehicleModel)
            throws IOException {
        String id = requireText(agentId, "Agent ID");
        if (findAgent(id) != null) {
            throw new IllegalArgumentException("That agent ID already exists.");
        }
        Task4DeliveryAgent agent = createValidatedAgent(id, name, phone, email,
                vehicleRegistrationNo, vehicleType, vehicleModel);
        repository.insert(agent);
        reload();
    }

    public void updateAgent(String agentId, String name, String phone, String email,
            String vehicleRegistrationNo, String vehicleType, String vehicleModel)
            throws IOException {
        Task4DeliveryAgent agent = requireAgent(agentId);
        Task4DeliveryAgent validated = createValidatedAgent(agent.getAgentId(), name, phone,
                email, vehicleRegistrationNo, vehicleType, vehicleModel);
        repository.update(validated);
        reload();
    }

    public void removeAgent(String agentId) throws IOException {
        repository.deactivate(requireAgent(agentId).getAgentId());
        reload();
    }

    private Task4DeliveryAgent createValidatedAgent(String agentId, String name, String phone,
            String email, String registrationNo, String vehicleType, String vehicleModel) {
        String cleanName = requireText(name, "Agent name");
        String cleanPhone = requireText(phone, "Phone number");
        String cleanEmail = requireText(email, "Email address");
        String cleanRegistrationNo = requireText(registrationNo, "Vehicle registration number");
        String cleanVehicleType = requireText(vehicleType, "Vehicle type");
        String cleanVehicleModel = requireText(vehicleModel, "Vehicle model");

        if (!cleanEmail.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("Enter a valid email address.");
        }

        return new Task4DeliveryAgent(agentId, cleanName, cleanPhone, cleanEmail,
                cleanRegistrationNo, cleanVehicleType, cleanVehicleModel);
    }

    private Task4DeliveryAgent requireAgent(String agentId) {
        Task4DeliveryAgent agent = findAgent(requireText(agentId, "Agent ID"));
        if (agent == null) {
            throw new IllegalArgumentException("Delivery agent was not found.");
        }
        return agent;
    }

    private Task4DeliveryAgent findAgent(String agentId) {
        for (Task4DeliveryAgent agent : agents) {
            if (agent.getAgentId().equalsIgnoreCase(agentId)) {
                return agent;
            }
        }
        return null;
    }

    private void reload() throws IOException {
        agents.clear();
        agents.addAll(repository.loadAll());
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        return value.trim();
    }
}
