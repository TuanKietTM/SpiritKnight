package com.soulknight.buff.effect.combat;

import com.soulknight.engine.Camera;
import com.soulknight.utils.Vector2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.BlendMode;
import javafx.scene.paint.Color;

import java.util.Random;

/**
 * Holy Nova:
 * vong nang luong vang-trang bung ra tu Player.
 */
public final class HolyNovaEffect implements CombatEffect {
    private static final double DURATION = 0.65;
    private static final double FLASH_DURATION = 0.08;
    private static final int RAY_COUNT = 24;
    private static final int PARTICLE_COUNT = 40;
    private final Vector2D center;
    private final double maxRadius;
    private final HolyParticle[] particles = new HolyParticle[PARTICLE_COUNT];
    private double elapsed;
    private double remaining = DURATION;

    public HolyNovaEffect(Vector2D center, double maxRadius) {this.center = center.copy();
        /*
         * Visual khong can bung het 20 tile.
         * Gioi han de khong che toan bo map.
         */
        this.maxRadius = Math.min(maxRadius, 360.0);
        Random random = new Random();
        for (int i = 0;
             i < particles.length;
             i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double speed = 80.0 + random.nextDouble() * 180.0;
            particles[i] = new HolyParticle(Math.cos(angle) * speed, Math.sin(angle) * speed);
        }
    }

    @Override
    public void update(
            double deltaSeconds) {
        if (deltaSeconds <= 0.0 || isFinished()) {
            return;
        }
        elapsed += deltaSeconds;
        remaining = Math.max(0.0, remaining - deltaSeconds);
        for (HolyParticle particle : particles) {
            particle.x += particle.velocityX * deltaSeconds;
            particle.y += particle.velocityY * deltaSeconds;
            double damping = Math.max(0.0, 1.0 - 2.8 * deltaSeconds);
            particle.velocityX *= damping;
            particle.velocityY *= damping;
        }
    }

    @Override
    public void render(GraphicsContext gc, Camera camera) {
        if (gc == null || camera == null || isFinished()) {
            return;
        }

        double progress = Math.min(1.0, elapsed / DURATION);
        double life = 1.0 - progress;
        double zoom = camera.getZoom();
        double x = camera.worldToScreenX(center.getX());
        double y = camera.worldToScreenY(center.getY());
        gc.save();
        gc.setImageSmoothing(false);
        renderFlash(gc);
        gc.setGlobalBlendMode(BlendMode.ADD);

        /*
         * Shockwave bung nhanh luc dau.
         */
        double eased = 1.0 - Math.pow(1.0 - progress, 3.0);
        double radius = maxRadius * eased * zoom;
        gc.setGlobalAlpha(life * 0.9);
        gc.setStroke(Color.rgb(255, 235, 120));
        gc.setLineWidth(Math.max(2.0, 7.0 * zoom * life));
        gc.strokeOval(x - radius, y - radius,
                radius * 2.0, radius * 2.0);

        /*
         * Vong trang ben trong.
         */
        double innerRadius = radius * 0.72;
        gc.setGlobalAlpha(life * 0.75);
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(Math.max(1.0, 3.0 * zoom));
        gc.strokeOval(x - innerRadius, y - innerRadius, innerRadius * 2.0, innerRadius * 2.0);
        renderRays(gc, x, y, radius, life);
        renderParticles(gc, camera, zoom, life);
        gc.restore();
    }
    private void renderFlash(GraphicsContext gc) {
        if (elapsed > FLASH_DURATION) {
            return;
        }
        double p = elapsed / FLASH_DURATION;

        gc.save();
        gc.setGlobalAlpha((1.0 - p) * 0.38);
        gc.setFill(Color.rgb(255, 245, 190));
        gc.fillRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());
        gc.restore();
    }
    private void renderRays(GraphicsContext gc, double centerX, double centerY, double radius, double life) {
        gc.setGlobalAlpha(life * 0.65);
        gc.setStroke(Color.rgb(255, 245, 180));
        gc.setLineWidth(2.0);
        for (int i = 0; i < RAY_COUNT; i++) {
            double angle = Math.PI * 2.0 * i / RAY_COUNT;
            double start = radius * 0.45;
            double end = radius * (i % 2 == 0 ? 1.05 : 0.82);
            gc.strokeLine(centerX + Math.cos(angle) * start,
                    centerY + Math.sin(angle) * start,
                    centerX + Math.cos(angle) * end,
                    centerY + Math.sin(angle) * end);
        }
    }

    private void renderParticles(GraphicsContext gc, Camera camera, double zoom, double life) {
        gc.setGlobalAlpha(life);
        gc.setFill(Color.rgb(255, 240, 135));
        double size = Math.max(2.0, Math.round(2.0 * zoom));
        for (HolyParticle particle : particles) {
            double x = camera.worldToScreenX(center.getX() + particle.x);
            double y = camera.worldToScreenY(center.getY() + particle.y);
            /*
             * Dau cong pixel.
             */
            gc.fillRect(Math.round(x - size), Math.round(y), size * 3.0, size);

            gc.fillRect(Math.round(x), Math.round(y - size), size, size * 3.0);
        }
    }

    @Override
    public boolean isFinished() {
        return remaining <= 0.0;
    }

    private static final class HolyParticle {
        private double x;
        private double y;
        private double velocityX;
        private double velocityY;
        private HolyParticle(double velocityX, double velocityY) {
            this.velocityX = velocityX;
            this.velocityY = velocityY;
        }
    }
}