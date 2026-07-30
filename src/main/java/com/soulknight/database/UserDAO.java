package com.soulknight.database;

import java.sql.*;
import java.util.Optional;
import java.util.regex.Pattern;

public final class UserDAO {

    private static final Pattern USERNAME_PATTERN =
            Pattern.compile("^[a-zA-Z0-9_]{3,20}$");

    public RegisterResult register(String username, String password, String confirmPassword) {
        username = normalizeUsername(username);

        if (!USERNAME_PATTERN.matcher(username).matches()) {
            return RegisterResult.failure("The account name must be 3–20 characters long and consist only of letters, numbers, or underscores.");
        }

        if (password == null || password.length() < 8) {
            return RegisterResult.failure("The password must be at least 8 characters long.");
        }

        if (!password.equals(confirmPassword)) {
            return RegisterResult.failure("The re-entered password does not match.");
        }

        if (existsByUsername(username)) {
            return RegisterResult.failure("The account name already exists.");
        }

        String passwordHash = PasswordHasher.hash(password);

        String sql = """
                INSERT INTO users(username,password_hash,first_play)
                VALUES(?,?,?)
                """;

        try (
                Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        ) {

            statement.setString(1, username);
            statement.setString(2, passwordHash);
            statement.setBoolean(3, true);

            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                return RegisterResult.failure("Unable to create account.");
            }

            try (ResultSet keys = statement.getGeneratedKeys()) {

                if (!keys.next()) {
                    return RegisterResult.failure("Cannot get account ID.");
                }

                UserAccount account = new UserAccount(
                        keys.getInt(1),
                        username,
                        passwordHash,
                        true
                );

                return RegisterResult.success(account);
            }

        } catch (SQLException exception) {
            System.err.println("Register error: " + exception.getMessage());
            return RegisterResult.failure("Unable to register account.");
        }
    }

    public LoginResult login(String username, String password) {
        username = normalizeUsername(username);

        if (username.isBlank() || password == null || password.isBlank()) {
            return LoginResult.failure("Please enter username and password.");
        }

        Optional<UserAccount> accountOptional = findByUsername(username);

        if (accountOptional.isEmpty()) {
            return LoginResult.failure("Incorrect username or password.");
        }

        UserAccount account = accountOptional.get();

        if (!PasswordHasher.verify(password, account.getPasswordHash())) {
            return LoginResult.failure("Incorrect username or password.");
        }

        return LoginResult.success(account);
    }

    public boolean existsByUsername(String username) {

        String sql = """
                SELECT 1
                FROM users
                WHERE username = ?
                LIMIT 1
                """;

        try (
                Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setString(1, normalizeUsername(username));

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }

        } catch (SQLException exception) {
            System.err.println("Username check error: " + exception.getMessage());
            return false;
        }
    }

    public Optional<UserAccount> findByUsername(String username) {

        String sql = """
                SELECT id,username,password_hash,first_play
                FROM users
                WHERE username=?
                """;

        try (
                Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setString(1, normalizeUsername(username));

            try (ResultSet resultSet = statement.executeQuery()) {

                if (!resultSet.next()) {
                    return Optional.empty();
                }

                UserAccount account = new UserAccount(
                        resultSet.getInt("id"),
                        resultSet.getString("username"),
                        resultSet.getString("password_hash"),
                        resultSet.getBoolean("first_play")
                );

                return Optional.of(account);
            }

        } catch (SQLException exception) {
            System.err.println("Find account error: " + exception.getMessage());
            return Optional.empty();
        }
    }

    public boolean setFirstPlay(int userId, boolean firstPlay) {

        String sql = """
                UPDATE users
                SET first_play = ?
                WHERE id = ?
                """;

        try (
                Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setBoolean(1, firstPlay);
            statement.setInt(2, userId);

            return statement.executeUpdate() > 0;

        } catch (SQLException exception) {
            System.err.println("Update first_play error: " + exception.getMessage());
            return false;
        }
    }

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim();
    }

    public record LoginResult(boolean success, String message, UserAccount account) {

        public static LoginResult success(UserAccount account) {
            return new LoginResult(true, "Login successful.", account);
        }

        public static LoginResult failure(String message) {
            return new LoginResult(false, message, null);
        }
    }

    public record RegisterResult(boolean success, String message, UserAccount account) {

        public static RegisterResult success(UserAccount account) {
            return new RegisterResult(true, "Register successful.", account);
        }

        public static RegisterResult failure(String message) {
            return new RegisterResult(false, message, null);
        }
    }
}