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
    @FXML private Button btnExit;
    @FXML private ImageView imgCharacter;

    private Runnable onPlayCallback;
    private Timeline spriteAnimation;
    private static final String SPRITE_PATH = "/assets/sprites/Knight.png";
    private static final int TOTAL_FRAMES = 3;
    private static final int FRAME_WIDTH = 32;
    private static final int FRAME_HEIGHT = 32;
    private static final double SCALE_FACTOR = 3.0;

    private int currentFrameIndex = 0;

    @FXML
    public void initialize() {
        try {
            Image spriteSheet = new Image(getClass().getResourceAsStream(SPRITE_PATH));
            imgCharacter.setImage(spriteSheet);
            imgCharacter.setFitWidth(FRAME_WIDTH * SCALE_FACTOR);
            imgCharacter.setFitHeight(FRAME_HEIGHT * SCALE_FACTOR);
            imgCharacter.setViewport(new Rectangle2D(0, 0, FRAME_WIDTH, FRAME_HEIGHT));
            spriteAnimation = new Timeline(
                    new KeyFrame(Duration.millis(150), event -> { // Cứ mỗi 150ms nhảy sang frame tiếp theo
                        // Tính toán tọa độ X của frame tiếp theo trên tấm ảnh dài
                        double xOffset = currentFrameIndex * FRAME_WIDTH;

                        // Cắt và cập nhật Viewport hiển thị
                        imgCharacter.setViewport(new Rectangle2D(xOffset, 0, FRAME_WIDTH, FRAME_HEIGHT));

                        // Tăng frame hoặc quay vòng về 0
                        currentFrameIndex = (currentFrameIndex + 1) % TOTAL_FRAMES;
                    })
            );
            spriteAnimation.setCycleCount(Timeline.INDEFINITE); // Chạy lặp vô hạn
            spriteAnimation.play(); // Kích hoạt chạy luôn khi vào Menu

        } catch (Exception e) {
            System.err.println("❌ Menu Controller: Không thể nạp được ảnh Sprite nhân vật!");
            e.printStackTrace();
        }
    }

    public void setOnPlayRequested(Runnable callback) {
        this.onPlayCallback = callback;
    }

    @FXML
    private void onPlayClicked(ActionEvent event) {
        // Khi bấm chơi game, dừng Timeline animation của Menu lại để giải phóng RAM tối ưu FPS cho game
        if (spriteAnimation != null) {
            spriteAnimation.stop();
        }
        if (onPlayCallback != null) {
            onPlayCallback.run();
        }
    }

    @FXML
    private void onSettingsClicked(ActionEvent event) {
        System.out.println("🔧 Đã bấm Settings!");
    }

    @FXML
    private void onExitClicked(ActionEvent event) {
        if (spriteAnimation != null) {
            spriteAnimation.stop();
        }
        Platform.exit();
        System.exit(0);
    }
}