package com.soulknight.engine;

import com.soulknight.utils.Vector2D;

public final class Camera {

    private double offsetX;
    private double offsetY;

    public void follow(Vector2D target, double viewportWidth, double viewportHeight, double worldWidth, double worldHeight) {
        offsetX = clamp(target.getX() - viewportWidth / 2.0, 0.0, Math.max(0.0, worldWidth - viewportWidth));
        offsetY = clamp(target.getY() - viewportHeight / 2.0, 0.0, Math.max(0.0, worldHeight - viewportHeight));
    }

    public double worldToScreenX(double worldX) {
        return worldX - offsetX;
    }

    public double worldToScreenY(double worldY) {
        return worldY - offsetY;
    }

    public double screenToWorldX(double screenX) {
        return screenX + offsetX;
    }

    public double screenToWorldY(double screenY) {
        return screenY + offsetY;
    }

    public Vector2D screenToWorld(Vector2D screenPosition) {
        return new Vector2D(screenToWorldX(screenPosition.getX()), screenToWorldY(screenPosition.getY()));
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
