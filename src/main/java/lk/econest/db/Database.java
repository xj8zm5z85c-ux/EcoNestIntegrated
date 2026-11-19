package lk.econest.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/** Provides connections to the shared EcoNest MariaDB database. */
public final class Database {
    private static final String DEFAULT_URL = "jdbc:mariadb://localhost:3306/econest_db";

    private Database() {
    }

    public static Connection openConnection() throws SQLException {
        String url = setting("ECONEST_DB_URL", "econest.db.url", DEFAULT_URL);
        String user = setting("ECONEST_DB_USER", "econest.db.user", "root");
        String password = setting("ECONEST_DB_PASSWORD", "econest.db.password", "");
        return DriverManager.getConnection(url, user, password);
    }

    public static void verifyConnection() throws SQLException {
        try (Connection connection = openConnection()) {
            if (!connection.isValid(5)) {
                throw new SQLException("The EcoNest database connection is not valid.");
            }
            String schemaCheck = "SELECT p.product_code, i.quantity_on_hand, c.client_code, "
                    + "a.agent_code, o.order_code, d.scheduled_for, n.send_status "
                    + "FROM products p LEFT JOIN inventory i ON i.product_id = p.product_id "
                    + "LEFT JOIN clients c ON 1 = 0 LEFT JOIN delivery_agents a ON 1 = 0 "
                    + "LEFT JOIN orders o ON 1 = 0 LEFT JOIN deliveries d ON 1 = 0 "
                    + "LEFT JOIN email_notifications n ON 1 = 0 WHERE 1 = 0";
            try (Statement statement = connection.createStatement()) {
                statement.executeQuery(schemaCheck).close();
            } catch (SQLException exception) {
                throw new SQLException("The EcoNest database schema is missing or outdated. "
                        + "Install EcoNestMainDatabase.sql and, for an older database, run "
                        + "db/migrations/V2__component_integration.sql.", exception);
            }
        }
    }

    private static String setting(String environmentName, String propertyName, String fallback) {
        String property = System.getProperty(propertyName);
        if (property != null && !property.isBlank()) {
            return property.trim();
        }
        String environment = System.getenv(environmentName);
        return environment == null || environment.isBlank() ? fallback : environment.trim();
    }
}
