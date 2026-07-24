package com.soulknight.weapon;

import com.soulknight.engine.Camera;
import com.soulknight.utils.ResourceLoader;
import com.soulknight.utils.Vector2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

public final class ExplosionEffect {

    // Sprite sheet hieu ung no: 8 khung hinh xep ngang, moi khung 150x150
    private static final String SPRITE_SHEET_PATH = "/assets/effects/danno_removebg.png";
    private static final double FRAME_SIZE = 150.0;
    // Anh cua hieu ung duoc tai mot lan va dung chung cho moi vu no
    private static final Image SPRITE_SHEET = ResourceLoader.image(SPRITE_SHEET_PATH);

    private final Vector2D position;
    private final double maxRadius;
    private final double duration;
    private double elapsedTime;
    private boolean active = true;
    private final int frameCount;

    public ExplosionEffect(Vector2D position, double maxRadius, double duration, Color color) {
        this.position = position.copy();
        this.maxRadius = maxRadius;
        this.duration = duration;
        this.elapsedTime = 0.0;
        // So khung hinh suy ra tu chieu rong sprite sheet (fallback 8 neu chua tai xong)
        int detected = (int) (SPRITE_SHEET.getWidth() / FRAME_SIZE);
        this.frameCount = detected > 0 ? detected : 8;
    }

    public void update(double deltaSeconds) {
        elapsedTime += deltaSeconds;
        if (elapsedTime >= duration) {
            active = false;
        }
    }

    public void render(GraphicsContext graphicsContext, Camera camera) {
        if (!active) {
            return;
        }

        double progress = elapsedTime / duration;        // 0 -> 1
        // Chon khung hinh hien tai theo tien do, chay het mot luot roi bien mat
        int frameIndex = (int) (progress * frameCount);
        if (frameIndex >= frameCount) {
            frameIndex = frameCount - 1;
        }

        double sourceX = frameIndex * FRAME_SIZE;
        double zoom = camera.getZoom();

        // Kich thuoc ve theo world roi nhan zoom de dong bo voi cac vat the khac
        double drawSize = maxRadius * 2.5 * zoom;
        double screenX = camera.worldToScreenX(position.getX()) - drawSize / 2.0;
        double screenY = camera.worldToScreenY(position.getY()) - drawSize / 2.0;

        graphicsContext.save();
        graphicsContext.setImageSmoothing(true);
        graphicsContext.drawImage(
            SPRITE_SHEET,
            sourceX, 0.0, FRAME_SIZE, FRAME_SIZE,
            screenX, screenY, drawSize, drawSize
        );
        graphicsContext.restore();
    }

    public boolean isActive() {
        return active;
    }

    public Vector2D getPosition() {
        return position;
    }

    public double getRadius() {
        return maxRadius * (elapsedTime / duration);
    }
}
