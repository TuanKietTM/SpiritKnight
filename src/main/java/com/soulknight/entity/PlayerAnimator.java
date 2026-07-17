package com.soulknight.entity;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import com.soulknight.engine.Camera;

public class PlayerAnimator {

    public enum State {
        IDLE, RUN
    }

    private Image idleSpriteSheet;
    private Image runSpriteSheet;

    private State currentState = State.IDLE;
    private int currentFrameIndex = 0;
    private double frameTimer = 0.0;
    private static final double FRAME_DURATION = 0.15;

    private static final double SPRITE_FRAME_WIDTH = 32;
    private static final double SPRITE_FRAME_HEIGHT = 32;

    public PlayerAnimator() {
        loadSpriteSheets();
    }

    private void loadSpriteSheets() {
        try {
            idleSpriteSheet = new Image(getClass().getResourceAsStream("/assets/sprites/IDLE_32.png"));
            runSpriteSheet = new Image(getClass().getResourceAsStream("/assets/sprites/run1_32.png"));
        } catch (Exception e) {
            System.err.println("Loi tai ");
        }
    }

    public void update(State newState, double deltaSeconds) {
        if (this.currentState != newState) {
            this.currentState = newState;
            this.currentFrameIndex = 0;
            this.frameTimer = 0.0;
        }

        Image currentSheet = (currentState == State.IDLE) ? idleSpriteSheet : runSpriteSheet;
        if (currentSheet == null) return;
        frameTimer += deltaSeconds;
        while (frameTimer >= FRAME_DURATION) {
            frameTimer -= FRAME_DURATION;

            int maxFramesDetected = (int) (currentSheet.getWidth() / SPRITE_FRAME_WIDTH);
            int totalFrames = maxFramesDetected > 0 ? maxFramesDetected : ((currentState == State.IDLE) ? 3 : 4);
            currentFrameIndex = (currentFrameIndex + 1) % totalFrames;
        }
    }

    public void render(GraphicsContext gc, Camera camera, double worldX, double worldY, double width, double height, double radius, boolean isFacingLeft) {
        Image currentSheet = (currentState == State.IDLE) ? idleSpriteSheet : runSpriteSheet;
        if (currentSheet == null) currentSheet = idleSpriteSheet;
        if (currentSheet == null) return;

        int maxFrames = (int) (currentSheet.getWidth() / SPRITE_FRAME_WIDTH);
        if (maxFrames > 0 && currentFrameIndex >= maxFrames) {
            currentFrameIndex = 0;
        }

        double sourceX = currentFrameIndex * SPRITE_FRAME_WIDTH;
        double sourceY = 0.0;

        double zoom = camera.getZoom();

        double drawX = worldX - (width / 2.0);
        double drawY = (worldY + radius) - height;

        double screenX = camera.worldToScreenX(drawX);
        double screenY = camera.worldToScreenY(drawY);

        double renderWidth = width * zoom;
        double renderHeight = height * zoom;

        gc.save();
        gc.setImageSmoothing(false);

        if (isFacingLeft) {
            gc.translate(screenX + renderWidth, screenY);
            gc.scale(-1, 1);
            gc.drawImage(currentSheet, sourceX, sourceY, SPRITE_FRAME_WIDTH, SPRITE_FRAME_HEIGHT, 0, 0, renderWidth, renderHeight);
        } else {
            gc.drawImage(currentSheet, sourceX, sourceY, SPRITE_FRAME_WIDTH, SPRITE_FRAME_HEIGHT, screenX, screenY, renderWidth, renderHeight);
        }
        gc.restore();
    }
}