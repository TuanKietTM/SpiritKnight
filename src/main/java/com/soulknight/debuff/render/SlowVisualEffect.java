package com.soulknight.debuff.render;

import com.soulknight.engine.Camera;
import com.soulknight.entity.Player;
import com.soulknight.utils.Vector2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class SlowVisualEffect implements DebuffVisualEffect {

    private double stateTime = 0.0;
    private static final Color[] SLOW_ARROW_COLORS = new Color[]{
            Color.web("#00E5FF"),
            Color.web("#29B6F6"),
            Color.web("#0288D1"),
            Color.web("#80DEEA")
    };

    @Override
    public void update(double deltaSeconds) {
        this.stateTime += deltaSeconds;
    }

    @Override
    public void render(GraphicsContext gc, Camera camera, Player player) {
        if (player == null) return;

        Vector2D playerPos = player.getPosition();
        double playerRadius = player.getRadius();

        double screenX = camera.worldToScreenX(playerPos.getX());
        double screenY = camera.worldToScreenY(playerPos.getY());
        double zoom = camera.getZoom();

        double heightBound = playerRadius * 2.2 * zoom;
        double widthBound = playerRadius * 1.4 * zoom;

        gc.save();
        gc.setImageSmoothing(false);

        int arrowCount = 6;

        for (int i = 0; i < arrowCount; i++) {
            double progress = (stateTime * 1.6 + i * 0.22) % 1.0;
            double pOffsetX = Math.sin(i * 2.3) * (widthBound * 0.5);
            double pOffsetY = progress * heightBound;

            double drawX = screenX + pOffsetX;
            double drawY = (screenY - heightBound * 0.4) + pOffsetY;
            double alpha = (1.0 - progress) * 0.75;
            gc.setGlobalAlpha(Math.max(0.0, alpha));

            gc.setFill(SLOW_ARROW_COLORS[i % SLOW_ARROW_COLORS.length]);

            renderPixelDownArrow(gc, drawX, drawY);
        }

        gc.restore();
    }

    private void renderPixelDownArrow(GraphicsContext gc, double cx, double cy) {
        int px = (int) Math.round(cx);
        int py = (int) Math.round(cy);
        int pSize = 2;

        gc.fillRect(px - pSize, py - pSize * 3, pSize * 2, pSize * 3);

        gc.fillRect(px - pSize * 3, py, pSize * 6, pSize);

        gc.fillRect(px - pSize * 2, py + pSize, pSize * 4, pSize);

        gc.fillRect(px - pSize, py + pSize * 2, pSize * 2, pSize);
    }
}