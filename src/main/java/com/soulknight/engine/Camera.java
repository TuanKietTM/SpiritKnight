package com.soulknight.engine;

import com.soulknight.utils.Vector2D;

public final class Camera {

    private double offsetX;
    private double offsetY;


    private double zoom = 1.5;

    public void follow(Vector2D target, double viewportWidth, double viewportHeight, double worldWidth, double worldHeight) {
        double zoomedViewportWidth = viewportWidth / zoom;
        double zoomedViewportHeight = viewportHeight / zoom;

        // 2. Nếu kích thước bản đồ thực tế nhỏ hơn khung nhìn viewport ảo:
        // Căn giữa camera vào giữa bản đồ thay vì khóa biên ở góc trái (0,0)
        if (worldWidth <= zoomedViewportWidth) {
            offsetX = (worldWidth - zoomedViewportWidth) / 2.0;
        } else {
            // Ngược lại, cho camera bám theo Player và khóa biên lại
            offsetX = clamp(target.getX() - zoomedViewportWidth / 2.0, 0.0, worldWidth - zoomedViewportWidth);
        }

        if (worldHeight <= zoomedViewportHeight) {
            offsetY = (worldHeight - zoomedViewportHeight) / 2.0;
        } else {
            offsetY = clamp(target.getY() - zoomedViewportHeight / 2.0, 0.0, worldHeight - zoomedViewportHeight);
        }
    }

    public double worldToScreenX(double worldX) {
        // Chuyển đổi tọa độ thế giới thực sang tọa độ pixel hiển thị trên màn hình
        return (worldX - offsetX) * zoom;
    }

    public double worldToScreenY(double worldY) {
        return (worldY - offsetY) * zoom;
    }

    public double screenToWorldX(double screenX) {
        // Dịch ngược từ pixel màn hình về tọa độ thế giới thực (dành cho logic bắn súng theo chuột)
        return (screenX / zoom) + offsetX;
    }

    public double screenToWorldY(double screenY) {
        return (screenY / zoom) + offsetY;
    }

    public Vector2D screenToWorld(Vector2D screenPosition) {
        return new Vector2D(screenToWorldX(screenPosition.getX()), screenToWorldY(screenPosition.getY()));
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public double getZoom() {
        return zoom;
    }

    public void setZoom(double zoom) {
        // Giới hạn zoom tối thiểu là 0.5 và tối đa là 4.0 để tránh lỗi vỡ hình hoặc quá nhỏ
        this.zoom = Math.max(0.5, Math.min(4.0, zoom));
    }

    public double getOffsetX() { return offsetX; }
    public double getOffsetY() { return offsetY; }
}