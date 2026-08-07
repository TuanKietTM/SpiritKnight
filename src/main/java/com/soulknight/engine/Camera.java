package com.soulknight.engine;

import com.soulknight.utils.Vector2D;

public final class Camera {

    private double offsetX;
    private double offsetY;

    private double zoom = 1.0;

    public void follow(Vector2D target, double viewportWidth, double viewportHeight, double worldWidth, double worldHeight) {
        if (target == null) return;

        double zoomedViewportWidth = viewportWidth / zoom;
        double zoomedViewportHeight = viewportHeight / zoom;

        // 1. Căn giữa camera vào giữa bản đồ nếu map quá nhỏ
        if (worldWidth <= zoomedViewportWidth) {
            offsetX = (worldWidth - zoomedViewportWidth) / 2.0;
        } else {
            offsetX = clamp(target.getX() - zoomedViewportWidth / 2.0, 0.0, worldWidth - zoomedViewportWidth);
        }

        if (worldHeight <= zoomedViewportHeight) {
            offsetY = (worldHeight - zoomedViewportHeight) / 2.0;
        } else {
            offsetY = clamp(target.getY() - zoomedViewportHeight / 2.0, 0.0, worldHeight - zoomedViewportHeight);
        }
    }

    // Làm tròn tọa độ Pixel thành số nguyên để JavaFX Render nét căng, không bị Anti-Aliasing nhấp nháy viền Tile
    public double worldToScreenX(double worldX) {
        return Math.floor((worldX - offsetX) * zoom);
    }

    public double worldToScreenY(double worldY) {
        return Math.floor((worldY - offsetY) * zoom);
    }

    public double screenToWorldX(double screenX) {
        return (screenX / zoom) + offsetX;
    }

    public double screenToWorldY(double screenY) {
        return (screenY / zoom) + offsetY;
    }

    public Vector2D screenToWorld(Vector2D screenPosition) {
        if (screenPosition == null) return new Vector2D();
        return new Vector2D(screenToWorldX(screenPosition.getX()), screenToWorldY(screenPosition.getY()));
    }

    public Vector2D worldToScreen(Vector2D worldPosition) {
        if (worldPosition == null) return new Vector2D();
        return new Vector2D(worldToScreenX(worldPosition.getX()), worldToScreenY(worldPosition.getY()));
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public double getZoom() {
        return zoom;
    }

    public void setZoom(double zoom) {
        this.zoom = Math.max(0.5, Math.min(4.0, zoom));
    }

    public double getOffsetX() { return offsetX; }
    public double getOffsetY() { return offsetY; }
}