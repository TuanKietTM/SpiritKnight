package com.soulknight.debuff.render;

import com.soulknight.engine.Camera;
import com.soulknight.entity.Player;
import com.soulknight.utils.Vector2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class ConfusionVisualEffect implements DebuffVisualEffect {

    private double stateTime = 0.0;

    @Override
    public void update(double deltaSeconds) {
        this.stateTime += deltaSeconds;
    }

    @Override
    public void render(GraphicsContext gc, Camera camera, Player player) {
        // Hàm render chung không dùng nữa vì ta chia thành 2 lớp renderBack và renderFront
    }

    /**
     * Vẽ nửa vòng phía SAU lưng Player (Cần gọi TRƯỚC KHU VẼ PLAYER)
     */
    public void renderBack(GraphicsContext gc, Camera camera, Player player) {
        renderEllipseLayer(gc, camera, player, true);
    }

    /**
     * Vẽ nửa vòng phía TRƯỚC mặt Player (Cần gọi SAU KHU VẼ PLAYER)
     */
    public void renderFront(GraphicsContext gc, Camera camera, Player player) {
        renderEllipseLayer(gc, camera, player, false);
    }

    private void renderEllipseLayer(GraphicsContext gc, Camera camera, Player player, boolean isBack) {
        if (player == null) return;

        Vector2D playerPos = player.getPosition();
        double playerRadius = player.getRadius();

        double screenX = camera.worldToScreenX(playerPos.getX());
        double screenY = camera.worldToScreenY(playerPos.getY());
        double zoom = camera.getZoom();
        double centerX = screenX;
        double centerY = screenY - (playerRadius * 0.8 * zoom);
        double radiusX = playerRadius * 1.6 * zoom;
        double radiusY = playerRadius * 0.6 * zoom;

        gc.save();
        gc.setImageSmoothing(false);
        int orbitPoints = 6;
        for (int i = 0; i < orbitPoints; i++) {
            double angle = stateTime * 4.0 + (i * (2 * Math.PI / orbitPoints));
            double x = centerX + Math.cos(angle) * radiusX;
            double y = centerY + Math.sin(angle) * radiusY;
            boolean isPointInBack = Math.sin(angle) < 0;

            if (isPointInBack == isBack) {
                double scale = isBack ? 0.8 : 1.1;
                double alpha = isBack ? 0.6 : 1.0;

                gc.setGlobalAlpha(alpha);
                if (i % 2 == 0) {
                    gc.setFill(Color.web("#FFD54F"));
                } else {
                    gc.setFill(Color.web("#FF9800"));
                }
                double pSize = 3.0 * scale;
                gc.fillRect(Math.round(x - pSize / 2), Math.round(y - pSize / 2), pSize, pSize);
            }
        }

        gc.restore();
    }
}