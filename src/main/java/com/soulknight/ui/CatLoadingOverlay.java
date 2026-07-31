package com.soulknight.ui;

import com.soulknight.pet.PetType;
import javafx.animation.AnimationTimer;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.net.URL;

/**
 * class nay tao hieu ung con cat di trong khi dang load du lieu
 */
public final class CatLoadingOverlay extends StackPane {

    private static final double CANVAS_SIZE = 100.0;
    private static final double CAT_SIZE = 72.0;
    private static final PetType LOADING_CAT = PetType.CAT;

    private final Canvas canvas = new Canvas(CANVAS_SIZE, CANVAS_SIZE);
    private final Label messageLabel = new Label("Dang tai...");
    private final Image runSpriteSheet;
    private final AnimationTimer animationTimer;

    private long lastTime;
    private double animTimer;

    public CatLoadingOverlay() {
        getStyleClass().add("cat-loading-overlay");
        setAlignment(Pos.CENTER);
        setVisible(false);
        setManaged(false);
        setMouseTransparent(false);

        messageLabel.getStyleClass().add("cat-loading-text");

        VBox content = new VBox(8, canvas, messageLabel);
        content.setAlignment(Pos.CENTER);
        content.getStyleClass().add("cat-loading-box");
        getChildren().add(content);

        runSpriteSheet = loadImage(LOADING_CAT.getRunImagePath());

        animationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                updateAnimation(now);
            }
        };
    }

    public void show(String message) {
        messageLabel.setText(
                message == null || message.isBlank()
                        ? "Dang tai..."
                        : message
        );

        animTimer = 0.0;
        lastTime = 0;

        setManaged(true);
        setVisible(true);
        toFront();

        animationTimer.start();
    }

    public void hide() {
        animationTimer.stop();
        setVisible(false);
        setManaged(false);
        clearCanvas();
    }

    private void updateAnimation(long now) {
        if (runSpriteSheet == null) return;

        if (lastTime == 0) {
            lastTime = now;
            drawCurrentFrame();
            return;
        }

        double deltaSeconds =
                (now - lastTime) / 1_000_000_000.0;

        lastTime = now;
        animTimer += Math.min(deltaSeconds, 0.05);

        drawCurrentFrame();
    }

    private void drawCurrentFrame() {
        int totalFrames = LOADING_CAT.getRunFrameCount();
        if (totalFrames <= 0) totalFrames = 1;

        int currentFrame =
                (int) (animTimer / LOADING_CAT.getFrameDuration())
                        % totalFrames;

        double sx =
                currentFrame * LOADING_CAT.getFrameWidth();

        double sw = LOADING_CAT.getFrameWidth();
        double sh = LOADING_CAT.getFrameHeight();

        double drawX = (canvas.getWidth() - CAT_SIZE) / 2.0;
        double drawY = (canvas.getHeight() - CAT_SIZE) / 2.0;

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        gc.setImageSmoothing(false);

        gc.drawImage(
                runSpriteSheet,
                sx, 0, sw, sh,
                drawX, drawY, CAT_SIZE, CAT_SIZE
        );
    }

    private Image loadImage(String path) {
        if (path == null || path.isBlank()) return null;

        URL resource = getClass().getResource(path);

        if (resource == null) {
            System.err.println(
                    "Khong tim thay anh loading cat: " + path
            );
            return null;
        }

        return new Image(resource.toExternalForm(), false);
    }

    private void clearCanvas() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }
}