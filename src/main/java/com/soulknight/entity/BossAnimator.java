package com.soulknight.entity;

import com.soulknight.engine.Camera;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Quan li cac animation cua BOSS
 * Boss co 3 trang thai
 */
public class BossAnimator {

    public enum State {
        IDLE, RUN, DIE
    }

    private Image idleSpriteSheet;
    private Image runSpriteSheet;
    private final List<Image> deathFrames = new ArrayList<>();

    private State currentState = State.IDLE;
    private int currentFrameIndex = 0;
    private double frameTimer = 0.0;

//  toc do di chuyen cua cac frame
    private static final double FRAME_DURATION = 0.12;

    private static final double SPRITE_FRAME_WIDTH = 64;
    private static final double SPRITE_FRAME_HEIGHT = 64;

    private boolean deathAnimationFinished = false;

    public BossAnimator() {
        loadSpriteSheets();
    }

    private void loadSpriteSheets() {
        try {
            idleSpriteSheet = loadImage("/assets/boss/BOSS_IDLE.png");
            runSpriteSheet = loadImage("/assets/boss/BOSS_RUN.png");
            int dieFrameCount = 10;
            for (int i = 1; i <= dieFrameCount; i++) {
//                hieu ung chet thi la chuoi cac anh chay duy nhat 1 lan
                Image frame = loadImage("/assets/boss/B" + i + ".png");
                if (frame != null) {
                    deathFrames.add(frame);
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi tải sprite Boss!");
            e.printStackTrace();
        }
    }

    private Image loadImage(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }

        try (InputStream stream = getClass().getResourceAsStream(path)) {
            if (stream == null) {
                System.err.println("Không tìm thấy sprite: " + path);
                return null;
            }
            return new Image(stream);
        } catch (Exception e) {
            System.err.println("Lỗi đọc đường dẫn sprite: " + path);
        }
        return null;
    }

//    update cac frmae anh
    public void update(State newState, double deltaSeconds) {
//        phan tach ra hieu ung die va run, idle thi khac nhau
        if (currentState == State.DIE && deathAnimationFinished) {
            return;
        }
        if (this.currentState != newState) {
            this.currentState = newState;
            this.currentFrameIndex = 0;
            this.frameTimer = 0.0;
        }

        frameTimer += deltaSeconds;
        while (frameTimer >= FRAME_DURATION) {
            frameTimer -= FRAME_DURATION;

            if (currentState == State.DIE) {
                if (deathFrames.isEmpty()) {
                    deathAnimationFinished = true;
                    break;
                }

                if (currentFrameIndex < deathFrames.size() - 1) {
                    currentFrameIndex++;
                } else {
                    deathAnimationFinished = true;
                    break;
                }
            } else {
                Image currentSheet = (currentState == State.IDLE) ? idleSpriteSheet : runSpriteSheet;
                if (currentSheet == null) break;
                int maxFramesDetected = (int) (currentSheet.getWidth() / SPRITE_FRAME_WIDTH);
                int totalFrames = maxFramesDetected > 0 ? maxFramesDetected : 4;
                currentFrameIndex = (currentFrameIndex + 1) % totalFrames;
            }
        }
    }

    public void render(GraphicsContext gc, Camera camera, double worldX, double worldY, double width, double height, double radius, boolean isFacingLeft) {
        double zoom = camera.getZoom();
        double drawX = worldX - (width / 2.0);
        double drawY = (worldY + radius) - height;

        double screenX = camera.worldToScreenX(drawX);
        double screenY = camera.worldToScreenY(drawY);

        double renderWidth = width * zoom;
        double renderHeight = height * zoom;

        gc.save();
        gc.setImageSmoothing(false);

        if (currentState == State.DIE) {
            if (!deathFrames.isEmpty() && currentFrameIndex < deathFrames.size()) {
                Image frame = deathFrames.get(currentFrameIndex);
                if (isFacingLeft) {
                    gc.translate(screenX + renderWidth, screenY);
                    gc.scale(-1, 1);
                    gc.drawImage(frame, 0, 0, renderWidth, renderHeight);
                } else {
                    gc.drawImage(frame, screenX, screenY, renderWidth, renderHeight);
                }
            }
        } else {
            Image currentSheet = (currentState == State.IDLE) ? idleSpriteSheet : runSpriteSheet;
            if (currentSheet == null) currentSheet = idleSpriteSheet;
            if (currentSheet == null) {
                gc.restore();
                return;
            }
            int maxFrames = (int) (currentSheet.getWidth() / SPRITE_FRAME_WIDTH);
            if (maxFrames > 0 && currentFrameIndex >= maxFrames) {
                currentFrameIndex = 0;
            }
            double sourceX = currentFrameIndex * SPRITE_FRAME_WIDTH;
            double sourceY = 0.0;

            if (isFacingLeft) {
                gc.translate(screenX + renderWidth, screenY);
                gc.scale(-1, 1);
                gc.drawImage(currentSheet, sourceX, sourceY, SPRITE_FRAME_WIDTH, SPRITE_FRAME_HEIGHT, 0, 0, renderWidth, renderHeight);
            } else {
                gc.drawImage(currentSheet, sourceX, sourceY, SPRITE_FRAME_WIDTH, SPRITE_FRAME_HEIGHT, screenX, screenY, renderWidth, renderHeight);
            }
        }

        gc.restore();
    }

    public boolean isDeathAnimationFinished() {
        return deathAnimationFinished;
    }
}