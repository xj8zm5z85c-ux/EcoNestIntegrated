package com.mycompany.econesttask4deliveryagents;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import lk.econest.db.Database;
import lk.econest.db.DatabaseException;

/** Loads and saves delivery agents in the shared EcoNest database. */
public class Task4DeliveryAgentRepository {
    public List<Task4DeliveryAgent> loadAll() throws IOException {
        String sql = "SELECT agent_code, full_name, phone, email, vehicle_registration_no, "
                + "vehicle_type, vehicle_model FROM delivery_agents WHERE is_active = TRUE ORDER BY full_name";
        try (Connection connection = Database.openConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet rows = statement.executeQuery()) {
            List<Task4DeliveryAgent> agents = new ArrayList<>();
            while (rows.next()) {
                agents.add(new Task4DeliveryAgent(rows.getString("agent_code"),
                        rows.getString("full_name"), rows.getString("phone"), rows.getString("email"),
                        rows.getString("vehicle_registration_no"), rows.getString("vehicle_type"),
                        rows.getString("vehicle_model")));
            }
            return agents;
        } catch (SQLException exception) {
            throw new DatabaseException("loading delivery agents", exception);
        }
    }

    public void insert(Task4DeliveryAgent agent) throws IOException {
        String sql = "INSERT INTO delivery_agents "
                + "(agent_code, full_name, phone, email, vehicle_registration_no, vehicle_type, vehicle_model, is_active) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, TRUE)";
        executeAgent(sql, agent, "adding delivery agent");
    }

    public void update(Task4DeliveryAgent agent) throws IOException {
        String sql = "UPDATE delivery_agents SET full_name = ?, phone = ?, email = ?, "
                + "vehicle_registration_no = ?, vehicle_type = ?, vehicle_model = ? "
                + "WHERE agent_code = ? AND is_active = TRUE";
        try (Connection connection = Database.openConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, agent.getName());
            statement.setString(2, agent.getPhone());
            statement.setString(3, agent.getEmail());
            statement.setString(4, agent.getVehicleRegistrationNo());
            statement.setString(5, agent.getVehicleType());
            statement.setString(6, agent.getVehicleModel());
            statement.setString(7, agent.getAgentId());
            if (statement.executeUpdate() != 1) {
                throw new IOException("Active delivery agent was not found: " + agent.getAgentId());
            }
        } catch (SQLException exception) {
            throw new DatabaseException("updating delivery agent", exception);
        }
    }

    public void deactivate(String agentCode) throws IOException {
        try (Connection connection = Database.openConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "UPDATE delivery_agents SET is_active = FALSE WHERE agent_code = ? AND is_active = TRUE")) {
            statement.setString(1, agentCode);
            if (statement.executeUpdate() != 1) {
                throw new IOException("Active delivery agent was not found: " + agentCode);
            }
        } catch (SQLException exception) {
            throw new DatabaseException("removing delivery agent", exception);
        }
    }

    private static void executeAgent(String sql, Task4DeliveryAgent agent, String operation)
            throws IOException {
        try (Connection connection = Database.openConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, agent.getAgentId());
            statement.setString(2, agent.getName());
            statement.setString(3, agent.getPhone());
            statement.setString(4, agent.getEmail());
            statement.setString(5, agent.getVehicleRegistrationNo());
            statement.setString(6, agent.getVehicleType());
            statement.setString(7, agent.getVehicleModel());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new DatabaseException(operation, exception);
        }
    }
}
