package lk.econest.db;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import model.Task2Client;

/** Database access for clients shared by management and order processing. */
public final class ClientRepository {
    public List<Task2Client> loadAll() throws IOException {
        String sql = "SELECT client_code, client_name, client_type, phone, email, address_line "
                + "FROM clients ORDER BY client_name";
        try (Connection connection = Database.openConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet rows = statement.executeQuery()) {
            List<Task2Client> clients = new ArrayList<>();
            while (rows.next()) {
                clients.add(new Task2Client(rows.getString("client_code"),
                        rows.getString("client_name"), titleCase(rows.getString("client_type")),
                        rows.getString("phone"), rows.getString("email"), rows.getString("address_line")));
            }
            return clients;
        } catch (SQLException exception) {
            throw new DatabaseException("loading clients", exception);
        }
    }

    public void insert(Task2Client client) throws IOException {
        String sql = "INSERT INTO clients "
                + "(client_code, client_type, client_name, email, phone, address_line, city) "
                + "VALUES (?, ?, ?, ?, ?, ?, '')";
        execute(sql, client, "adding client");
    }

    public void update(Task2Client client) throws IOException {
        String sql = "UPDATE clients SET client_type = ?, client_name = ?, email = ?, phone = ?, "
                + "address_line = ? WHERE client_code = ?";
        try (Connection connection = Database.openConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, client.getClientType().toUpperCase(java.util.Locale.ROOT));
            statement.setString(2, client.getName());
            statement.setString(3, client.getEmail());
            statement.setString(4, client.getPhone());
            statement.setString(5, client.getAddress());
            statement.setString(6, client.getClientId());
            if (statement.executeUpdate() != 1) {
                throw new IOException("Client was not found: " + client.getClientId());
            }
        } catch (SQLException exception) {
            throw new DatabaseException("updating client", exception);
        }
    }

    public void delete(String clientCode) throws IOException {
        String referenceCheck = "SELECT COUNT(*) FROM orders o "
                + "JOIN clients c ON c.client_id = o.client_id "
                + "WHERE c.client_code = ?";
        String sql = "DELETE FROM clients WHERE client_code = ?";
        try (Connection connection = Database.openConnection();
                PreparedStatement checkStatement = connection.prepareStatement(referenceCheck);
                PreparedStatement deleteStatement = connection.prepareStatement(sql)) {
            checkStatement.setString(1, clientCode);
            try (ResultSet rows = checkStatement.executeQuery()) {
                if (rows.next() && rows.getInt(1) > 0) {
                    throw new IOException("This client has existing orders and cannot be deleted.");
                }
            }

            deleteStatement.setString(1, clientCode);
            if (deleteStatement.executeUpdate() != 1) {
                throw new IOException("Client was not found: " + clientCode);
            }
        } catch (SQLException exception) {
            throw new DatabaseException("deleting client", exception);
        }
    }

    private static void execute(String sql, Task2Client client, String operation) throws IOException {
        try (Connection connection = Database.openConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, client.getClientId());
            statement.setString(2, client.getClientType().toUpperCase(java.util.Locale.ROOT));
            statement.setString(3, client.getName());
            statement.setString(4, client.getEmail());
            statement.setString(5, client.getPhone());
            statement.setString(6, client.getAddress());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new DatabaseException(operation, exception);
        }
    }

    private static String titleCase(String value) {
        return value.substring(0, 1) + value.substring(1).toLowerCase(java.util.Locale.ROOT);
    }
}
