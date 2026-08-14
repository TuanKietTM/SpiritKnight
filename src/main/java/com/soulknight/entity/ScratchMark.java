package com.soulknight.entity;

import com.soulknight.engine.Camera;
import com.soulknight.engine.GameWorld;
import com.soulknight.utils.Vector2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;

import java.util.Random;

public class ScratchMark {
    private final Vector2D position;
    private final double radius;
    private final double maxLifetime;
    private double currentLifetime;
    private final int damagePerSecond;
    private double damageTimer = 0.0;
    private final double angle;

    private final long seed;

    public ScratchMark(Vector2D position, double radius, double lifetimeSeconds, int damagePerSecond) {
        this.position = position.copy();
        this.radius = radius;
        this.maxLifetime = lifetimeSeconds;
        this.currentLifetime = lifetimeSeconds;
        this.damagePerSecond = damagePerSecond;
        this.angle = 42.0 + (Math.random() * 6.0 - 3.0);
        this.seed = (long) (Math.random() * 100000);
    }

    public void update(GameWorld world, double deltaSeconds) {
        currentLifetime -= deltaSeconds;
        damageTimer += deltaSeconds;

        if (world.getPlayer() != null && world.getPlayer().getPosition() != null) {
            double dist = world.getPlayer().getPosition().distance(position);

            if (dist <= radius + world.getPlayer().getRadius()) {
                if (damageTimer >= 0.25) {
                    int damageToApply = Math.max(1, (int)(damagePerSecond * 0.25));
                    world.getPlayer().takeDamage(damageToApply);
                    damageTimer = 0.0;
                }
            }
        }
    }

    public void render(GraphicsContext gc, Camera camera) {
        if (currentLifetime <= 0 || camera == null) return;

        double screenX = camera.worldToScreenX(position.getX());
        double screenY = camera.worldToScreenY(position.getY());
        double zoom = camera.getZoom();

        double length = radius * 4.5 * zoom;
        double alpha = Math.max(0.0, currentLifetime / maxLifetime);

        gc.save();
        gc.translate(screenX, screenY);
        gc.rotate(angle);
        gc.setGlobalAlpha(alpha);

        gc.setLineCap(StrokeLineCap.SQUARE);

        Color neonLimeCore = Color.rgb(50, 255, 80, 1.0);     // Xanh lá rực rỡ
        Color neonLimeGlow = Color.rgb(0, 230, 60, 0.6);      // Viền phát sáng xanh lá
        Color voidBlack = Color.rgb(12, 18, 12, 0.92);        // Lõi đen hư không

        gc.setLineWidth(32.0 * zoom);
        gc.setStroke(neonLimeGlow);
        gc.strokeLine(-length * 0.95, 0, length * 0.95, 0);

        gc.setLineWidth(22.0 * zoom);
        gc.setStroke(voidBlack);
        gc.strokeLine(-length, 0, length, 0);

        Random rand = new Random(seed);

        gc.setLineWidth(2.5 * zoom);
        gc.setStroke(neonLimeCore);

        double upperEdgeY = -11.0 * zoom;
        double lowerEdgeY = 11.0 * zoom;

        gc.strokeLine(-length * 0.8, upperEdgeY, length * 0.8, upperEdgeY);
        gc.strokeLine(-length * 0.85, lowerEdgeY, length * 0.75, lowerEdgeY);

        gc.setFill(neonLimeCore);

        int glitchCount = 28;
        for (int i = 0; i < glitchCount; i++) {
            double posX = (-0.9 + rand.nextDouble() * 1.8) * length;
            double side = rand.nextBoolean() ? 1.0 : -1.0;
            double posY = (12.0 + rand.nextDouble() * 14.0) * zoom * side;

            double pWidth = (8.0 + rand.nextDouble() * 24.0) * zoom; // Độ dài hạt Glitch
            double pHeight = (1.8 + rand.nextDouble() * 2.5) * zoom; // Độ dày hạt Glitch

            gc.fillRect(posX, posY, pWidth, pHeight);
        }

        gc.setLineWidth(1.8 * zoom);
        gc.setStroke(Color.rgb(200, 255, 200, 0.9)); // Lõi trắng xanh nhẹ

        gc.strokeLine(-length * 0.6, -18.0 * zoom, length * 0.4, -18.0 * zoom);
        gc.strokeLine(-length * 0.3, 19.0 * zoom, length * 0.7, 19.0 * zoom);

        gc.restore();
    }

    public boolean isExpired() {
        return currentLifetime <= 0;
    }
}