package com.soulknight.weapon.render;

import com.soulknight.engine.Camera;
import com.soulknight.entity.Player;
import com.soulknight.weapon.Gun;
import com.soulknight.weapon.IonElectromagneticGun;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public final class IonChargeRenderer {

    private static final Color OUTER_COLOR = Color.web("#006E62");
    private static final Color MAIN_COLOR = Color.web("#14E6C1");
    private static final Color INNER_COLOR = Color.web("#76FFE9");
    private static final Color CORE_COLOR = Color.web("#F5FFFD");

    private IonChargeRenderer() {
    }

    // Ve qua cau Ion dang nap ngay tai dau nong sung.
    public static void render(Player player, IonElectromagneticGun ionGun, GraphicsContext gc, Camera camera,
                              double aimAngle) {
        if (player == null || ionGun == null || gc == null || camera == null) return;
        if (!ionGun.isCharging() || player.getPosition() == null) return;

        double charge = ionGun.getChargeRatio();
        double zoom = camera.getZoom();

        double muzzleWorldX = player.getPosition().getX() + Math.cos(aimAngle) * Gun.MUZZLE_DISTANCE;
        double muzzleWorldY = player.getPosition().getY() + Math.sin(aimAngle) * Gun.MUZZLE_DISTANCE;

        double screenX = camera.worldToScreenX(muzzleWorldX);
        double screenY = camera.worldToScreenY(muzzleWorldY);

        // Cung radius voi projectile de nha tay khong bi nhay kich thuoc.
        double radius = ionGun.getCurrentOrbRadius();
        double size = radius * 2.0 * zoom;

        double pulse = 1.0 + Math.sin(System.nanoTime() * 0.000000018) * (0.03 + charge * 0.05);
        size *= pulse;

        double pixel = Math.max(1.0, Math.round(2.0 * zoom));

        gc.save();
        gc.setImageSmoothing(false);

        renderChargeParticles(gc, screenX, screenY, size, pixel, charge);

        drawLayer(gc, screenX, screenY, size, pixel, OUTER_COLOR);
        drawLayer(gc, screenX, screenY, size * 0.76, pixel, MAIN_COLOR);
        drawLayer(gc, screenX, screenY, size * 0.46, pixel, INNER_COLOR);
        drawLayer(gc, screenX, screenY, size * (0.18 + charge * 0.08), pixel, CORE_COLOR);

        gc.restore();
    }

    private static void renderChargeParticles(GraphicsContext gc, double x, double y,
                                              double orbSize, double pixel, double charge) {
        int count = 3 + (int) Math.round(charge * 7.0);
        double time = System.nanoTime() * 0.0000000025;

        for (int i = 0; i < count; i++) {
            double phase = (time * (1.4 + i * 0.04) + i * 0.618) % 1.0;
            double distance = orbSize * (2.1 - phase * 1.55);
            double angle = i * 2.399 + time * 1.6;

            double px = x + Math.cos(angle) * distance;
            double py = y + Math.sin(angle) * distance;

            double particleSize = pixel * (phase > 0.70 ? 2.0 : 1.0);

            gc.setGlobalAlpha(0.35 + phase * 0.65);
            gc.setFill(i % 3 == 0 ? CORE_COLOR : MAIN_COLOR);

            gc.fillRect(
                    snap(px - particleSize / 2.0, pixel),
                    snap(py - particleSize / 2.0, pixel),
                    particleSize,
                    particleSize
            );
        }

        gc.setGlobalAlpha(1.0);
    }

    private static void drawLayer(GraphicsContext gc, double x, double y, double size,
                                  double pixel, Color color) {
        double half = size / 2.0;

        gc.setFill(color);

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

    private static double snap(double value, double pixel) {
        return Math.round(value / pixel) * pixel;
    }
}