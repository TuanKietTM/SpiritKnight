package com.soulknight.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseManager {

    private DatabaseManager() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                DatabaseConfig.URL,
                DatabaseConfig.getUser(),
                DatabaseConfig.getPassword()
        );
    }

    public static boolean testConnection() {
        try (Connection connection = getConnection()) {

            if (connection.isValid(5)) {
                System.out.println("Ket noi  Aiven MySQL thanh cong ");
                return true;
            }

            System.err.println("Khong hop le ");
            return false;

        } catch (SQLException exception) {
            System.err.println("That bai.");
            System.err.println("Message: " + exception.getMessage());
            System.err.println("SQL State: " + exception.getSQLState());
            System.err.println("Error Code: " + exception.getErrorCode());

            return false;
        }
    }
}