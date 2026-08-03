package com.soulknight.ui;

import com.soulknight.utils.SoundManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

public final class Menu {

    @FXML private Button btnNewGame;
    @FXML private Button btnContinue;
    @FXML private Button btnLeaderboard;
    @FXML private Button btnSettings;
    @FXML private Button btnShop;
    @FXML private Button btnExit;
    @FXML private ImageView imgCharacter;

    private Runnable onNewGameCallback;
    private Runnable onContinueCallback;
    private Runnable onLeaderboardCallback;
    private Runnable onSettingsCallback;
    private Runnable onShopCallback;
    private Runnable onAccountRequested;

    private Timeline spriteAnimation;

    private static final String SPRITE_PATH = "/assets/sprites/Knight.png";
    private static final int TOTAL_FRAMES = 3;
    private static final int FRAME_WIDTH = 32;
    private static final int FRAME_HEIGHT = 32;
    private static final double SCALE_FACTOR = 4.0;

    private int currentFrameIndex = 0;

    @FXML
    public void initialize() {
        try {
            Image spriteSheet = new Image(getClass().getResourceAsStream(SPRITE_PATH));
            imgCharacter.setImage(spriteSheet);
            imgCharacter.setSmooth(false);
            imgCharacter.setFitWidth(FRAME_WIDTH * SCALE_FACTOR);
            imgCharacter.setFitHeight(FRAME_HEIGHT * SCALE_FACTOR);
            imgCharacter.setViewport(new Rectangle2D(0, 0, FRAME_WIDTH, FRAME_HEIGHT));

            spriteAnimation = new Timeline(
                    new KeyFrame(Duration.millis(200), event -> {
                        double xOffset = currentFrameIndex * FRAME_WIDTH;
                        imgCharacter.setViewport(new Rectangle2D(xOffset, 0, FRAME_WIDTH, FRAME_HEIGHT));
                        currentFrameIndex = (currentFrameIndex + 1) % TOTAL_FRAMES;
                    })
            );

            spriteAnimation.setCycleCount(Timeline.INDEFINITE);
            spriteAnimation.play();

        } catch (Exception exception) {
            System.err.println("Khong the tai sprite Knight.");
            exception.printStackTrace();
        }
    }

    public void setOnNewGameRequested(Runnable callback) {
        this.onNewGameCallback = callback;
    }

    public void setOnContinueRequested(Runnable callback) {
        this.onContinueCallback = callback;
    }

    public void setOnLeaderboardRequested(Runnable callback) {
        this.onLeaderboardCallback = callback;
    }

    public void setOnSettingsRequested(Runnable callback) {
        this.onSettingsCallback = callback;
    }

    public void setOnShopRequested(Runnable callback) {
        this.onShopCallback = callback;
    }

    public void setContinueAvailable(boolean available) {
        btnContinue.setDisable(!available);
        btnContinue.setOpacity(available ? 1.0 : 0.5);
    }

    public void setOnAccountRequested(Runnable callback) {
        this.onAccountRequested = callback;
    }

    @FXML
    private void openAccount() {
        SoundManager.getInstance().playSFX("button");

        if (onAccountRequested != null) {
            onAccountRequested.run();
        }
    }


    public void startAnimation() {
        if (spriteAnimation != null) {
            spriteAnimation.play();
        }
    }

    @FXML
    private void onNewGameClicked(ActionEvent event) {
        stopAnimation();
        if (onNewGameCallback != null) onNewGameCallback.run();
    }

    @FXML
    private void onContinueClicked(ActionEvent event) {
        stopAnimation();
        if (onContinueCallback != null) onContinueCallback.run();
    }

    @FXML
    private void onLeaderboardClicked(ActionEvent event) {
        if (onLeaderboardCallback != null) onLeaderboardCallback.run();
    }

    @FXML
    private void onSettingsClicked(ActionEvent event) {
        if (onSettingsCallback != null) onSettingsCallback.run();
    }

    @FXML
    private void onShopClicked(ActionEvent event) {
        if (onShopCallback != null) onShopCallback.run();
    }

    @FXML
    private void onExitClicked(ActionEvent event) {
        stopAnimation();
        Platform.exit();
        System.exit(0);
    }

    private void stopAnimation() {
        if (spriteAnimation != null) {
            spriteAnimation.stop();
        }
    }
}