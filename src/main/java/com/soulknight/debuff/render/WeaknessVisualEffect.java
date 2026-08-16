package com.soulknight.debuff.render;

import com.soulknight.engine.Camera;
import com.soulknight.entity.Player;
import com.soulknight.utils.Vector2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class WeaknessVisualEffect implements DebuffVisualEffect {

    private double stateTime = 0.0;

    private static final Color COLOR_HEART_MAIN  = Color.web("#B71C1C");
    private static final Color COLOR_HEART_DARK  = Color.web("#5C061C");
    private static final Color COLOR_HEART_LIGHT = Color.web("#E53935");
    private static final Color COLOR_CRACK       = Color.web("#1A0307");
    private static final Color COLOR_PARTICLE    = Color.web("#FF5252");

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
        double drawX = screenX;
        double drawY = screenY - (playerRadius * 2.6 * zoom);

        gc.save();
        gc.setImageSmoothing(false);
        double shakeX = (Math.random() - 0.5) * 1.5;
        double floatOffsetY = Math.sin(stateTime * 3.0) * 3.0;

        double finalX = drawX + shakeX;
        double finalY = drawY + floatOffsetY;
        renderBigBrokenHeart(gc, finalX, finalY);
        renderCrumblingShards(gc, finalX, finalY);

        gc.restore();
    }
    private void renderBigBrokenHeart(GraphicsContext gc, double cx, double cy) {
        int px = (int) Math.round(cx);
        int py = (int) Math.round(cy);
        int pSize = 3;

        gc.setFill(Color.web("#0D0003"));
        gc.fillRect(px - pSize * 5, py - pSize * 4, pSize * 10, pSize * 9);
        gc.setFill(COLOR_HEART_MAIN);
        gc.fillRect(px - pSize * 4, py - pSize * 3, pSize * 3, pSize);
        gc.fillRect(px + pSize,     py - pSize * 3, pSize * 3, pSize);

        gc.fillRect(px - pSize * 4, py - pSize * 2, pSize * 8, pSize);

        gc.fillRect(px - pSize * 4, py - pSize,     pSize * 8, pSize * 2);

        gc.fillRect(px - pSize * 3, py + pSize,     pSize * 6, pSize);
        gc.fillRect(px - pSize * 2, py + pSize * 2, pSize * 4, pSize);
        gc.fillRect(px - pSize,     py + pSize * 3, pSize * 2, pSize);

        gc.setFill(COLOR_HEART_DARK);
        gc.fillRect(px + pSize, py - pSize * 2, pSize * 3, pSize * 3);
        gc.fillRect(px,         py + pSize,     pSize * 3, pSize);
        gc.fillRect(px,         py + pSize * 2, pSize * 2, pSize);

        gc.setFill(COLOR_HEART_LIGHT);
        gc.fillRect(px - pSize * 3, py - pSize * 2, pSize, pSize * 2);

        gc.setFill(COLOR_CRACK);
        gc.fillRect(px - pSize, py - pSize * 3, pSize, pSize);
        gc.fillRect(px,         py - pSize * 2, pSize, pSize);
        gc.fillRect(px - pSize, py - pSize,     pSize, pSize);
        gc.fillRect(px,         py,             pSize, pSize);
        gc.fillRect(px - pSize, py + pSize,     pSize, pSize);
    }

    private void renderCrumblingShards(GraphicsContext gc, double cx, double cy) {
        int shardCount = 8;

        for (int i = 0; i < shardCount; i++) {
            double speed = 1.5 + (i % 3) * 0.5;
            double progress = (stateTime * speed + i * 0.15) % 1.0;

            double spreadX = Math.sin(i * 2.5 + stateTime * 2.0) * (6.0 + (i % 4) * 3.0);
            double dropY = progress * 20.0;

            double alpha = Math.max(0.0, 1.0 - progress);
            gc.setGlobalAlpha(alpha);

            gc.setFill(i % 2 == 0 ? COLOR_PARTICLE : COLOR_HEART_MAIN);

            double shardSize = (i % 3 == 0) ? 3.0 : 2.0;

            gc.fillRect(
                    cx + spreadX - shardSize / 2.0,
                    cy + dropY + 4.0,
                    shardSize,
                    shardSize
            );
        }
    }
}