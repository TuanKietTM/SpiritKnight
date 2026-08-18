package com.soulknight.weapon;

import com.soulknight.engine.Camera;
import com.soulknight.utils.Vector2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class RainbowRayEffect {
    private final Vector2D startPos;
    private final Vector2D endPos;
    private final double maxThickness;
    private final double duration;
    private final Color rayColor;
    private double timer = 0.0;

    public RainbowRayEffect(Vector2D startPos, Vector2D endPos, double maxThickness, double duration, Color rayColor) {
        this.startPos = startPos.copy();
        this.endPos = endPos.copy();
        this.maxThickness = maxThickness;
        this.duration = duration;
        this.rayColor = (rayColor != null) ? rayColor : Color.CYAN;
    }

    public void update(double deltaSeconds) {
        timer += deltaSeconds;
    }

    public void render(GraphicsContext gc, Camera camera) {
        if (isFinished()) return;

        double progress = timer / duration;
        double alpha = 1.0 - progress;
        double currentThickness = maxThickness * Math.sin(progress * Math.PI);

        double sx = camera.worldToScreenX(startPos.getX());
        double sy = camera.worldToScreenY(startPos.getY());
        double ex = camera.worldToScreenX(endPos.getX());
        double ey = camera.worldToScreenY(endPos.getY());

        gc.save();

        gc.setGlobalAlpha(alpha * 0.3);
        gc.setStroke(rayColor);
        gc.setLineWidth(Math.max(1.0, currentThickness * 1.8 * camera.getZoom()));
        gc.strokeLine(sx, sy, ex, ey);

        gc.setGlobalAlpha(alpha);
        gc.setStroke(Color.WHITE.interpolate(rayColor, 0.3));
        gc.setLineWidth(Math.max(1.0, currentThickness * camera.getZoom()));
        gc.strokeLine(sx, sy, ex, ey);

        gc.restore();
    }

    public boolean isActive() {
        return timer < duration;
    }

    public boolean isFinished() {
        return timer >= duration;
    }
}