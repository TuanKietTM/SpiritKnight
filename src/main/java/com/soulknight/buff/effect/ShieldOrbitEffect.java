package com.soulknight.buff.effect;

import com.soulknight.engine.Camera;
import com.soulknight.entity.Player;
import com.soulknight.utils.ResourceLoader;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

/**
 * Ba icon Shield quay theo quy dao elip quanh Player.
 * Nua tren quy dao duoc ve sau lung Player.
 * Nua duoi quy dao duoc ve truoc Player.
 */
public final class ShieldOrbitEffect implements BuffVisualEffect {
    private static final int SHIELD_COUNT = 3;
    private static final double ROTATION_SPEED = 95.0;
    // Quy dao om sat than Player
    private static final double ORBIT_RADIUS_X = 17.0;
    private static final double ORBIT_RADIUS_Y = 10.0;
    private static final double ICON_SIZE = 20.0;
    private static final String SHIELD_IMAGE_PATH = "/assets/buff/shield.png";
    private final Image shieldImage;
    private boolean active;
    private double angleDegrees;

    public ShieldOrbitEffect() {
        shieldImage = ResourceLoader.image(SHIELD_IMAGE_PATH);
    }

    @Override
    public void start(Player player) {
        active = player != null;
        angleDegrees = 0.0;
    }

    @Override
    public void update(Player player, double deltaSeconds, double remainingSeconds, double durationSeconds) {
        if (!active || player == null || deltaSeconds <= 0.0) {
            return;
        }
        angleDegrees += ROTATION_SPEED * deltaSeconds;

        if (angleDegrees >= 360.0) {
            angleDegrees %= 360.0;
        }
    }

    @Override
    public void renderBehind(GraphicsContext graphicsContext, Camera camera, Player player, double remainingSeconds, double durationSeconds) {
        renderOrbitLayer(graphicsContext, camera, player, true);
    }

    @Override
    public void renderFront(GraphicsContext graphicsContext, Camera camera, Player player, double remainingSeconds, double durationSeconds) {
        renderOrbitLayer(graphicsContext, camera, player, false);
    }

    /**
     * behindLayer = true:
     * chi ve icon o nua tren quy dao.
     * behindLayer = false:
     * chi ve icon o nua duoi quy dao.
     */
    private void renderOrbitLayer(GraphicsContext graphicsContext, Camera camera, Player player, boolean behindLayer) {
        if (!active || graphicsContext == null || camera == null || player == null || player.getPosition() == null || shieldImage == null || shieldImage.getWidth() <= 0.0) {
            return;
        }

        double zoom = camera.getZoom();
        double centerX = camera.worldToScreenX(player.getPosition().getX());

        double centerY = camera.worldToScreenY(player.getPosition().getY());

        // Nang quy dao len phan than Player
        centerY -= 2.0 * zoom;
        double radiusX = ORBIT_RADIUS_X * zoom;

        double radiusY = ORBIT_RADIUS_Y * zoom;
        graphicsContext.save();
        graphicsContext.setImageSmoothing(false);

        for (int i = 0; i < SHIELD_COUNT; i++) {
            double currentAngle = angleDegrees + i * (360.0 / SHIELD_COUNT);
            double radians = Math.toRadians(currentAngle);
            double sinValue = Math.sin(radians);
            boolean isBehind = sinValue < 0.0;
            if (isBehind != behindLayer) {
                continue;
            }

            double shieldX = centerX + Math.cos(radians) * radiusX;
            double shieldY = centerY + sinValue * radiusY;

            /*
             * Khi o sau lung thi nho hon mot chut.
             * Khi o truoc thi lon hon mot chut.
             */
            double depth = (sinValue + 1.0) * 0.5;

            double currentSize = ICON_SIZE * zoom * (0.88 + depth * 0.12);

            graphicsContext.drawImage(shieldImage, shieldX - currentSize / 2.0,
                    shieldY - currentSize / 2.0, currentSize, currentSize);
        }
        graphicsContext.restore();
    }

    @Override
    public void notifyPlayerHit() {}

    @Override
    public void stop() {
        active = false;
    }

    @Override
    public boolean isActive() {
        return active;
    }
}