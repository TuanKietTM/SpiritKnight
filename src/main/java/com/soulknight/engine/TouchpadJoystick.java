package com.soulknight.engine;

import com.soulknight.utils.Vector2D;

public class TouchpadJoystick {
    /**
     * Huong dan choi mode touchpadjoytick
     * mode ranh tay
     * tu dong tim ke thu va ban
     *su dung bang cach doi mode joytick
     * an trai chuot ( nen dung chuot ) va di tuochpad de di chuyen 360 do
     */
    private Vector2D centerPosition;
    private Vector2D currentPosition;
    private boolean active;

    private final double deadZone = 10.0;
    private final double maxRadius = 70.0;   //

    public TouchpadJoystick() {
        this.centerPosition = new Vector2D(0, 0);
        this.currentPosition = new Vector2D(0, 0);
        this.active = false;
    }

    // Khi bắt đầu chạm/nhấn giữ Touchpad
    public void onTouchStart(double x, double y) {
        this.centerPosition.setX(x);
        this.centerPosition.setY(y);
        this.currentPosition.setX(x);
        this.currentPosition.setY(y);
        this.active = true;
    }

    // Khi vuốt di chuyển ngón tay trên Touchpad
    public void onTouchMove(double x, double y) {
        if (active) {
            this.currentPosition.setX(x);
            this.currentPosition.setY(y);
        }
    }

    // Khi nhấc ngón tay ra khỏi Touchpad
    public void onTouchEnd() {
        this.active = false;
    }

    /**
     * Tính toán Vector hướng di chuyển 360 độ (đã được chuẩn hóa từ 0.0 đến 1.0)
     */
    public Vector2D getMoveDirection() {
        if (!active) {
            return new Vector2D(0, 0);
        }

        Vector2D delta = currentPosition.copy().subtract(centerPosition);
        double distance = delta.length();

        // Nằm trong vùng chết -> Chưa di chuyển
        if (distance < deadZone) {
            return new Vector2D(0, 0);
        }

        // Chuẩn hóa hướng di chuyển 360 độ
        Vector2D direction = delta.normalize();

        // Tính tỉ lệ lực kéo (từ 0.0 đến 1.0)
        double intensity = Math.min((distance - deadZone) / (maxRadius - deadZone), 1.0);

        return direction.scale(intensity);
    }

    public boolean isActive() { return active; }
}