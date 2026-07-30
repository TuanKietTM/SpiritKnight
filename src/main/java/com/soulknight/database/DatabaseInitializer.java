package com.soulknight.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseInitializer {

    private DatabaseInitializer() {
    }

    public static boolean initialize() {
        String createUsersTable = """
                CREATE TABLE IF NOT EXISTS users (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    username VARCHAR(50) NOT NULL UNIQUE,
                    password_hash VARCHAR(255) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                        DEFAULT CURRENT_TIMESTAMP
                )
                """;

        String createPlayerSavesTable = """
                CREATE TABLE IF NOT EXISTS player_saves (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    player_name VARCHAR(50) NOT NULL UNIQUE,
                    level INT NOT NULL DEFAULT 1,
                    gold INT NOT NULL DEFAULT 0,
                    gems INT NOT NULL DEFAULT 0,
                    hp DOUBLE NOT NULL DEFAULT 100,
                    energy DOUBLE NOT NULL DEFAULT 100,
                    current_room INT NOT NULL DEFAULT 1,
                    score INT NOT NULL DEFAULT 0,
                    updated_at TIMESTAMP NOT NULL
                        DEFAULT CURRENT_TIMESTAMP
                        ON UPDATE CURRENT_TIMESTAMP
                )
                """;

        try (
                Connection connection = DatabaseManager.getConnection();
                Statement statement = connection.createStatement()
        ) {
            statement.executeUpdate(createUsersTable);
            statement.executeUpdate(createPlayerSavesTable);

            System.out.println("Bang users da san sang.");
            System.out.println("Bang player_saves da san sang.");

            return true;

        } catch (SQLException exception) {
            System.err.println(
                    "Khong the khoi tao database: "
                            + exception.getMessage()
            );
            return false;
        }
    }
}