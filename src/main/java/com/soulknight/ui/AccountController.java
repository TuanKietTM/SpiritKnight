package com.soulknight.ui;

import com.soulknight.database.UserDAO;
import com.soulknight.database.UserSession;
import com.soulknight.utils.DatabaseExecutor;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;

public final class AccountController {

    @FXML
    private Label usernameLabel;

    @FXML
    private PasswordField currentPasswordField;

    @FXML
    private PasswordField newPasswordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Label messageLabel;

    @FXML
    private Button changePasswordButton;

    private final UserDAO userDAO = new UserDAO();

    private Runnable onBackRequested;
    private Runnable onLogoutRequested;
    private java.util.function.Consumer<String> onShowLoading;
    private Runnable onHideLoading;

    @FXML
    public void initialize() {
        currentPasswordField.setOnAction(event -> newPasswordField.requestFocus());
        newPasswordField.setOnAction(event -> confirmPasswordField.requestFocus());

        confirmPasswordField.setOnAction(event -> {
            if (!changePasswordButton.isDisabled()) {
                handleChangePassword();
            }
        });
    }


    public void setup(Runnable onBackRequested, Runnable onLogoutRequested,
                      java.util.function.Consumer<String> onShowLoading, Runnable onHideLoading) {

        this.onBackRequested = onBackRequested;
        this.onLogoutRequested = onLogoutRequested;
        this.onShowLoading = onShowLoading;
        this.onHideLoading = onHideLoading;
        refreshAccountData();
    }

    public void setOnBackRequested(Runnable callback) {
        this.onBackRequested = callback;
    }

    public void setOnLogoutRequested(Runnable callback) {
        this.onLogoutRequested = callback;
    }

    public void setLoadingCallbacks(java.util.function.Consumer<String> onShowLoading, Runnable onHideLoading) {
        this.onShowLoading = onShowLoading;
        this.onHideLoading = onHideLoading;
    }

    public void refreshAccountData() {
        usernameLabel.setText(UserSession.getCurrentUsername());
        clearPasswordFields();
        messageLabel.setText("");
        setLoading(false);
    }

    @FXML
    private void handleChangePassword() {
        int userId = UserSession.getCurrentUserId();

        if (userId <= 0) {
            messageLabel.setText("Phien dang nhap khong hop le.");
            return;
        }

        String currentPassword = currentPasswordField.getText();
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        setLoading(true);
        messageLabel.setText("");

        if (onShowLoading != null) {
            onShowLoading.accept("Changing password...");
        }

        Task<UserDAO.ChangePasswordResult> task = new Task<>() {
            @Override
            protected UserDAO.ChangePasswordResult call() {
                return userDAO.changePassword(userId, currentPassword, newPassword, confirmPassword);
            }
        };

        task.setOnSucceeded(event -> {
            setLoading(false);
            hideLoadingOverlay();

            UserDAO.ChangePasswordResult result = task.getValue();
            messageLabel.setText(result.message());

            if (!result.success()) {
                currentPasswordField.clear();
                currentPasswordField.requestFocus();
                return;
            }

            clearPasswordFields();
        });

        task.setOnFailed(event -> {
            setLoading(false);
            hideLoadingOverlay();
            messageLabel.setText("Khong the ket noi den database.");

            if (task.getException() != null) {
                task.getException().printStackTrace();
            }
        });

        DatabaseExecutor.getExecutor().execute(task);
    }

    @FXML
    private void handleBack() {
        clearPasswordFields();
        messageLabel.setText("");

        if (onBackRequested != null) {
            onBackRequested.run();
        }
    }

    @FXML
    private void handleLogout() {
        clearPasswordFields();
        messageLabel.setText("");

        if (onLogoutRequested != null) {
            onLogoutRequested.run();
        }
    }

    private void clearPasswordFields() {
        currentPasswordField.clear();
        newPasswordField.clear();
        confirmPasswordField.clear();
    }

    private void setLoading(boolean loading) {
        currentPasswordField.setDisable(loading);
        newPasswordField.setDisable(loading);
        confirmPasswordField.setDisable(loading);
        changePasswordButton.setDisable(loading);
    }

    private void hideLoadingOverlay() {
        if (onHideLoading != null) {
            onHideLoading.run();
        }
    }
}