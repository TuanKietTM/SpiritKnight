package com.soulknight.animation;

import com.soulknight.engine.Camera;
import com.soulknight.utils.SoundManager;
import com.soulknight.utils.Vector2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.Random;

/**
 * Hieu ung Void Collapse khi quai chet.
 * Sprite mo dan, cac manh nang luong bi hut xuong khe nut va khe dong lai.
 */
public final class EnemyDeathEffect {

    private static final int PARTICLE_COUNT = 20;

    private final Vector2D position;
    private final double duration;
    private final double radius;
    private final double[] particleAngle = new double[PARTICLE_COUNT];
    private final double[] particleDistance = new double[PARTICLE_COUNT];
    private final double[] particleHeight = new double[PARTICLE_COUNT];
    private final double[] particleSize = new double[PARTICLE_COUNT];
    private final double[] particleDelay = new double[PARTICLE_COUNT];

    private double timer;
    private boolean finished;

    public EnemyDeathEffect(Vector2D position, double radius, double duration) {
        this.position = position.copy();
        this.radius = Math.max(8.0, radius);
        this.duration = Math.max(0.25, duration);

        Random random = new Random();
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            particleAngle[i] = random.nextDouble() * Math.PI * 2.0;
            particleDistance[i] = 10.0 + random.nextDouble() * (this.radius + 18.0);
            particleHeight[i] = 8.0 + random.nextDouble() * (this.radius + 20.0);
            particleSize[i] = 1.0 + random.nextInt(3);
            particleDelay[i] = random.nextDouble() * 0.28;
        }
        SoundManager.getInstance().playSFX("player_die");
    }

    public void update(double deltaSeconds) {
        if (finished) return;

        timer += Math.max(0.0, deltaSeconds);
        if (timer >= duration) {
            timer = duration;
            finished = true;
        }
    }

    public double getEntityAlpha() {
        double progress = getProgress();
        if (progress < 0.10) return 1.0;
        return 1.0 - smoothStep(clamp01((progress - 0.10) / 0.58));
    }

    public double getShakeX() {
        double progress = getProgress();
        if (progress > 0.48) return 0.0;
        return Math.sin(timer * 92.0) * (1.8 * (1.0 - progress));
    }

    public double getShakeY() {
        double progress = getProgress();
        if (progress > 0.48) return 0.0;
        return Math.cos(timer * 76.0) * (1.1 * (1.0 - progress));
    }

    public boolean isFinished() {
        return finished;
    }

    public void render(GraphicsContext gc, Camera camera) {
        double progress = getProgress();
        double zoom = camera.getZoom();
        double pixel = Math.max(2.0, Math.floor(2.0 * zoom));
        double screenX = snap(camera.worldToScreenX(position.getX()), pixel);
        double screenY = snap(camera.worldToScreenY(position.getY()) + 10.0 * zoom, pixel);

        gc.save();
        renderFlash(gc, screenX, screenY, pixel, zoom, progress);
        renderParticles(gc, screenX, screenY, pixel, zoom, progress);
        renderCollapseRift(gc, screenX, screenY, pixel, zoom, progress);
        renderFinalPulse(gc, screenX, screenY, pixel, zoom, progress);
        gc.restore();
    }

    private void renderFlash(GraphicsContext gc, double screenX, double screenY,
                             double pixel, double zoom, double progress) {
        if (progress > 0.13) return;

        double local = 1.0 - progress / 0.13;
        double size = (radius * 2.2 + 12.0) * zoom * (1.0 - local * 0.18);
        gc.setStroke(Color.rgb(210, 105, 255, 0.72 * local));
        gc.setLineWidth(pixel);
        gc.strokeOval(screenX - size / 2.0, screenY - size * 0.30,
                size, size * 0.60);
    }

    private void renderParticles(GraphicsContext gc, double screenX, double screenY,
                                 double pixel, double zoom, double progress) {
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            double local = clamp01((progress - particleDelay[i]) / (0.78 - particleDelay[i]));
            if (local <= 0.0 || local >= 1.0) continue;

            double eased = smoothStep(local);
            double startX = Math.cos(particleAngle[i]) * particleDistance[i] * zoom;
            double startY = -particleHeight[i] * zoom + Math.sin(particleAngle[i]) * radius * 0.35 * zoom;
            double swirl = Math.sin(local * Math.PI * 3.0 + particleAngle[i]) * 6.0 * zoom * (1.0 - local);
            double x = screenX + startX * (1.0 - eased) + swirl;
            double y = screenY + startY * (1.0 - eased);
            double size = Math.max(pixel, particleSize[i] * pixel * (1.0 - local * 0.45));
            double alpha = Math.sin(local * Math.PI) * 0.90;

            if (i % 4 == 0) {
                gc.setFill(Color.rgb(205, 85, 255, alpha));
            } else if (i % 2 == 0) {
                gc.setFill(Color.rgb(112, 28, 176, alpha));
            } else {
                gc.setFill(Color.rgb(30, 0, 44, alpha));
            }

            gc.fillRect(snap(x, pixel), snap(y, pixel), size, size);
        }
    }

    private void renderCollapseRift(GraphicsContext gc, double screenX, double screenY,
                                    double pixel, double zoom, double progress) {
        double openAmount;
        if (progress < 0.16) {
            openAmount = smoothStep(progress / 0.16);
        } else {
            openAmount = 1.0 - smoothStep(clamp01((progress - 0.60) / 0.32));
        }

        if (openAmount <= 0.01) return;

        double pulse = 0.94 + Math.sin(progress * Math.PI * 14.0) * 0.06;
        double width = (radius * 3.0 + 24.0) * zoom * openAmount * pulse;
        double height = Math.max(pixel, 9.0 * zoom * openAmount);

        gc.setFill(Color.rgb(142, 35, 204, 0.68));
        gc.fillOval(screenX - width / 2.0, screenY - height / 2.0, width, height);

        gc.setFill(Color.rgb(48, 3, 70, 0.94));
        gc.fillOval(screenX - width * 0.39, screenY - height * 0.34,
                width * 0.78, height * 0.68);

        gc.setFill(Color.rgb(7, 0, 12, 0.98));
        gc.fillOval(screenX - width * 0.25, screenY - height * 0.17,
                width * 0.50, Math.max(pixel, height * 0.34));

        double lineWidth = Math.max(pixel * 2.0, width * 0.18);
        gc.setFill(Color.rgb(215, 100, 255, 0.74));
        gc.fillRect(snap(screenX - lineWidth / 2.0, pixel),
                snap(screenY - pixel / 2.0, pixel), lineWidth, pixel);
    }

    private void renderFinalPulse(GraphicsContext gc, double screenX, double screenY,
                                  double pixel, double zoom, double progress) {
        if (progress < 0.72) return;

        double local = clamp01((progress - 0.72) / 0.28);
        double width = (radius * 2.2 + 20.0) * zoom * local;
        double height = width * 0.35;
        double alpha = (1.0 - local) * 0.52;

        gc.setStroke(Color.rgb(155, 48, 220, alpha));
        gc.setLineWidth(pixel);
        gc.strokeOval(screenX - width / 2.0, screenY - height / 2.0, width, height);
    }

    private double getProgress() {
        return clamp01(timer / duration);
    }

    private static double smoothStep(double value) {
        return value * value * (3.0 - 2.0 * value);
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static double snap(double value, double pixel) {
        return Math.floor(value / pixel) * pixel;
    }
}