package com.soulknight.ui;

import com.soulknight.database.UserDAO;
import com.soulknight.database.UserSession;
import com.soulknight.utils.DatabaseExecutor;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.beans.property.DoubleProperty;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.util.function.Consumer;

public final class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;
    @FXML private Button loginButton;
    @FXML private StackPane loginRoot;
    @FXML private Canvas backgroundCanvas;
    @FXML private VBox loginBox;

    private DeveloperRoomBackground roomBackground;

    private final UserDAO userDAO = new UserDAO();

    private Runnable onRegisterRequested;
    private Runnable onLoginSuccess;
    private Consumer<String> onShowLoading;
    private Runnable onHideLoading;

    @FXML
    public void initialize() {
        roomBackground = new DeveloperRoomBackground(backgroundCanvas);
        backgroundCanvas.widthProperty().bind(loginRoot.widthProperty());
        backgroundCanvas.heightProperty().bind(loginRoot.heightProperty());

        // Enter o username -> focus password
        usernameField.setOnAction(e -> passwordField.requestFocus());

        // Enter o password -> login
        passwordField.setOnAction(e -> {
            if (!loginButton.isDisabled()) {
                handleLogin();
            }
        });
    }

    public void setOnRegisterRequested(Runnable callback) {
        this.onRegisterRequested = callback;
    }

    public void setOnLoginSuccess(Runnable callback) {
        this.onLoginSuccess = callback;
    }

    public void setLoadingCallbacks(Consumer<String> onShowLoading, Runnable onHideLoading) {
        this.onShowLoading = onShowLoading;
        this.onHideLoading = onHideLoading;
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        setLoading(true);
        messageLabel.setText("Logging in...");

        if (onShowLoading != null) {
            onShowLoading.accept("Logging in ...");
        }

        Task<UserDAO.LoginResult> task = new Task<>() {
            @Override
            protected UserDAO.LoginResult call() {
                return userDAO.login(username, password);
            }
        };

        task.setOnSucceeded(event -> {
            setLoading(false);

            if (onHideLoading != null) {
                onHideLoading.run();
            }

            UserDAO.LoginResult result = task.getValue();

            if (!result.success()) {
                messageLabel.setText(result.message());
                passwordField.clear();
                passwordField.requestFocus();
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

            if (onHideLoading != null) {
                onHideLoading.run();
            }

            messageLabel.setText("Unable to connect to the database.");

            if (task.getException() != null) {
                task.getException().printStackTrace();
            }
        });

        DatabaseExecutor.getExecutor().execute(task);
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
        usernameField.setText(username == null ? "" : username);
        passwordField.clear();
        passwordField.requestFocus();
    }

    public void prepareCinematicReveal() {
        if (roomBackground != null) {
            roomBackground.prepareCinematicReveal();
            roomBackground.start();
        }
        loginBox.setOpacity(0.0);
        loginBox.setScaleX(0.97);
        loginBox.setScaleY(0.97);
        loginBox.setDisable(true);
    }

    public void showBackgroundImmediately() {
        if (roomBackground != null) {
            roomBackground.showImmediately();
            roomBackground.start();
        }
        loginBox.setOpacity(1.0);
        loginBox.setScaleX(1.0);
        loginBox.setScaleY(1.0);
        loginBox.setDisable(false);
    }

    public void finishCinematicReveal() {
        loginBox.setDisable(false);
        usernameField.requestFocus();
    }

    public VBox getLoginBox() { return loginBox; }
    public DoubleProperty roomRevealProperty() { return roomBackground.roomRevealProperty(); }
    public DoubleProperty monitorPowerProperty() { return roomBackground.monitorPowerProperty(); }
    public DoubleProperty rainRevealProperty() { return roomBackground.rainRevealProperty(); }
    public DoubleProperty ambientRevealProperty() { return roomBackground.ambientRevealProperty(); }

    public void startBackground() {
        if (roomBackground != null) {
            roomBackground.start();
        }
    }

    public void stopBackground() {
        if (roomBackground != null) {
            roomBackground.stop();
        }
    }

    private void setLoading(boolean loading) {
        usernameField.setDisable(loading);
        passwordField.setDisable(loading);
        loginButton.setDisable(loading);
    }
}