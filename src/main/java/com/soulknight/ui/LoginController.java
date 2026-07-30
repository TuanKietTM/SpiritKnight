package com.soulknight.ui;

import com.soulknight.database.UserDAO;
import com.soulknight.database.UserSession;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public final class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label messageLabel;

    @FXML
    private Button loginButton;

    private final UserDAO userDAO = new UserDAO();

    private Runnable onRegisterRequested;
    private Runnable onLoginSuccess;

    public void setOnRegisterRequested(Runnable callback) {
        this.onRegisterRequested = callback;
    }

    public void setOnLoginSuccess(Runnable callback) {
        this.onLoginSuccess = callback;
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        setLoading(true);
        messageLabel.setText("Logging in...");

        Task<UserDAO.LoginResult> task = new Task<>() {
            @Override
            protected UserDAO.LoginResult call() {
                return userDAO.login(username, password);
            }
        };

        task.setOnSucceeded(event -> {
            setLoading(false);

            UserDAO.LoginResult result = task.getValue();

            if (!result.success()) {
                messageLabel.setText(result.message());
                passwordField.clear();
                return;
            }

            UserSession.login(result.account());
            messageLabel.setText("");

            if (onLoginSuccess != null) {
                onLoginSuccess.run();
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
                "login-database-thread"
        );
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void openRegister() {
        messageLabel.setText("");
        passwordField.clear();

        if (onRegisterRequested != null) {
            onRegisterRequested.run();
        }
    }

    public void clearForm() {
        usernameField.clear();
        passwordField.clear();
        messageLabel.setText("");
        setLoading(false);
    }

    public void setUsername(String username) {
        usernameField.setText(
                username == null ? "" : username
        );
        passwordField.clear();
    }

    private void setLoading(boolean loading) {
        usernameField.setDisable(loading);
        passwordField.setDisable(loading);
        loginButton.setDisable(loading);
    }
}