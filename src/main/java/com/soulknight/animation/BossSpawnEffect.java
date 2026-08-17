package com.soulknight.animation;

import com.soulknight.engine.Camera;
import com.soulknight.utils.Vector2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.util.Random;

/**
 * Hieu ung spawn boss bang render
 */
public final class BossSpawnEffect {

    private static final int PARTICLE_COUNT = 40;
    private static final int RUNE_COUNT = 8;

    private final Vector2D position;
    private final double delayDuration;
    private final double effectDuration;

    private final double[] particleAngle = new double[PARTICLE_COUNT];
    private final double[] particleRadius = new double[PARTICLE_COUNT];
    private final double[] particleSpeed = new double[PARTICLE_COUNT];
    private final double[] particleSize = new double[PARTICLE_COUNT];

    private double timer;
    private boolean finished;
    private final Random random = new Random();

    public BossSpawnEffect(Vector2D position, double delayDuration, double effectDuration) {
        this.position = position.copy();
        this.delayDuration = Math.max(0.0, delayDuration);
        this.effectDuration = Math.max(0.5, effectDuration);

        for (int i = 0; i < PARTICLE_COUNT; i++) {
            particleAngle[i] = random.nextDouble() * Math.PI * 2.0;
            particleRadius[i] = 20.0 + random.nextDouble() * 70.0;
            particleSpeed[i] = 1.0 + random.nextDouble() * 2.5;
            particleSize[i] = 2.0 + random.nextDouble() * 3.5;
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
        if (progress < 0.50) return 0.0;

        double appearProgress = (progress - 0.50) / 0.50;
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
        double screenY = snap(camera.worldToScreenY(position.getY()) + 15.0 * zoom, pixel);

        gc.save();
        renderMagicRuneCircle(gc, screenX, screenY, pixel, zoom, progress);
        renderAscendingParticles(gc, screenX, screenY, pixel, zoom, progress);
        renderEnergyBeam(gc, screenX, screenY, pixel, zoom, progress);
        renderBossLightning(gc, screenX, screenY, pixel, zoom, progress);
        renderImpactShockwave(gc, screenX, screenY, pixel, zoom, progress);

        gc.restore();
    }
    private void renderMagicRuneCircle(GraphicsContext gc, double screenX, double screenY,
                                       double pixel, double zoom, double progress) {
        double circleAlpha = progress < 0.8 ? Math.min(1.0, progress * 2.5) : (1.0 - progress) / 0.2;
        if (circleAlpha <= 0) return;

        double outerRadius = 85.0 * zoom * Math.min(1.0, progress * 2.0);
        double innerRadius = outerRadius * 0.65;
        gc.setLineWidth(Math.max(2.0, 3.0 * zoom));
        gc.setStroke(Color.rgb(235, 45, 80, circleAlpha * 0.85));
        gc.strokeOval(screenX - outerRadius, screenY - outerRadius * 0.45, outerRadius * 2, outerRadius * 0.9);
        gc.setStroke(Color.rgb(255, 180, 50, circleAlpha * 0.95));
        gc.strokeOval(screenX - innerRadius, screenY - innerRadius * 0.45, innerRadius * 2, innerRadius * 0.9);
        int runePoints = RUNE_COUNT;
        double angleOffset = progress * Math.PI * 6.0;
        gc.setFill(Color.rgb(255, 220, 100, circleAlpha));

        for (int i = 0; i < runePoints; i++) {
            double angle = angleOffset + (i * Math.PI * 2.0 / runePoints);
            double rx = screenX + Math.cos(angle) * outerRadius;
            double ry = screenY + Math.sin(angle) * outerRadius * 0.45;
            double size = Math.max(pixel, 4.0 * zoom);
            gc.fillRect(snap(rx - size / 2, pixel), snap(ry - size / 2, pixel), size, size);
        }
    }

    private void renderEnergyBeam(GraphicsContext gc, double screenX, double screenY,
                                  double pixel, double zoom, double progress) {
        if (progress < 0.25 || progress > 0.85) return;

        double beamProgress = (progress - 0.25) / 0.6;
        double beamWidth = (1.0 - Math.abs(beamProgress - 0.5) * 1.8) * 90.0 * zoom;
        beamWidth = Math.max(pixel * 4, beamWidth);

        double topY = 0;
        double alpha = Math.sin(beamProgress * Math.PI) * 0.9;
        gc.setFill(Color.rgb(180, 20, 90, alpha * 0.5));
        gc.fillRect(screenX - beamWidth / 2, topY, beamWidth, screenY - topY);
        double coreWidth = beamWidth * 0.45;
        gc.setFill(Color.rgb(255, 245, 200, alpha * 0.95));
        gc.fillRect(screenX - coreWidth / 2, topY, beamWidth * 0.45, screenY - topY);
    }

    private void renderAscendingParticles(GraphicsContext gc, double screenX, double screenY,
                                          double pixel, double zoom, double progress) {
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            double pProgress = (progress * particleSpeed[i] + (i / (double) PARTICLE_COUNT)) % 1.0;
            double angle = particleAngle[i] + progress * 2.0;
            double rad = particleRadius[i] * zoom * (0.3 + pProgress * 0.7);

            double x = screenX + Math.cos(angle) * rad;
            double y = screenY + Math.sin(angle) * rad * 0.4 - (pProgress * 90.0 * zoom);

            double alpha = (1.0 - pProgress) * Math.sin(progress * Math.PI) * 0.9;
            double size = Math.max(pixel, particleSize[i] * zoom);

            gc.setFill(i % 2 == 0
                    ? Color.rgb(255, 80, 50, alpha)
                    : Color.rgb(255, 210, 60, alpha));
            gc.fillRect(snap(x, pixel), snap(y, pixel), size, size);
        }
    }

    private void renderBossLightning(GraphicsContext gc, double screenX, double screenY,
                                     double pixel, double zoom, double progress) {
        if (progress < 0.3 || progress > 0.8) return;

        gc.setStroke(Color.rgb(255, 230, 150, 0.9));
        gc.setLineWidth(Math.max(2.0, 2.5 * zoom));

        for (int i = 0; i < 3; i++) {
            double side = (i == 0 ? -1.0 : i == 1 ? 1.0 : 0.2);
            double startX = screenX + side * (30.0 + i * 15.0) * zoom;
            double startY = screenY - 180.0 * zoom;

            double midX = startX + (random.nextDouble() - 0.5) * 30.0 * zoom;
            double midY = screenY - 90.0 * zoom;

            double endX = screenX + side * (15.0 + i * 10.0) * zoom;
            double endY = screenY;

            gc.strokeLine(startX, startY, midX, midY);
            gc.strokeLine(midX, midY, endX, endY);
        }
    }

    private void renderImpactShockwave(GraphicsContext gc, double screenX, double screenY,
                                       double pixel, double zoom, double progress) {
        if (progress < 0.55) return;

        double waveProgress = (progress - 0.55) / 0.45;
        double radius = waveProgress * 140.0 * zoom;
        double alpha = (1.0 - waveProgress) * 0.8;

        gc.setLineWidth(Math.max(pixel, 6.0 * zoom * (1.0 - waveProgress)));
        gc.setStroke(Color.rgb(255, 100, 30, alpha));
        gc.strokeOval(screenX - radius, screenY - radius * 0.45, radius * 2, radius * 0.9);
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