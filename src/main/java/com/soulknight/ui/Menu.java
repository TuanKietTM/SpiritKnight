package com.soulknight.ui;

import com.soulknight.utils.SoundManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public final class Menu {

    @FXML private Button btnNewGame;
    @FXML private Button btnContinue;
    @FXML private Button btnLeaderboard;
    @FXML private Button btnSettings;
    @FXML private Button btnShop;
    @FXML private Button btnExit;

    @FXML private ImageView newGameIcon;
    @FXML private ImageView continueIcon;
    @FXML private ImageView leaderboardIcon;
    @FXML private ImageView shopIcon;
    @FXML private ImageView accountIcon;
    @FXML private ImageView settingsIcon;
    @FXML private ImageView exitIcon;

    private Runnable onNewGameCallback;
    private Runnable onContinueCallback;
    private Runnable onLeaderboardCallback;
    private Runnable onSettingsCallback;
    private Runnable onShopCallback;
    private Runnable onAccountRequested;

    @FXML
    public void initialize() {
        setIcon(newGameIcon, "/assets/icon/play.png");
        setIcon(continueIcon, "/assets/icon/continue.png");
        setIcon(leaderboardIcon, "/assets/icon/tropy.png");
        setIcon(shopIcon, "/assets/icon/shop.png");
        setIcon(accountIcon, "/assets/icon/person.png");
        setIcon(settingsIcon, "/assets/icon/setting.png");
        setIcon(exitIcon, "/assets/icon/close.png");
    }

    private void setIcon(ImageView imageView, String path) {
        if (imageView == null) {
            System.err.println("ImageView chua duoc gan trong FXML: " + path);
            return;
        }

        var resource = getClass().getResource(path);

        if (resource == null) {
            System.err.println("Khong tim thay icon: " + path);
            imageView.setVisible(false);
            imageView.setManaged(false);
            return;
        }

        imageView.setImage(new Image(resource.toExternalForm()));
        imageView.setSmooth(false);
        imageView.setPreserveRatio(true);
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

    public void setOnAccountRequested(Runnable callback) {
        this.onAccountRequested = callback;
    }

    public void setContinueAvailable(boolean available) {
        btnContinue.setDisable(!available);
    }

    @FXML
    private void openAccount() {
        SoundManager.getInstance().playSFX("button");

        if (onAccountRequested != null) {
            onAccountRequested.run();
        }
    }

    /*
     * Giu lai de UIManager hien tai khong can sua.
     * Menu khong con sprite animation.
     */
    public void startAnimation() {
    }

    public void stopAnimation() {
    }

    @FXML
    private void onNewGameClicked(ActionEvent event) {
        if (onNewGameCallback != null) {
            onNewGameCallback.run();
        }
    }

    @FXML
    private void onContinueClicked(ActionEvent event) {
        if (onContinueCallback != null) {
            onContinueCallback.run();
        }
    }

    @FXML
    private void onLeaderboardClicked(ActionEvent event) {
        if (onLeaderboardCallback != null) {
            onLeaderboardCallback.run();
        }
    }

    @FXML
    private void onSettingsClicked(ActionEvent event) {
        if (onSettingsCallback != null) {
            onSettingsCallback.run();
        }
    }

    @FXML
    private void onShopClicked(ActionEvent event) {
        if (onShopCallback != null) {
            onShopCallback.run();
        }
    }

    @FXML
    private void onExitClicked(ActionEvent event) {
        Platform.exit();
        System.exit(0);
    }
}