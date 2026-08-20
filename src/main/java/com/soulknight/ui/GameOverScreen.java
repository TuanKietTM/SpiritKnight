package com.soulknight.ui;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public final class GameOverScreen {

    @FXML private StackPane rootPane;
    @FXML private Label promptLabel;

    private Runnable onRestartCallback;

    @FXML
    public void initialize() {
        startPromptBlink();
        setupKeyListeners();
    }

    public void setOnRestart(Runnable onRestartCallback) {
        this.onRestartCallback = onRestartCallback;
    }

    @FXML
    private void handleRestart() {
        if (onRestartCallback != null) {
            onRestartCallback.run();
        }
    }

    private void startPromptBlink() {
        if (promptLabel == null) return;

        FadeTransition blink = new FadeTransition(Duration.millis(750), promptLabel);
        blink.setFromValue(1.0);
        blink.setToValue(0.25);
        blink.setCycleCount(Animation.INDEFINITE);
        blink.setAutoReverse(true);
        blink.play();
    }

    private void setupKeyListeners() {
        if (rootPane == null) return;

        rootPane.setFocusTraversable(true);
        rootPane.setOnKeyPressed((KeyEvent event) -> {
            if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
                handleRestart();
            }
        });
    }
}