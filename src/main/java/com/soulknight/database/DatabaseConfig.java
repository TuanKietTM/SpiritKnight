package com.soulknight.database;

public final class DatabaseConfig {

    private static final String HOST = "mysql-soul-knight-vnu-66e5.b.aivencloud.com";
    private static final int PORT = 26736;
    private static final String DATABASE = "defaultdb";

    private static final String USER = System.getenv("DB_USER");
    private static final String PASSWORD = System.getenv("DB_PASSWORD");


    public static final String URL =
            "jdbc:mysql://" + HOST + ":" + PORT + "/" + DATABASE
                    + "?sslMode=REQUIRED"
                    + "&serverTimezone=UTC"
                    + "&connectTimeout=10000"
                    + "&socketTimeout=10000";

    public static String getUser() {
        return USER;
    }

    public static String getPassword() {
        return PASSWORD;
    }
}