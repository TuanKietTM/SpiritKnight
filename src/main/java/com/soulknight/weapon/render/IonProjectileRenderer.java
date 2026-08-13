package com.soulknight.weapon.render;

import com.soulknight.engine.Camera;
import com.soulknight.weapon.Bullet;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Renderer ve dan Ion theo phong cach Pixel-Art.
 * Tu dong tinh toán 4 lop mau tu bullet.getColor() de ho tro moi loai mau dan (Player/Enemy/Boss).
 */
public final class IonProjectileRenderer implements ProjectileRenderer {

    private final double charge;

    public IonProjectileRenderer(double charge) {
        this.charge = clamp01(charge);
    }

    @Override
    public void render(Bullet bullet, GraphicsContext gc, Camera camera) {
        if (bullet == null || gc == null || camera == null) return;

        double x = camera.worldToScreenX(bullet.getPosition().getX());
        double y = camera.worldToScreenY(bullet.getPosition().getY());
        double zoom = camera.getZoom();

        // Orb rung nhe de tao cam giac plasma dang chay
        double pulse = 1.0 + Math.sin(bullet.getAge() * 24.0) * 0.06;
        double size = bullet.getRadius() * 1.3 * zoom * pulse;
        double pixel = Math.max(1.0, Math.round(2.0 * zoom));

        // Layout bang mau dong dua tren mau goc cua Bullet
        Color baseColor = (bullet.getColor() != null) ? bullet.getColor() : Color.web("#14E6C1");
        Color outerColor = baseColor.darker();
        Color mainColor = baseColor;
        Color innerColor = baseColor.brighter();
        Color coreColor = Color.WHITE.interpolate(baseColor, 0.2); // Loi trang phat sang

        gc.save();
        gc.setImageSmoothing(false);

        // 1. Ve vet duoi Ion phia sau (Motion Trail)
        renderTrail(bullet, gc, x, y, size, pixel, mainColor, outerColor);

        // 2. Ve 4 lop cau Ion Pixel-Art
        drawLayer(gc, x, y, size, pixel, outerColor);
        drawLayer(gc, x, y, size * 0.76, pixel, mainColor);
        drawLayer(gc, x, y, size * 0.48, pixel, innerColor);
        drawLayer(gc, x, y, size * (0.20 + charge * 0.10), pixel, coreColor);

        // 3. Ve cac tia dien xoay quanh (Orbiting Sparks)
        renderSparks(bullet, gc, x, y, size, pixel, mainColor, coreColor);

        gc.restore();
    }

    private void renderTrail(Bullet bullet, GraphicsContext gc, double x, double y, double size, double pixel, Color mainColor, Color outerColor) {
        double speed = bullet.getVelocity().length();
        if (speed <= 0.001) return;

        double dirX = bullet.getVelocity().getX() / speed;
        double dirY = bullet.getVelocity().getY() / speed;

        for (int i = 5; i >= 1; i--) {
            double distance = size * (0.35 + i * 0.22);
            double px = x - dirX * distance;
            double py = y - dirY * distance;

            double trailSize = Math.max(pixel, size * (0.25 - i * 0.035));

            gc.setGlobalAlpha(0.06 + (5 - i) * 0.08);
            gc.setFill(i <= 2 ? mainColor : outerColor);

            gc.fillRect(
                    snap(px - trailSize / 2.0, pixel),
                    snap(py - trailSize / 2.0, pixel),
                    Math.max(pixel, snap(trailSize, pixel)),
                    Math.max(pixel, snap(trailSize, pixel))
            );
        }

        gc.setGlobalAlpha(1.0);
    }

    private void renderSparks(Bullet bullet, GraphicsContext gc, double x, double y, double size, double pixel, Color mainColor, Color coreColor) {
        int count = 4 + (int) Math.round(charge * 4.0);

        for (int i = 0; i < count; i++) {
            double angle = bullet.getAge() * 8.0 + i * Math.PI * 2.0 / count;
            double orbit = size * (0.62 + 0.08 * Math.sin(bullet.getAge() * 14.0 + i));

            double px = x + Math.cos(angle) * orbit;
            double py = y + Math.sin(angle) * orbit;

            double sparkSize = pixel * (i % 2 == 0 ? 2.0 : 1.0);

            gc.setFill(i % 2 == 0 ? coreColor : mainColor);

            gc.fillRect(
                    snap(px - sparkSize / 2.0, pixel),
                    snap(py - sparkSize / 2.0, pixel),
                    sparkSize,
                    sparkSize
            );
        }
    }

    private void drawLayer(GraphicsContext gc, double x, double y, double size, double pixel, Color color) {
        double half = size / 2.0;

        gc.setFill(color);

        // Ve khoi Thap giac 8-bit Pixel-Art
        gc.fillRect(
                snap(x - half, pixel),
                snap(y - half * 0.68, pixel),
                Math.max(pixel, snap(size, pixel)),
                Math.max(pixel, snap(size * 0.68, pixel))
        );

        gc.fillRect(
                snap(x - half * 0.68, pixel),
                snap(y - half, pixel),
                Math.max(pixel, snap(size * 0.68, pixel)),
                Math.max(pixel, snap(size, pixel))
        );
    }

    private double snap(double value, double pixel) {
        return Math.round(value / pixel) * pixel;
    }

    private double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}