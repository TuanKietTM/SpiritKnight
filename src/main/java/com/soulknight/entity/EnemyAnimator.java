package com.soulknight.entity;

import com.soulknight.engine.Camera;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

/**
 * Quan li cac animation cua Enemy
 * enemy thuong thi chi co trang thai run
 */
public class EnemyAnimator {

    private Image runSpriteSheet;

    private int currentFrameIndex = 0;
    private double frameTimer = 0.0;
    private static final double FRAME_DURATION = 0.15;

    private static final double SPRITE_FRAME_WIDTH = 64.0;
    private static final double SPRITE_FRAME_HEIGHT = 64.0;

    public EnemyAnimator(EnemyArchetype archetype) {
        loadSpriteSheets(archetype);
    }

    private void loadSpriteSheets(EnemyArchetype archetype) {
        try {
            String path = switch (archetype) {
                case MELEE_NORMAL -> "/assets/Enemy/slime_sheet.png";
                case RANGED_NORMAL, RANGED_ELITE -> "/assets/Enemy/sleketon_sheet.png";
                default -> "/assets/Enemy/slime_sheet.png";
            };

            runSpriteSheet = new Image(getClass().getResourceAsStream(path));
        } catch (Exception e) {
            System.err.println("Loi tai " + archetype + ": " + e.getMessage());
        }
    }
//cap nhat cac frame anh
    public void update(double deltaSeconds) {
        if (runSpriteSheet == null) return;

        frameTimer += deltaSeconds;
        while (frameTimer >= FRAME_DURATION) {
            frameTimer -= FRAME_DURATION;

            int maxFramesDetected = (int) (runSpriteSheet.getWidth() / SPRITE_FRAME_WIDTH);
            int totalFrames = maxFramesDetected > 0 ? maxFramesDetected : 4;
            currentFrameIndex = (currentFrameIndex + 1) % totalFrames;
        }
    }

    public void render(GraphicsContext gc, Camera camera, double worldX, double worldY,
                       double width, double height, double radius, boolean isFacingLeft) {
        if (runSpriteSheet == null) return;

        int maxFrames = (int) (runSpriteSheet.getWidth() / SPRITE_FRAME_WIDTH);
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
            gc.drawImage(runSpriteSheet, sourceX, sourceY, SPRITE_FRAME_WIDTH, SPRITE_FRAME_HEIGHT, 0, 0, renderWidth, renderHeight);
        } else {
            gc.drawImage(runSpriteSheet, sourceX, sourceY, SPRITE_FRAME_WIDTH, SPRITE_FRAME_HEIGHT, screenX, screenY, renderWidth, renderHeight);
        }
        gc.restore();
    }
}