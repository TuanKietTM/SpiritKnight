package com.soulknight.engine;

import com.soulknight.utils.Vector2D;

public final class Camera {

    private double offsetX;
    private double offsetY;

    // 🎯 THÊM: Hệ số zoom (Ví dụ: 2.0 nghĩa là phóng to gấp đôi. Bạn có thể chỉnh 1.5, 2.5 tùy ý)
    private double zoom = 2.0;

    public void follow(Vector2D target, double viewportWidth, double viewportHeight, double worldWidth, double worldHeight) {
        // Tính toán kích thước viewport ảo sau khi đã tính toán tỉ lệ zoom
        double zoomedViewportWidth = viewportWidth / zoom;
        double zoomedViewportHeight = viewportHeight / zoom;

        // Khóa biên camera dựa trên viewport ảo đã zoom
        offsetX = clamp(target.getX() - zoomedViewportWidth / 2.0, 0.0, Math.max(0.0, worldWidth - zoomedViewportWidth));
        offsetY = clamp(target.getY() - zoomedViewportHeight / 2.0, 0.0, Math.max(0.0, worldHeight - zoomedViewportHeight));
    }

    public double worldToScreenX(double worldX) {
        // 🎯 CHỈNH SỬA: Nhân thêm hệ số zoom khi chuyển đổi sang tọa độ màn hình
        return (worldX - offsetX) * zoom;
    }

    public double worldToScreenY(double worldY) {
        // 🎯 CHỈNH SỬA: Nhân thêm hệ số zoom khi chuyển đổi sang tọa độ màn hình
        return (worldY - offsetY) * zoom;
    }

    public double screenToWorldX(double screenX) {
        // Chia cho zoom khi dịch ngược từ màn hình về thế giới thực
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

    // GETTERS VÀ SETTERS ĐỂ TIỆN THAY ĐỔI ĐỘ ZOOM NẾU MUỐN
    public double getZoom() { return zoom; }
    public void setZoom(double zoom) { this.zoom = zoom; }
}