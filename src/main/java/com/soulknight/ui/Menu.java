package com.soulknight.ui;

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

    @FXML private Button btnPlay;
    @FXML private Button btnSettings;
    @FXML private Button btnShop;
    @FXML private Button btnExit;
    @FXML private ImageView imgCharacter;

    private Runnable onPlayCallback;
    private Runnable onSettingsCallback;
    private Runnable onShopCallback;

    private Timeline spriteAnimation;
    private static final String SPRITE_PATH = "/assets/sprites/Knight.png";
    private static final int TOTAL_FRAMES = 3;
    private static final int FRAME_WIDTH = 32;
    private static final int FRAME_HEIGHT = 32;
    private static final double SCALE_FACTOR = 4.0; // Tăng nhẹ kích cỡ nhân vật cho nổi bật

    private int currentFrameIndex = 0;

    @FXML
    public void initialize() {
        try {
            Image spriteSheet = new Image(getClass().getResourceAsStream(SPRITE_PATH));
            imgCharacter.setImage(spriteSheet);

            // Tắt chế độ Smooth giúp ảnh giữ nguyên chất Pixel Art thô ráp, sắc nét
            imgCharacter.setSmooth(false);
            imgCharacter.setFitWidth(FRAME_WIDTH * SCALE_FACTOR);
            imgCharacter.setFitHeight(FRAME_HEIGHT * SCALE_FACTOR);
            imgCharacter.setViewport(new Rectangle2D(0, 0, FRAME_WIDTH, FRAME_HEIGHT));

            spriteAnimation = new Timeline(
                    new KeyFrame(Duration.millis(200), event -> { // Tần suất 200ms giúp nhịp "thở" tự nhiên hơn
                        double xOffset = currentFrameIndex * FRAME_WIDTH;
                        imgCharacter.setViewport(new Rectangle2D(xOffset, 0, FRAME_WIDTH, FRAME_HEIGHT));
                        currentFrameIndex = (currentFrameIndex + 1) % TOTAL_FRAMES;
                    })
            );
            spriteAnimation.setCycleCount(Timeline.INDEFINITE);
            spriteAnimation.play();

        } catch (Exception e) {
            System.err.println("❌ Menu Controller: Không thể nạp được ảnh Sprite nhân vật!");
            e.printStackTrace();
        }
    }

    // --- Các phương thức gán hành động từ UIManager ---

    public void setOnPlayRequested(Runnable callback) {
        this.onPlayCallback = callback;
    }

    public void setOnSettingsRequested(Runnable callback) {
        this.onSettingsCallback = callback;
    }

    public void setOnShopRequested(Runnable callback) {
        this.onShopCallback = callback;
    }

    // --- Các nút xử lý sự kiện click chuột ---

    @FXML
    private void onPlayClicked(ActionEvent event) {
        stopAnimation();
        if (onPlayCallback != null) {
            onPlayCallback.run();
        }
    }

    @FXML
    private void onSettingsClicked(ActionEvent event) {
        if (onSettingsCallback != null) {
            onSettingsCallback.run(); // Kích hoạt mở bảng Settings
        }
    }

    @FXML
    private void onShopClicked(ActionEvent event) {
        if (onShopCallback != null) {
            onShopCallback.run(); // Kích hoạt mở cửa hàng
        }
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