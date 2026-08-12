package com.soulknight.animation;

import com.soulknight.engine.Camera;
import com.soulknight.utils.Vector2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.BlendMode;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class RestHealEffect {

    private static final double DURATION = 1.35;
    private static final int PARTICLE_COUNT = 18;

    private final Vector2D position;
    private final List<HealParticle> particles = new ArrayList<>();
    private final Random random = new Random();

    private double age;

    public RestHealEffect(Vector2D position) {
        this.position = position.copy();
        createParticles();
    }

    private void createParticles() {
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double radius = 8.0 + random.nextDouble() * 18.0;

            particles.add(new HealParticle(
                    angle,
                    radius,
                    16.0 + random.nextDouble() * 18.0,
                    random.nextDouble() * 0.35,
                    1.5 + random.nextDouble() * 2.5
            ));
        }
    }

    public void update(double deltaSeconds) {
        if (deltaSeconds <= 0.0) return;
        age += deltaSeconds;
    }

    public void render(GraphicsContext gc, Camera camera) {
        if (gc == null || camera == null || isFinished()) return;

        double x = camera.worldToScreenX(position.getX());
        double y = camera.worldToScreenY(position.getY());
        double zoom = camera.getZoom();
        double progress = Math.min(1.0, age / DURATION);

        gc.save();
        gc.setGlobalBlendMode(BlendMode.ADD);

        renderMagicCircle(gc, x, y, zoom, progress);
        renderParticles(gc, x, y, zoom, progress);
        renderEnergyColumn(gc, x, y, zoom, progress);
        renderFinalPulse(gc, x, y, zoom, progress);

        gc.restore();
    }

    private void renderMagicCircle(GraphicsContext gc, double x, double y, double zoom, double progress) {
        double appear = Math.min(1.0, progress * 5.0);
        double fade = 1.0 - Math.max(0.0, (progress - 0.65) / 0.35);
        double alpha = appear * fade;

        double radius = (13.0 + progress * 9.0) * zoom;

        gc.setGlobalAlpha(alpha * 0.75);
        gc.setStroke(Color.AQUAMARINE);
        gc.setLineWidth(Math.max(1.0, zoom));

        gc.strokeOval(x - radius, y - radius * 0.38, radius * 2.0, radius * 0.76);

        double inner = radius * 0.65;

        gc.setGlobalAlpha(alpha * 0.55);
        gc.strokeOval(x - inner, y - inner * 0.38, inner * 2.0, inner * 0.76);

        // Bon rune nho quay quanh vong heal.
        for (int i = 0; i < 4; i++) {
            double angle = age * 2.5 + Math.PI * 0.5 * i;
            double runeX = x + Math.cos(angle) * radius;
            double runeY = y + Math.sin(angle) * radius * 0.38;
            double size = 2.2 * zoom;

            gc.setGlobalAlpha(alpha);
            gc.setFill(Color.LIGHTGOLDENRODYELLOW);
            gc.fillRect(
                    Math.floor(runeX - size * 0.5),
                    Math.floor(runeY - size * 0.5),
                    Math.ceil(size),
                    Math.ceil(size)
            );
        }
    }

    private void renderParticles(GraphicsContext gc, double x, double y, double zoom, double progress) {
        for (int i = 0; i < particles.size(); i++) {
            HealParticle particle = particles.get(i);

            double localProgress = (progress - particle.delay) / Math.max(0.01, 1.0 - particle.delay);
            if (localProgress < 0.0 || localProgress > 1.0) continue;

            double angle = particle.angle + localProgress * 2.4;
            double radius = particle.radius * (1.0 - localProgress * 0.65);

            double px = x + Math.cos(angle) * radius * zoom;
            double py = y + Math.sin(angle) * radius * 0.45 * zoom - particle.height * localProgress * zoom;

            double alpha = Math.sin(localProgress * Math.PI);
            double size = particle.size * zoom;

            gc.setGlobalAlpha(alpha * 0.9);
            gc.setFill(i % 3 == 0 ? Color.LIGHTGOLDENRODYELLOW : Color.AQUAMARINE);

            // Hinh vuong de giu chat pixel-art.
            gc.fillRect(
                    Math.floor(px - size * 0.5),
                    Math.floor(py - size * 0.5),
                    Math.ceil(size),
                    Math.ceil(size)
            );
        }
    }

    private void renderEnergyColumn(GraphicsContext gc, double x, double y, double zoom, double progress) {
        if (progress < 0.15 || progress > 0.82) return;

        double local = (progress - 0.15) / 0.67;
        double alpha = Math.sin(local * Math.PI) * 0.20;

        double width = (10.0 + Math.sin(age * 9.0) * 2.0) * zoom;
        double height = 38.0 * zoom;

        gc.setGlobalAlpha(alpha);
        gc.setFill(Color.AQUAMARINE);

        gc.fillRect(
                x - width * 0.5,
                y - height,
                width,
                height
        );
    }

    private void renderFinalPulse(GraphicsContext gc, double x, double y, double zoom, double progress) {
        if (progress < 0.62) return;

        double local = (progress - 0.62) / 0.38;
        double radius = (8.0 + local * 31.0) * zoom;
        double alpha = (1.0 - local) * 0.7;

        gc.setGlobalAlpha(alpha);
        gc.setStroke(Color.LIGHTCYAN);
        gc.setLineWidth(Math.max(1.0, 1.5 * zoom));

        gc.strokeOval(
                x - radius,
                y - radius,
                radius * 2.0,
                radius * 2.0
        );
    }

    public boolean isFinished() {
        return age >= DURATION;
    }

    private static final class HealParticle {

        private final double angle;
        private final double radius;
        private final double height;
        private final double delay;
        private final double size;

        private HealParticle(double angle, double radius, double height, double delay, double size) {
            this.angle = angle;
            this.radius = radius;
            this.height = height;
            this.delay = delay;
            this.size = size;
        }
    }
}