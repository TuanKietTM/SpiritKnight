package com.soulknight.weapon.render;

import com.soulknight.engine.Camera;
import com.soulknight.utils.Vector2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Hieu ung no cua dan ion
 * duoc goi trong gameworld cho vao list explosion
 */
public final class IonExplosionEffect {

    private static final double DURATION = 0.46;

    private static final Color DARK_COLOR = Color.web("#005C52");
    private static final Color OUTER_COLOR = Color.web("#008C78");
    private static final Color MAIN_COLOR = Color.web("#14E6C1");
    private static final Color INNER_COLOR = Color.web("#76FFE9");
    private static final Color CORE_COLOR = Color.web("#F5FFFD");

    private final Vector2D position;
    private final double radius;

    private double age;
    private boolean active = true;

    public IonExplosionEffect(Vector2D position, double radius) {
        this.position = position == null ? new Vector2D(0.0, 0.0) : position.copy();
        this.radius = Math.max(1.0, radius);
    }

    public void update(double deltaSeconds) {
        if (!active) return;

        age += Math.max(0.0, deltaSeconds);

        if (age >= DURATION) {
            active = false;
        }
    }

    public void render(GraphicsContext gc, Camera camera) {
        if (!active || gc == null || camera == null) return;

        double progress = Math.min(1.0, age / DURATION);

        double x = camera.worldToScreenX(position.getX());
        double y = camera.worldToScreenY(position.getY());
        double zoom = camera.getZoom();

        double maxSize = radius * 2.0 * zoom;
        double pixel = Math.max(2.0, Math.round(2.0 * zoom));

        gc.save();
        gc.setImageSmoothing(false);

        renderFlash(gc, x, y, maxSize, pixel, progress);
        renderEnergyCore(gc, x, y, maxSize, pixel, progress);
        renderShockwave(gc, x, y, maxSize, pixel, progress);
        renderElectricSparks(gc, x, y, maxSize, pixel, progress);

        gc.restore();
    }

    // Chớp trắng cực ngắn tại thời điểm Ion Orb va chạm.
    private void renderFlash(GraphicsContext gc, double x, double y, double maxSize, double pixel, double progress) {
        if (progress > 0.14) return;

        double local = progress / 0.14;
        double size = maxSize * (0.12 + local * 0.38);

        gc.setGlobalAlpha(1.0 - local);
        drawPixelOrb(gc, x, y, size, pixel, CORE_COLOR);
        gc.setGlobalAlpha(1.0);
    }

    // Quả cầu plasma phình rất nhanh rồi co và tan đi.
    private void renderEnergyCore(GraphicsContext gc, double x, double y, double maxSize, double pixel, double progress) {
        double strength;

        if (progress < 0.30) {
            strength = progress / 0.30;
        } else {
            strength = 1.0 - (progress - 0.30) / 0.70;
        }

        strength = clamp01(strength);

        double size = maxSize * (0.16 + strength * 0.52);
        double alpha = Math.min(1.0, strength * 1.6);

        gc.setGlobalAlpha(alpha);

        drawPixelOrb(gc, x, y, size, pixel, DARK_COLOR);
        drawPixelOrb(gc, x, y, size * 0.82, pixel, OUTER_COLOR);
        drawPixelOrb(gc, x, y, size * 0.62, pixel, MAIN_COLOR);
        drawPixelOrb(gc, x, y, size * 0.34, pixel, INNER_COLOR);

        if (progress < 0.48) {
            drawPixelOrb(gc, x, y, size * 0.14, pixel, CORE_COLOR);
        }

        gc.setGlobalAlpha(1.0);
    }

    // Song xung kích Ion mở rộng ra ngoài theo phong cách pixel.
    private void renderShockwave(GraphicsContext gc, double x, double y, double maxSize, double pixel, double progress) {
        if (progress < 0.08 || progress > 0.88) return;

        double local = (progress - 0.08) / 0.80;
        double distance = maxSize * (0.12 + local * 0.42);
        double thickness = Math.max(pixel, pixel * (2.2 - local));

        gc.setGlobalAlpha((1.0 - local) * 0.82);
        gc.setFill(local < 0.45 ? INNER_COLOR : MAIN_COLOR);

        // Bốn cạnh rời nhau thay vì strokeOval để giữ chất pixel-art.
        double half = distance;

        gc.fillRect(
                snap(x - half, pixel),
                snap(y - half, pixel),
                snap(distance * 2.0, pixel),
                thickness
        );

        gc.fillRect(
                snap(x - half, pixel),
                snap(y + half - thickness, pixel),
                snap(distance * 2.0, pixel),
                thickness
        );

        gc.fillRect(
                snap(x - half, pixel),
                snap(y - half, pixel),
                thickness,
                snap(distance * 2.0, pixel)
        );

        gc.fillRect(
                snap(x + half - thickness, pixel),
                snap(y - half, pixel),
                thickness,
                snap(distance * 2.0, pixel)
        );

        gc.setGlobalAlpha(1.0);
    }

    // Các tia năng lượng bắn ra theo nhiều hướng rồi tan dần.
    private void renderElectricSparks(GraphicsContext gc, double x, double y, double maxSize, double pixel, double progress) {
        if (progress < 0.06) return;

        double local = clamp01((progress - 0.06) / 0.94);
        int count = 16;

        gc.setGlobalAlpha((1.0 - local) * 0.95);

        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2.0 * i / count + Math.sin(i * 1.73) * 0.18;
            double variation = 0.72 + (i % 4) * 0.10;
            double distance = maxSize * local * 0.48 * variation;

            double px = x + Math.cos(angle) * distance;
            double py = y + Math.sin(angle) * distance;

            double sparkLength = Math.max(pixel, maxSize * (0.055 - local * 0.025));
            double sparkWidth = pixel;

            Color sparkColor;

            if (i % 4 == 0) {
                sparkColor = CORE_COLOR;
            } else if (i % 2 == 0) {
                sparkColor = INNER_COLOR;
            } else {
                sparkColor = MAIN_COLOR;
            }

            gc.setFill(sparkColor);

            // Tia ngang hoặc dọc tùy hướng chính.
            if (Math.abs(Math.cos(angle)) >= Math.abs(Math.sin(angle))) {
                gc.fillRect(
                        snap(px - sparkLength / 2.0, pixel),
                        snap(py - sparkWidth / 2.0, pixel),
                        snap(sparkLength, pixel),
                        sparkWidth
                );
            } else {
                gc.fillRect(
                        snap(px - sparkWidth / 2.0, pixel),
                        snap(py - sparkLength / 2.0, pixel),
                        sparkWidth,
                        snap(sparkLength, pixel)
                );
            }

            // Một pixel phụ tạo hình tia điện gãy.
            if (i % 3 == 0) {
                double branchX = px + Math.cos(angle + Math.PI / 2.0) * pixel * 2.0;
                double branchY = py + Math.sin(angle + Math.PI / 2.0) * pixel * 2.0;

                gc.fillRect(
                        snap(branchX - pixel / 2.0, pixel),
                        snap(branchY - pixel / 2.0, pixel),
                        pixel,
                        pixel
                );
            }
        }

        gc.setGlobalAlpha(1.0);
    }

    private void drawPixelOrb(GraphicsContext gc, double x, double y, double size, double pixel, Color color) {
        double half = size / 2.0;

        gc.setFill(color);

        gc.fillRect(
                snap(x - half, pixel),
                snap(y - half * 0.66, pixel),
                Math.max(pixel, snap(size, pixel)),
                Math.max(pixel, snap(size * 0.66, pixel))
        );

        gc.fillRect(
                snap(x - half * 0.66, pixel),
                snap(y - half, pixel),
                Math.max(pixel, snap(size * 0.66, pixel)),
                Math.max(pixel, snap(size, pixel))
        );
    }

    private double snap(double value, double pixel) {
        return Math.round(value / pixel) * pixel;
    }

    private double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    public boolean isActive() {
        return active;
    }
}