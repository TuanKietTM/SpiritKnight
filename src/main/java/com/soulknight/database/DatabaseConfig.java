package com.soulknight.database;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

public final class DatabaseConfig {

    private static final Path CONFIG_FILE = Path.of("database.properties");
    private static final Path EXAMPLE_FILE = Path.of("database.properties.example");
    private static final Properties PROPERTIES = loadProperties();

    private static final String HOST = getRequiredProperty("db.host");
    private static final int PORT = Integer.parseInt(getRequiredProperty("db.port"));
    private static final String DATABASE = getRequiredProperty("db.name");
    private static final String USER = getRequiredProperty("db.user");
    private static final String PASSWORD = getRequiredProperty("db.password");

    public static final String URL =
            "jdbc:mysql://" + HOST + ":" + PORT + "/" + DATABASE
                    + "?sslMode=REQUIRED"
                    + "&serverTimezone=UTC"
                    + "&connectTimeout=10000"
                    + "&socketTimeout=10000";

    private DatabaseConfig() {
    }

    private static Properties loadProperties() {
        createConfigIfMissing();

        Properties properties = new Properties();

        try (InputStream input = Files.newInputStream(CONFIG_FILE)) {
            properties.load(input);
            return properties;
        } catch (IOException exception) {
            throw new IllegalStateException("Khong the doc file database.properties: " + CONFIG_FILE.toAbsolutePath(), exception);
        }
    }

    private static void createConfigIfMissing() {
        if (Files.exists(CONFIG_FILE)) {
            return;
        }

        if (!Files.exists(EXAMPLE_FILE)) {
            throw new IllegalStateException("Khong tim thay database.properties va database.properties.example tai: " + Path.of("").toAbsolutePath());
        }

        try {
            Files.copy(EXAMPLE_FILE, CONFIG_FILE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Khong the tao database.properties tu database.properties.example.",
                    exception
            );
        }
    }

    private static String getRequiredProperty(String key) {
        String value = PROPERTIES.getProperty(key);

        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Thieu gia tri '" + key + "' trong database.properties."
            );
        }

        return value.trim();
    }

    public static String getUser() {
        return USER;
    }

    public static String getPassword() {
        return PASSWORD;
    }
}