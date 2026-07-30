package com.soulknight.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import java.util.regex.Pattern;

public final class UserDAO {

    private static final Pattern USERNAME_PATTERN =
            Pattern.compile("^[a-zA-Z0-9_]{3,20}$");

    public RegisterResult register(
            String username,
            String password,
            String confirmPassword
    ) {
        username = normalizeUsername(username);

        if (!USERNAME_PATTERN.matcher(username).matches()) {
            return RegisterResult.failure(
                    "Ten tai khoan phai co 3-20 ky tu, "
                            + "chi gom chu, so hoac dau gach duoi."
            );
        }

        if (password == null || password.length() < 8) {
            return RegisterResult.failure(
                    "Mat khau phai co it nhat 8 ky tu."
            );
        }

        if (!password.equals(confirmPassword)) {
            return RegisterResult.failure(
                    "Mat khau nhap lai khong trung khop."
            );
        }

        if (existsByUsername(username)) {
            return RegisterResult.failure(
                    "Ten tai khoan da ton tai."
            );
        }

        String passwordHash =
                PasswordHasher.hash(password);

        String sql = """
                INSERT INTO users (
                    username,
                    password_hash
                )
                VALUES (?, ?)
                """;

        try (
                Connection connection =
                        DatabaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                sql,
                                Statement.RETURN_GENERATED_KEYS
                        )
        ) {
            statement.setString(1, username);
            statement.setString(2, passwordHash);

            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                return RegisterResult.failure(
                        "Khong the tao tai khoan."
                );
            }

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    return RegisterResult.failure(
                            "Khong lay duoc ID tai khoan."
                    );
                }

                UserAccount account = new UserAccount(
                        keys.getInt(1),
                        username,
                        passwordHash
                );

                return RegisterResult.success(account);
            }

        } catch (SQLException exception) {
            System.err.println(
                    "Loi dang ky: "
                            + exception.getMessage()
            );

            return RegisterResult.failure(
                    "Khong the dang ky tai khoan."
            );
        }
    }

    public LoginResult login(
            String username,
            String password
    ) {
        username = normalizeUsername(username);

        if (username.isBlank() || password == null
                || password.isBlank()) {
            return LoginResult.failure("Vui long nhap day du tai khoan va mat khau.");
        }

        Optional<UserAccount> accountOptional =
                findByUsername(username);

        if (accountOptional.isEmpty()) {
            return LoginResult.failure("Tai khoan hoac mat khau khong dung."
            );
        }

        UserAccount account = accountOptional.get();

        if (!PasswordHasher.verify(password, account.getPasswordHash()
        )) {
            return LoginResult.failure("Tai khoan hoac mat khau khong dung."
            );
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
                Connection connection =
                        DatabaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setString(
                    1,
                    normalizeUsername(username)
            );

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                return resultSet.next();
            }

        } catch (SQLException exception) {
            System.err.println(
                    "Loi kiem tra username: "
                            + exception.getMessage()
            );
            return false;
        }
    }

    public Optional<UserAccount> findByUsername(
            String username
    ) {
        String sql = """
                SELECT id, username, password_hash
                FROM users
                WHERE username = ?
                """;

        try (
                Connection connection =
                        DatabaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setString(
                    1,
                    normalizeUsername(username)
            );

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                if (!resultSet.next()) {
                    return Optional.empty();
                }

                UserAccount account = new UserAccount(
                        resultSet.getInt("id"),
                        resultSet.getString("username"),
                        resultSet.getString("password_hash")
                );

                return Optional.of(account);
            }

        } catch (SQLException exception) {
            System.err.println(
                    "Loi tim tai khoan: "
                            + exception.getMessage()
            );
            return Optional.empty();
        }
    }

    private String normalizeUsername(String username) {
        return username == null
                ? ""
                : username.trim();
    }

    public record LoginResult(
            boolean success,
            String message,
            UserAccount account
    ) {
        public static LoginResult success(
                UserAccount account
        ) {
            return new LoginResult(true,
                    "Dang nhap thanh cong.",
                    account
            );
        }

        public static LoginResult failure(
                String message
        ) {
            return new LoginResult(false, message, null);
        }
    }

    public record RegisterResult(boolean success, String message, UserAccount account) {
        public static RegisterResult success(
                UserAccount account
        ) {
            return new RegisterResult(true, "Dang ky thanh cong.", account);
        }

        public static RegisterResult failure(
                String message
        ) {
            return new RegisterResult(false, message, null);
        }
    }
}