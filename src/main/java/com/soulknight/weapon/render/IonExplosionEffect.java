package com.soulknight.weapon.render;

import com.soulknight.engine.Camera;
import com.soulknight.utils.Vector2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public final class IonExplosionEffect {

    private static final double DURATION = 0.42;

    private final Vector2D position;
    private final double radius;
    private final Color baseColor;

    private double age;
    private boolean active = true;

    // Constructor mac dinh xanh Cyan cho Player
    public IonExplosionEffect(Vector2D position, double radius) {
        this(position, radius, Color.web("#14E6C1"));
    }

    // Constructor truyen mau tuy chinh cho Quai / Boss
    public IonExplosionEffect(Vector2D position, double radius, Color baseColor) {
        this.position = position == null ? new Vector2D(0.0, 0.0) : position.copy();
        this.radius = Math.max(1.0, radius);
        this.baseColor = (baseColor != null) ? baseColor : Color.web("#14E6C1");
    }

    public void update(double deltaSeconds) {
        if (!active) return;
        age += Math.max(0.0, deltaSeconds);
        if (age >= DURATION) active = false;
    }

    public void render(GraphicsContext gc, Camera camera) {
        if (!active || gc == null || camera == null) return;

        double progress = Math.min(1.0, age / DURATION);

        double x = camera.worldToScreenX(position.getX());
        double y = camera.worldToScreenY(position.getY());
        double zoom = camera.getZoom();

        double maxSize = radius * 2.0 * zoom;
        double pixel = Math.max(2.0, Math.round(2.0 * zoom));

        Color darkColor = baseColor.darker();
        Color mainColor = baseColor;
        Color innerColor = baseColor.brighter();
        Color coreColor = Color.WHITE.interpolate(baseColor, 0.2);

        gc.save();
        gc.setImageSmoothing(false);

        // Flash dot ngot
        if (progress <= 0.14) {
            double local = progress / 0.14;
            gc.setGlobalAlpha(1.0 - local);
            drawPixelOrb(gc, x, y, maxSize * (0.12 + local * 0.38), pixel, coreColor);
        }

        // Plasma Core No
        double strength = (progress < 0.30) ? (progress / 0.30) : (1.0 - (progress - 0.30) / 0.70);
        strength = Math.max(0.0, Math.min(1.0, strength));
        double size = maxSize * (0.16 + strength * 0.52);

        gc.setGlobalAlpha(Math.min(1.0, strength * 1.6));
        drawPixelOrb(gc, x, y, size, pixel, darkColor);
        drawPixelOrb(gc, x, y, size * 0.82, pixel, baseColor);
        drawPixelOrb(gc, x, y, size * 0.62, pixel, mainColor);
        drawPixelOrb(gc, x, y, size * 0.34, pixel, innerColor);
        if (progress < 0.48) drawPixelOrb(gc, x, y, size * 0.14, pixel, coreColor);

        // Electric Sparks
        if (progress >= 0.06) {
            double local = (progress - 0.06) / 0.94;
            int count = 12;
            gc.setGlobalAlpha((1.0 - local) * 0.95);

            for (int i = 0; i < count; i++) {
                double angle = Math.PI * 2.0 * i / count + Math.sin(i * 1.73) * 0.18;
                double distance = maxSize * local * 0.48 * (0.72 + (i % 4) * 0.10);

                double px = x + Math.cos(angle) * distance;
                double py = y + Math.sin(angle) * distance;

                double sparkLength = Math.max(pixel, maxSize * (0.055 - local * 0.025));
                gc.setFill((i % 2 == 0) ? coreColor : innerColor);

                gc.fillRect(
                        snap(px - sparkLength / 2.0, pixel),
                        snap(py - pixel / 2.0, pixel),
                        snap(sparkLength, pixel),
                        pixel
                );
            }
        }

        gc.restore();
    }

    private void drawPixelOrb(GraphicsContext gc, double x, double y, double size, double pixel, Color color) {
        double half = size / 2.0;
        gc.setFill(color);
        gc.fillRect(snap(x - half, pixel), snap(y - half * 0.66, pixel), Math.max(pixel, snap(size, pixel)), Math.max(pixel, snap(size * 0.66, pixel)));
        gc.fillRect(snap(x - half * 0.66, pixel), snap(y - half, pixel), Math.max(pixel, snap(size * 0.68, pixel)), Math.max(pixel, snap(size, pixel)));
    }

    private double snap(double value, double pixel) {
        return Math.round(value / pixel) * pixel;
    }

    public boolean isActive() { return active; }
}