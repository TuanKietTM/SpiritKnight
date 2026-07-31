package com.soulknight.ui;

import com.soulknight.database.PlayerSave;
import com.soulknight.database.PlayerSaveDAO;
import com.soulknight.database.UserDAO;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public final class RegisterController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Label messageLabel;

    @FXML
    private Button registerButton;

    private final UserDAO userDAO = new UserDAO();
    private final PlayerSaveDAO playerSaveDAO =
            new PlayerSaveDAO();

    private Runnable onLoginRequested;

    public void setOnLoginRequested(Runnable callback) {
        this.onLoginRequested = callback;
    }

    @FXML
    private void handleRegister() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        String confirmPassword =
                confirmPasswordField.getText();

        setLoading(true);
        messageLabel.setText("Creating account...");

        Task<UserDAO.RegisterResult> task = new Task<>() {
            @Override
            protected UserDAO.RegisterResult call() {
                UserDAO.RegisterResult result = userDAO.register(username, password, confirmPassword);

                if (!result.success()) {
                    return result;
                }

                /*
                 * Tạo save mặc định ngay sau khi đăng ký.
                 * username cũng chính là player_name.
                 */
                PlayerSave defaultSave = new PlayerSave(
                        result.account().getUsername(), 1, 0, 0, 100, 100, 1, 0);

                boolean saveCreated =
                        playerSaveDAO.save(defaultSave);

                if (!saveCreated) {
                    System.err.println("Tai khoan da tao nhung " + "chua tao duoc save mac dinh.");
                }

                return result;
            }
        };

        task.setOnSucceeded(event -> {
            setLoading(false);

            UserDAO.RegisterResult result =
                    task.getValue();

            if (!result.success()) {
                messageLabel.setText(result.message());
                return;
            }

            messageLabel.setText(
                    "Registration successful."
            );

            passwordField.clear();
            confirmPasswordField.clear();

            if (onLoginRequested != null) {
                onLoginRequested.run();
            }
        });

        task.setOnFailed(event -> {
            setLoading(false);
            messageLabel.setText(
                    "Unable to connect to the database."
            );

            if (task.getException() != null) {
                task.getException().printStackTrace();
            }
        });

        Thread thread = new Thread(
                task,
                "register-database-thread"
        );
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void openLogin() {
        clearForm();

        if (onLoginRequested != null) {
            onLoginRequested.run();
        }
    }
    @FXML
    public void initialize() {

        // Enter o username -> sang password
        usernameField.setOnAction(e -> passwordField.requestFocus());

        // Enter o password -> sang confirm password
        passwordField.setOnAction(
                e -> confirmPasswordField.requestFocus()
        );

        // Enter o confirm -> dang ky
        confirmPasswordField.setOnAction(e -> {
            if (!registerButton.isDisabled()) {
                handleRegister();
            }
        });
    }

    public String getEnteredUsername() {
        return usernameField.getText() == null
                ? ""
                : usernameField.getText().trim();
    }

    public void clearForm() {
        usernameField.clear();
        passwordField.clear();
        confirmPasswordField.clear();
        messageLabel.setText("");
        setLoading(false);
    }

    private void setLoading(boolean loading) {
        usernameField.setDisable(loading);
        passwordField.setDisable(loading);
        confirmPasswordField.setDisable(loading);
        registerButton.setDisable(loading);
    }
}