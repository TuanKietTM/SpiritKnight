package com.soulknight.buff.effect.combat;

import com.soulknight.engine.Camera;
import com.soulknight.utils.Vector2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Tia set tim pixel-art. day set lan truyen
 * points: 0 = Player, 1 = muc tieu chinh, 2.. = cac muc tieu chain.
 */
public final class PurpleChainLightningEffect implements CombatEffect {

    private static final double DURATION = 0.20;
    private static final double REBUILD_INTERVAL = 0.035;
    private static final double POINT_SPACING = 5.0;
    private static final double JITTER = 4.0;
    private static final double PIXEL_SIZE = 2.0;

    private final Random random = new Random();
    private final List<Vector2D> controlPoints = new ArrayList<>();
    private final List<List<Vector2D>> boltSegments = new ArrayList<>();

    private double remaining = DURATION;
    private double rebuildTimer;

    public PurpleChainLightningEffect(List<Vector2D> points) {
        if (points != null) {
            for (Vector2D point : points) {
                if (point != null) controlPoints.add(point.copy());
            }
        }
        rebuildBolt();
    }

    @Override
    public void update(double deltaSeconds) {
        if (deltaSeconds <= 0.0 || isFinished()) return;

        remaining = Math.max(0.0, remaining - deltaSeconds);
        rebuildTimer -= deltaSeconds;

        if (rebuildTimer <= 0.0 && !isFinished()) {
            rebuildTimer = REBUILD_INTERVAL;
            rebuildBolt();
        }
    }

    private void rebuildBolt() {
        boltSegments.clear();
        if (controlPoints.size() < 2) return;

        for (int i = 0; i < controlPoints.size() - 1; i++) {
            boltSegments.add(createJaggedSegment(controlPoints.get(i), controlPoints.get(i + 1)));
        }
    }

    private List<Vector2D> createJaggedSegment(Vector2D start, Vector2D end) {
        List<Vector2D> points = new ArrayList<>();
        double dx = end.getX() - start.getX();
        double dy = end.getY() - start.getY();
        double distance = Math.hypot(dx, dy);

        if (distance <= 0.001) {
            points.add(start.copy());
            points.add(end.copy());
            return points;
        }

        double dirX = dx / distance;
        double dirY = dy / distance;
        double perpendicularX = -dirY;
        double perpendicularY = dirX;
        int steps = Math.max(2, (int) Math.ceil(distance / POINT_SPACING));

        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            double x = start.getX() + dx * t;
            double y = start.getY() + dy * t;

            if (i != 0 && i != steps) {
                double offset = randomRange(-JITTER, JITTER);
                x += perpendicularX * offset;
                y += perpendicularY * offset;
            }

            points.add(new Vector2D(x, y));
        }

        return points;
    }

    @Override
    public void render(GraphicsContext gc, Camera camera) {
        if (gc == null || camera == null || isFinished()) return;

        double life = Math.max(0.0, remaining / DURATION);
        double zoom = camera.getZoom();
        double pixel = Math.max(1.0, Math.round(PIXEL_SIZE * zoom));

        gc.save();
        gc.setImageSmoothing(false);

        gc.setGlobalAlpha(0.45 + life * 0.55);
        gc.setFill(Color.rgb(170, 70, 255));
        for (List<Vector2D> segment : boltSegments) {
            drawPixelSegment(gc, camera, segment, pixel);
        }

        double corePixel = Math.max(1.0, Math.round(pixel * 0.5));
        gc.setGlobalAlpha(0.55 + life * 0.45);
        gc.setFill(Color.rgb(235, 205, 255));
        for (List<Vector2D> segment : boltSegments) {
            drawPixelSegment(gc, camera, segment, corePixel);
        }

        gc.restore();
    }

    private void drawPixelSegment(GraphicsContext gc, Camera camera,
                                  List<Vector2D> points, double pixelSize) {
        if (points == null || points.size() < 2) return;

        for (int i = 0; i < points.size() - 1; i++) {
            Vector2D a = points.get(i);
            Vector2D b = points.get(i + 1);

            double ax = camera.worldToScreenX(a.getX());
            double ay = camera.worldToScreenY(a.getY());
            double bx = camera.worldToScreenX(b.getX());
            double by = camera.worldToScreenY(b.getY());

            double dx = bx - ax;
            double dy = by - ay;
            double distance = Math.hypot(dx, dy);
            int steps = Math.max(1, (int) Math.ceil(distance / Math.max(1.0, pixelSize)));

            for (int step = 0; step <= steps; step++) {
                double t = (double) step / steps;
                double x = Math.round(ax + dx * t);
                double y = Math.round(ay + dy * t);
                gc.fillRect(x - pixelSize / 2.0, y - pixelSize / 2.0, pixelSize, pixelSize);
            }
        }
    }

    @Override
    public boolean isFinished() {
        return remaining <= 0.0;
    }

    private double randomRange(double min, double max) {
        return min + random.nextDouble() * (max - min);
    }
}