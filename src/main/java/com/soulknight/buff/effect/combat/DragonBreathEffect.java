package com.soulknight.buff.effect.combat;

import com.soulknight.engine.Camera;
import com.soulknight.utils.Vector2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.BlendMode;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class DragonBreathEffect implements CombatEffect {

    private static final double DURATION = 0.68;
    private static final int FLAME_COUNT = 70;
    private static final int EMBER_COUNT = 28;
    private static final double BASE_PIXEL_SIZE = 2.0;

    private final Vector2D origin;
    private final double dirX;
    private final double dirY;
    private final double perpendicularX;
    private final double perpendicularY;
    private final double range;
    private final double halfAngle;

    private final List<FlameParticle> flames = new ArrayList<>();
    private final List<EmberParticle> embers = new ArrayList<>();

    private double elapsed;
    private double remaining = DURATION;

    public DragonBreathEffect(Vector2D origin, double dirX, double dirY,
                              double range, double halfAngle) {
        this.origin = origin.copy();

        double length = Math.hypot(dirX, dirY);
        if (length <= 0.001) {
            this.dirX = 1.0;
            this.dirY = 0.0;
        } else {
            this.dirX = dirX / length;
            this.dirY = dirY / length;
        }

        this.perpendicularX = -this.dirY;
        this.perpendicularY = this.dirX;
        this.range = Math.max(1.0, range);
        this.halfAngle = Math.max(Math.toRadians(5.0), halfAngle);

        createParticles();
    }

    private void createParticles() {
        Random random = new Random();
        double maxSpread = Math.tan(halfAngle) * range;

        for (int i = 0; i < FLAME_COUNT; i++) {
            double depth = 0.06 + random.nextDouble() * 0.94;
            double currentSpread = maxSpread * depth;
            double side = (random.nextDouble() * 2.0 - 1.0) * currentSpread;
            double travelSpeed = range * (0.75 + random.nextDouble() * 0.55);
            double delay = random.nextDouble() * 0.13;
            double size = 1.5 + random.nextDouble() * 2.5;
            int colorVariant = random.nextInt(4);

            flames.add(new FlameParticle(
                    depth, side, travelSpeed, delay, size, colorVariant
            ));
        }

        for (int i = 0; i < EMBER_COUNT; i++) {
            double depth = 0.12 + random.nextDouble() * 0.88;
            double spread = maxSpread * depth * (0.35 + random.nextDouble() * 0.8);
            double sign = random.nextBoolean() ? 1.0 : -1.0;

            embers.add(new EmberParticle(
                    depth,
                    spread * sign,
                    range * (0.95 + random.nextDouble() * 0.7),
                    random.nextDouble() * 0.18
            ));
        }
    }

    @Override
    public void update(double deltaSeconds) {
        if (deltaSeconds <= 0.0 || isFinished()) {
            return;
        }

        elapsed += deltaSeconds;
        remaining = Math.max(0.0, remaining - deltaSeconds);
    }

    @Override
    public void render(GraphicsContext gc, Camera camera) {
        if (gc == null || camera == null || isFinished()) {
            return;
        }

        double zoom = camera.getZoom();

        gc.save();
        gc.setImageSmoothing(false);
        gc.setGlobalBlendMode(BlendMode.ADD);

        for (FlameParticle flame : flames) {
            renderFlame(gc, camera, zoom, flame);
        }

        for (EmberParticle ember : embers) {
            renderEmber(gc, camera, zoom, ember);
        }

        gc.restore();
    }

    private void renderFlame(GraphicsContext gc, Camera camera,
                             double zoom, FlameParticle flame) {
        double localTime = elapsed - flame.delay;

        if (localTime <= 0.0) {
            return;
        }

        double targetTravel = flame.depth * range;
        double travel = Math.min(targetTravel, localTime * flame.travelSpeed);

        if (travel <= 0.0) {
            return;
        }

        double travelRatio = Math.min(1.0, travel / Math.max(1.0, targetTravel));
        double side = flame.side * travelRatio;

        double worldX = origin.getX() + dirX * travel + perpendicularX * side;
        double worldY = origin.getY() + dirY * travel + perpendicularY * side;

        double screenX = camera.worldToScreenX(worldX);
        double screenY = camera.worldToScreenY(worldY);

        double arrivalTime = targetTravel / flame.travelSpeed;
        double ageAfterArrival = Math.max(0.0, localTime - arrivalTime);
        double alpha = 1.0 - Math.min(1.0, ageAfterArrival / 0.24);

        if (alpha <= 0.0) {
            return;
        }

        gc.setGlobalAlpha(alpha * 0.95);

        switch (flame.colorVariant) {
            case 0 -> gc.setFill(Color.rgb(175, 10, 5));
            case 1 -> gc.setFill(Color.rgb(255, 35, 5));
            case 2 -> gc.setFill(Color.rgb(255, 105, 10));
            default -> gc.setFill(Color.rgb(255, 225, 90));
        }

        double pixel = Math.max(
                1.0,
                Math.round(BASE_PIXEL_SIZE * flame.size * 0.55 * zoom)
        );

        drawFlamePattern(gc, screenX, screenY, pixel);
    }

    private void drawFlamePattern(GraphicsContext gc, double x, double y, double pixel) {
        double px = Math.round(x);
        double py = Math.round(y);

        gc.fillRect(px, py, pixel, pixel);
        gc.fillRect(px - pixel, py + pixel, pixel, pixel);
        gc.fillRect(px, py + pixel, pixel, pixel);
        gc.fillRect(px + pixel, py + pixel, pixel, pixel);
        gc.fillRect(px, py + pixel * 2.0, pixel, pixel);
    }

    private void renderEmber(GraphicsContext gc, Camera camera,
                             double zoom, EmberParticle ember) {
        double localTime = elapsed - ember.delay;

        if (localTime <= 0.0) {
            return;
        }

        double maxTravel = ember.depth * range;
        double travel = Math.min(maxTravel, localTime * ember.travelSpeed);
        double ratio = Math.min(1.0, travel / Math.max(1.0, maxTravel));
        double side = ember.side * ratio * 1.3;

        double worldX = origin.getX() + dirX * travel + perpendicularX * side;
        double worldY = origin.getY() + dirY * travel + perpendicularY * side;

        double x = camera.worldToScreenX(worldX);
        double y = camera.worldToScreenY(worldY);

        double alpha = Math.max(0.0, 1.0 - elapsed / DURATION);

        gc.setGlobalAlpha(alpha * 0.9);
        gc.setFill(Color.rgb(255, 65, 15));

        double size = Math.max(1.0, Math.round(1.5 * zoom));

        gc.fillRect(Math.round(x), Math.round(y), size, size);
    }

    @Override
    public boolean isFinished() {
        return remaining <= 0.0;
    }

    private static final class FlameParticle {
        private final double depth;
        private final double side;
        private final double travelSpeed;
        private final double delay;
        private final double size;
        private final int colorVariant;

        private FlameParticle(double depth, double side, double travelSpeed,
                              double delay, double size, int colorVariant) {
            this.depth = depth;
            this.side = side;
            this.travelSpeed = travelSpeed;
            this.delay = delay;
            this.size = size;
            this.colorVariant = colorVariant;
        }
    }

    private static final class EmberParticle {
        private final double depth;
        private final double side;
        private final double travelSpeed;
        private final double delay;

        private EmberParticle(double depth, double side,
                              double travelSpeed, double delay) {
            this.depth = depth;
            this.side = side;
            this.travelSpeed = travelSpeed;
            this.delay = delay;
        }
    }
}