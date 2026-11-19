package lk.econest.db;

import java.io.IOException;
import java.sql.SQLException;

/** Gives database failures a consistent user-facing message while preserving the cause. */
public final class DatabaseException extends IOException {
    public DatabaseException(String operation, SQLException cause) {
        super("Database operation failed while " + operation + ": " + cause.getMessage(), cause);
    }
}
