package com.soulknight.debuff.render;

import com.soulknight.engine.Camera;
import com.soulknight.entity.Player;
import com.soulknight.utils.Vector2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class PoisonVisualEffect implements DebuffVisualEffect {

    private double stateTime = 0.0;

    private static final Color[] POISON_PARTICLE_COLORS = new Color[]{
            Color.web("#E066FF"),
            Color.web("#BA68C8"),
            Color.web("#9C27B0"),
            Color.web("#7B1FA2"),
            Color.web("#4A148C")
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

        double heightBound = playerRadius * 2.5 * zoom;
        double widthBound = playerRadius * 1.5 * zoom;

        gc.save();
        gc.setImageSmoothing(false);

        int particleCount = 16;

        for (int i = 0; i < particleCount; i++) {
            double speedMultiplier = 1.2 + (i % 4) * 0.3;
            double progress = (stateTime * speedMultiplier + i * 0.17) % 1.0;

            double waveFrequency = 3.0 + (i % 3);
            double pOffsetX = Math.sin(stateTime * waveFrequency + i * 1.5) * (widthBound * 0.5);
            double pOffsetY = progress * heightBound;

            double drawX = screenX + pOffsetX;
            double drawY = (screenY + playerRadius * 0.6 * zoom) - pOffsetY;

            double alpha = 1.0 - progress;
            gc.setGlobalAlpha(Math.max(0.0, alpha));

            gc.setFill(POISON_PARTICLE_COLORS[i % POISON_PARTICLE_COLORS.length]);

            double particleSize = (i % 3 == 0) ? 4.0 : ((i % 2 == 0) ? 3.0 : 2.0);

            gc.fillRect(
                    Math.round(drawX - particleSize / 2.0),
                    Math.round(drawY - particleSize / 2.0),
                    particleSize,
                    particleSize
            );
        }

        gc.restore();
    }
}