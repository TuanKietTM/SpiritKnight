package com.soulknight.animation;

import com.soulknight.engine.Camera;
import com.soulknight.utils.Vector2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.Random;

/**
 * Hieu ung Void Rift dung rieng cho quai.
 * Khe nut mo tren mat dat, hat toi bi hut vao tam va quai hien dan len.
 */
public final class EnemySpawnEffect {

    private static final int PARTICLE_COUNT = 18;

    private final Vector2D position;
    private final double delayDuration;
    private final double effectDuration;
    private final double[] particleAngle = new double[PARTICLE_COUNT];
    private final double[] particleRadius = new double[PARTICLE_COUNT];
    private final double[] particleSize = new double[PARTICLE_COUNT];
    private final double[] particlePhase = new double[PARTICLE_COUNT];

    private double timer;
    private boolean finished;

    public EnemySpawnEffect(Vector2D position, double delayDuration, double effectDuration) {
        this.position = position.copy();
        this.delayDuration = Math.max(0.0, delayDuration);
        this.effectDuration = Math.max(0.1, effectDuration);

        Random random = new Random();
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            particleAngle[i] = random.nextDouble() * Math.PI * 2.0;
            particleRadius[i] = 26.0 + random.nextDouble() * 34.0;
            particleSize[i] = 1.0 + random.nextInt(3);
            particlePhase[i] = random.nextDouble();
        }
    }

    public void update(double deltaSeconds) {
        if (finished) return;

        timer += Math.max(0.0, deltaSeconds);
        if (timer >= delayDuration + effectDuration) {
            timer = delayDuration + effectDuration;
            finished = true;
        }
    }

    public double getEntityAlpha() {
        if (timer < delayDuration) return 0.0;

        double progress = getProgress();
        if (progress < 0.42) return 0.0;

        double appearProgress = (progress - 0.42) / 0.43;
        return smoothStep(clamp01(appearProgress));
    }

    public boolean blocksEnemyLogic() {
        return !finished;
    }

    public boolean isSpawning() {
        return !finished;
    }

    public boolean isFinished() {
        return finished;
    }

    public void render(GraphicsContext gc, Camera camera) {
        if (finished || timer < delayDuration) return;

        double progress = getProgress();
        double zoom = camera.getZoom();
        double pixel = Math.max(2.0, Math.floor(2.0 * zoom));
        double screenX = snap(camera.worldToScreenX(position.getX()), pixel);
        double screenY = snap(camera.worldToScreenY(position.getY()) + 10.0 * zoom, pixel);

        double openAmount = progress < 0.28
                ? smoothStep(progress / 0.28)
                : 1.0 - smoothStep(clamp01((progress - 0.78) / 0.22));

        double pulse = 0.88 + Math.sin(progress * Math.PI * 12.0) * 0.08;
        double riftWidth = 58.0 * zoom * openAmount * pulse;
        double riftHeight = Math.max(pixel, 10.0 * zoom * openAmount);

        gc.save();

        renderIncomingParticles(gc, screenX, screenY, pixel, zoom, progress, openAmount);
        renderDarkMist(gc, screenX, screenY, pixel, zoom, progress, openAmount);
        renderRift(gc, screenX, screenY, pixel, riftWidth, riftHeight, progress);
        renderVoidLightning(gc, screenX, screenY, pixel, zoom, progress, openAmount);

        gc.restore();
    }

    private void renderIncomingParticles(GraphicsContext gc, double screenX, double screenY,
                                         double pixel, double zoom, double progress, double openAmount) {
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            double local = (progress * 1.8 + particlePhase[i]) % 1.0;
            double radius = particleRadius[i] * zoom * (1.0 - local);
            double angle = particleAngle[i] + progress * (i % 2 == 0 ? 2.8 : -2.3);
            double x = screenX + Math.cos(angle) * radius;
            double y = screenY + Math.sin(angle) * radius * 0.42 - local * 8.0 * zoom;
            double size = Math.max(pixel, particleSize[i] * pixel);
            double alpha = Math.min(0.8, (0.2 + local * 0.7) * openAmount);

            gc.setFill(i % 3 == 0
                    ? Color.rgb(188, 70, 255, alpha)
                    : Color.rgb(72, 8, 105, alpha));
            gc.fillRect(snap(x, pixel), snap(y, pixel), size, size);
        }
    }

    private void renderDarkMist(GraphicsContext gc, double screenX, double screenY,
                                double pixel, double zoom, double progress, double openAmount) {
        int mistCount = 8;
        for (int i = 0; i < mistCount; i++) {
            double phase = (progress * 1.4 + i / (double) mistCount) % 1.0;
            double side = (i % 2 == 0 ? -1.0 : 1.0);
            double x = screenX + side * (6.0 + i * 2.2) * zoom * (1.0 - phase);
            double y = screenY - phase * 34.0 * zoom;
            double size = Math.max(pixel, (2.0 + (i % 3)) * pixel);
            double alpha = (1.0 - phase) * 0.42 * openAmount;

            gc.setFill(Color.rgb(18, 0, 28, alpha));
            gc.fillRect(snap(x, pixel), snap(y, pixel), size, size);
        }
    }

    private void renderRift(GraphicsContext gc, double screenX, double screenY, double pixel,
                            double riftWidth, double riftHeight, double progress) {
        if (riftWidth < pixel * 2.0) return;

        double outerX = snap(screenX - riftWidth / 2.0, pixel);
        double outerY = snap(screenY - riftHeight / 2.0, pixel);

        gc.setFill(Color.rgb(126, 24, 174, 0.72));
        gc.fillOval(outerX, outerY, riftWidth, riftHeight);

        double middleWidth = riftWidth * 0.78;
        double middleHeight = Math.max(pixel, riftHeight * 0.68);
        gc.setFill(Color.rgb(45, 2, 66, 0.95));
        gc.fillOval(screenX - middleWidth / 2.0, screenY - middleHeight / 2.0,
                middleWidth, middleHeight);

        double coreWidth = riftWidth * (0.48 + Math.sin(progress * 25.0) * 0.04);
        double coreHeight = Math.max(pixel, riftHeight * 0.34);
        gc.setFill(Color.rgb(8, 0, 14, 0.98));
        gc.fillOval(screenX - coreWidth / 2.0, screenY - coreHeight / 2.0,
                coreWidth, coreHeight);

        double highlightWidth = Math.max(pixel * 2.0, riftWidth * 0.22);
        gc.setFill(Color.rgb(204, 80, 255, 0.7));
        gc.fillRect(snap(screenX - highlightWidth / 2.0, pixel),
                snap(screenY - pixel / 2.0, pixel), highlightWidth, pixel);
    }

    private void renderVoidLightning(GraphicsContext gc, double screenX, double screenY,
                                     double pixel, double zoom, double progress, double openAmount) {
        if (progress < 0.16 || progress > 0.76 || openAmount <= 0.05) return;

        gc.setStroke(Color.rgb(158, 45, 224, 0.72));
        gc.setLineWidth(pixel);

        for (int branch = 0; branch < 3; branch++) {
            double side = branch == 0 ? -1.0 : branch == 1 ? 1.0 : 0.35;
            double startX = screenX + side * (13.0 + branch * 3.0) * zoom;
            double startY = screenY - 2.0 * zoom;
            double midX = startX - side * 7.0 * zoom;
            double midY = startY - (9.0 + branch * 3.0) * zoom;
            double endX = midX + side * 5.0 * zoom;
            double endY = midY - (7.0 + branch * 2.0) * zoom;

            gc.strokeLine(snap(startX, pixel), snap(startY, pixel), snap(midX, pixel), snap(midY, pixel));
            gc.strokeLine(snap(midX, pixel), snap(midY, pixel), snap(endX, pixel), snap(endY, pixel));
        }
    }

    private double getProgress() {
        return clamp01((timer - delayDuration) / effectDuration);
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