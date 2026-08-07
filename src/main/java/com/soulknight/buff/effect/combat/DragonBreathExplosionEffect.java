package com.soulknight.buff.effect.combat;

import com.soulknight.engine.Camera;
import com.soulknight.utils.Vector2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.BlendMode;
import javafx.scene.paint.Color;

import java.util.Random;

public final class DragonBreathExplosionEffect implements CombatEffect {

    private static final double DURATION = 0.46;
    private static final double FLASH_DURATION = 0.055;
    private static final double MAX_RADIUS = 150.0;
    private static final int FIRE_COUNT = 36;
    private static final int EMBER_COUNT = 20;

    private final Vector2D center;
    private final FireParticle[] fire = new FireParticle[FIRE_COUNT];
    private final Ember[] embers = new Ember[EMBER_COUNT];

    private double elapsed;
    private double remaining = DURATION;

    public DragonBreathExplosionEffect(Vector2D center) {
        this.center = center.copy();

        Random random = new Random();

        for (int i = 0; i < fire.length; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double speed = 75.0 + random.nextDouble() * 155.0;

            fire[i] = new FireParticle(
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed,
                    2.0 + random.nextDouble() * 2.4,
                    random.nextInt(4)
            );
        }

        for (int i = 0; i < embers.length; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double speed = 150.0 + random.nextDouble() * 180.0;

            embers[i] = new Ember(
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed
            );
        }
    }

    @Override
    public void update(double deltaSeconds) {
        if (deltaSeconds <= 0.0 || isFinished()) {
            return;
        }

        elapsed += deltaSeconds;
        remaining = Math.max(0.0, remaining - deltaSeconds);

        for (FireParticle particle : fire) {
            particle.x += particle.velocityX * deltaSeconds;
            particle.y += particle.velocityY * deltaSeconds;

            double damping = Math.max(0.0, 1.0 - 4.0 * deltaSeconds);
            particle.velocityX *= damping;
            particle.velocityY *= damping;
        }

        for (Ember ember : embers) {
            ember.x += ember.velocityX * deltaSeconds;
            ember.y += ember.velocityY * deltaSeconds;

            double damping = Math.max(0.0, 1.0 - 2.5 * deltaSeconds);
            ember.velocityX *= damping;
            ember.velocityY *= damping;
        }
    }

    @Override
    public void render(GraphicsContext gc, Camera camera) {
        if (gc == null || camera == null || isFinished()) {
            return;
        }

        double zoom = camera.getZoom();
        double centerX = camera.worldToScreenX(center.getX());
        double centerY = camera.worldToScreenY(center.getY());

        double progress = Math.min(1.0, elapsed / DURATION);
        double life = 1.0 - progress;

        gc.save();
        gc.setImageSmoothing(false);

        renderFlash(gc);

        gc.setGlobalBlendMode(BlendMode.ADD);

        renderShockwave(gc, centerX, centerY, zoom, progress);
        renderCore(gc, centerX, centerY, zoom, progress);
        renderFire(gc, camera, zoom, life);
        renderEmbers(gc, camera, zoom, life);

        gc.restore();
    }
    private void renderFlash(GraphicsContext gc) {
        if (elapsed > FLASH_DURATION) {
            return;
        }
        double progress = elapsed / FLASH_DURATION;
        gc.save();
        gc.setGlobalAlpha((1.0 - progress) * 0.16);
        gc.setFill(Color.rgb(255, 80, 20));
        gc.fillRect(0.0, 0.0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight()
        );
        gc.restore();
    }

    private void renderShockwave(GraphicsContext gc, double x, double y,
                                 double zoom, double progress) {
        double eased = 1.0 - Math.pow(1.0 - progress, 3.0);
        double radius = MAX_RADIUS * eased * zoom;
        double alpha = (1.0 - progress) * 0.85;
        gc.save();
        gc.setGlobalAlpha(alpha);
        gc.setStroke(Color.rgb(255, 35, 8));
        gc.setLineWidth(Math.max(2.0, 6.0 * zoom * (1.0 - progress)));
        gc.strokeOval(x - radius, y - radius, radius * 2.0, radius * 2.0);

        double inner = radius * 0.64;
        gc.setGlobalAlpha(alpha * 0.8);
        gc.setStroke(Color.rgb(255, 145, 20));
        gc.setLineWidth(Math.max(1.0, 3.0 * zoom));
        gc.strokeOval(x - inner, y - inner, inner * 2.0, inner * 2.0);
        gc.restore();
    }

    private void renderCore(GraphicsContext gc, double x, double y,
                            double zoom, double progress) {
        if (progress > 0.58) {
            return;
        }

        double p = progress / 0.58;
        double size = (14.0 + p * 82.0) * zoom;
        double alpha = 1.0 - p;
        gc.save();
        gc.setGlobalAlpha(alpha);
        gc.setFill(Color.rgb(170, 8, 5));
        drawSquare(gc, x, y, size);
        gc.setFill(Color.rgb(255, 28, 5));
        drawSquare(gc, x, y, size * 0.80);
        gc.setFill(Color.rgb(255, 105, 10));
        drawSquare(gc, x, y, size * 0.58);
        gc.setFill(Color.rgb(255, 225, 70));
        drawSquare(gc, x, y, size * 0.34);
        gc.setFill(Color.rgb(255, 250, 215));
        drawSquare(gc, x, y, size * 0.16);
        gc.restore();
    }

    private void renderFire(GraphicsContext gc, Camera camera,
                            double zoom, double life) {
        gc.save();
        gc.setGlobalAlpha(life);

        for (FireParticle particle : fire) {
            double x = camera.worldToScreenX(center.getX() + particle.x);
            double y = camera.worldToScreenY(center.getY() + particle.y);

            switch (particle.color) {
                case 0 -> gc.setFill(Color.rgb(185, 8, 4));
                case 1 -> gc.setFill(Color.rgb(255, 30, 5));
                case 2 -> gc.setFill(Color.rgb(255, 100, 10));
                default -> gc.setFill(Color.rgb(255, 220, 70));
            }
            double size = Math.max(2.0, Math.round(particle.size * zoom));
            gc.fillRect(Math.round(x), Math.round(y), size, size);
        }

        gc.restore();
    }

    private void renderEmbers(GraphicsContext gc, Camera camera,
                              double zoom, double life) {
        gc.save();
        gc.setGlobalAlpha(life * 0.9);
        gc.setFill(Color.rgb(255, 45, 8));
        double size = Math.max(1.0, Math.round(1.7 * zoom));
        for (Ember ember : embers) {
            double x = camera.worldToScreenX(center.getX() + ember.x);
            double y = camera.worldToScreenY(center.getY() + ember.y);
            gc.fillRect(Math.round(x), Math.round(y), size, size);
        }

        gc.restore();
    }

    private void drawSquare(GraphicsContext gc, double x, double y, double size) {
        size = Math.max(1.0, Math.round(size));
        gc.fillRect(Math.round(x - size / 2.0), Math.round(y - size / 2.0), size, size);
    }

    @Override
    public boolean isFinished() {
        return remaining <= 0.0;
    }

    private static final class FireParticle {
        private double x;
        private double y;
        private double velocityX;
        private double velocityY;
        private final double size;
        private final int color;

        private FireParticle(double velocityX, double velocityY,
                             double size, int color) {
            this.velocityX = velocityX;
            this.velocityY = velocityY;
            this.size = size;
            this.color = color;
        }
    }

    private static final class Ember {
        private double x;
        private double y;
        private double velocityX;
        private double velocityY;

        private Ember(double velocityX, double velocityY) {
            this.velocityX = velocityX;
            this.velocityY = velocityY;
        }
    }
}