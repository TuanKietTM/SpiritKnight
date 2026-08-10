package com.soulknight.weapon.render;

import com.soulknight.engine.Camera;
import com.soulknight.weapon.Bullet;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Vien dan dac biet ion to dan khi tich nang luong
 */
public final class IonProjectileRenderer implements ProjectileRenderer {

    private static final Color OUTER_COLOR = Color.web("#006E62");
    private static final Color MAIN_COLOR = Color.web("#14E6C1");
    private static final Color INNER_COLOR = Color.web("#76FFE9");
    private static final Color CORE_COLOR = Color.web("#F5FFFD");

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

        // Orb rung nhe de tao cam giac plasma song.
        double pulse = 1.0 + Math.sin(bullet.getAge() * 22.0) * 0.055;
        double size = bullet.getRadius() * 2.0 * zoom * pulse;
        double pixel = Math.max(1.0, Math.round(2.0 * zoom));

        gc.save();
        gc.setImageSmoothing(false);

        renderTrail(bullet, gc, x, y, size, pixel);

        drawLayer(gc, x, y, size, pixel, OUTER_COLOR);
        drawLayer(gc, x, y, size * 0.76, pixel, MAIN_COLOR);
        drawLayer(gc, x, y, size * 0.46, pixel, INNER_COLOR);
        drawLayer(gc, x, y, size * (0.18 + charge * 0.08), pixel, CORE_COLOR);

        renderSparks(bullet, gc, x, y, size, pixel);

        gc.restore();
    }

    private void renderTrail(Bullet bullet, GraphicsContext gc, double x, double y, double size, double pixel) {
        double speed = bullet.getVelocity().length();
        if (speed <= 0.001) return;

        double dirX = bullet.getVelocity().getX() / speed;
        double dirY = bullet.getVelocity().getY() / speed;

        for (int i = 4; i >= 1; i--) {
            double distance = size * (0.45 + i * 0.25);

            double px = x - dirX * distance;
            double py = y - dirY * distance;

            double trailSize = Math.max(pixel, size * (0.20 - i * 0.025));

            gc.setGlobalAlpha(0.08 + (4 - i) * 0.07);
            gc.setFill(i <= 2 ? MAIN_COLOR : OUTER_COLOR);

            gc.fillRect(
                    snap(px - trailSize / 2.0, pixel),
                    snap(py - trailSize / 2.0, pixel),
                    Math.max(pixel, snap(trailSize, pixel)),
                    Math.max(pixel, snap(trailSize, pixel))
            );
        }

        gc.setGlobalAlpha(1.0);
    }

    private void renderSparks(Bullet bullet, GraphicsContext gc, double x, double y, double size, double pixel) {
        int count = 3 + (int) Math.round(charge * 4.0);

        for (int i = 0; i < count; i++) {
            double angle = bullet.getAge() * 6.0 + i * Math.PI * 2.0 / count;
            double orbit = size * (0.60 + 0.08 * Math.sin(bullet.getAge() * 13.0 + i));

            double px = x + Math.cos(angle) * orbit;
            double py = y + Math.sin(angle) * orbit;

            double sparkSize = pixel * (i % 2 == 0 ? 2.0 : 1.0);

            gc.setFill(i % 3 == 0 ? CORE_COLOR : MAIN_COLOR);

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

        // Hai khoi giao nhau tao hinh cau 8-bit.
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