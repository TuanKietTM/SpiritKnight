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
                        true,0,0
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
                SELECT id,username,password_hash,first_play,gold_bank, gem_bank
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
                        resultSet.getBoolean("first_play"),
                        resultSet.getInt("gold_bank"),
                        resultSet.getInt("gem_bank")
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



    public ChangePasswordResult changePassword(int userId, String currentPassword, String newPassword, String confirmPassword) {
        if (userId <= 0) return ChangePasswordResult.failure("Tai khoan khong hop le.");
        if (currentPassword == null || currentPassword.isBlank()) {
            return ChangePasswordResult.failure("Vui long nhap mat khau hien tai.");
        }
        if (newPassword == null || newPassword.length() < 8) {
            return ChangePasswordResult.failure("Mat khau moi phai co it nhat 8 ky tu.");
        }
        if (!newPassword.equals(confirmPassword)) {
            return ChangePasswordResult.failure("Mat khau xac nhan khong khop.");
        }
        if (currentPassword.equals(newPassword)) {
            return ChangePasswordResult.failure("Mat khau moi phai khac mat khau hien tai.");
        }

        String findSql = """
                SELECT password_hash
                FROM users
                WHERE id = ?
                """;

        String updateSql = """
                UPDATE users
                SET password_hash = ?
                WHERE id = ?
                """;

        try (Connection connection = DatabaseManager.getConnection()) {
            String currentPasswordHash;

            try (PreparedStatement statement = connection.prepareStatement(findSql)) {
                statement.setInt(1, userId);

                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        return ChangePasswordResult.failure("Khong tim thay tai khoan.");
                    }

                    currentPasswordHash = resultSet.getString("password_hash");
                }
            }

            if (!PasswordHasher.verify(currentPassword, currentPasswordHash)) {
                return ChangePasswordResult.failure("Mat khau hien tai khong dung.");
            }

            String newPasswordHash = PasswordHasher.hash(newPassword);

            try (PreparedStatement statement = connection.prepareStatement(updateSql)) {
                statement.setString(1, newPasswordHash);
                statement.setInt(2, userId);

                if (statement.executeUpdate() == 0) {
                    return ChangePasswordResult.failure("Khong the cap nhat mat khau.");
                }
            }

            return ChangePasswordResult.ok();

        } catch (SQLException exception) {
            System.err.println("Loi doi mat khau: " + exception.getMessage());
            return ChangePasswordResult.failure("Khong the ket noi den database.");
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

    public record ChangePasswordResult(boolean success, String message) {

        public static ChangePasswordResult ok() {
            return new ChangePasswordResult(true, "Doi mat khau thanh cong.");
        }

        public static ChangePasswordResult failure(String message) {
            return new ChangePasswordResult(false, message);
        }
    }

}