package com.soulknight.entity;

import com.soulknight.engine.Camera;
import com.soulknight.engine.GameWorld;
import com.soulknight.utils.Vector2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Lop quan ly Song Xung Kich (Shockwave) do Boss giam chan/no nang luong tao ra.
 * Song lan rong tu tam ra ngoai va gay sat thuong khi cham vao Player.
 */
public class Shockwave {

    private final Vector2D position;
    private double currentRadius;
    private final double maxRadius;
    private final double expandSpeed;
    private final double thickness;
    private final int damage;
    private final Color color;
    private final boolean fromPlayer;

    private boolean active = true;
    private boolean hitPlayer = false; // Dung de dam bao Player chi bi trung 1 lan moi dot song

    // Constructor 1: Mặc định (7 tham số - dành cho Boss/Enemy)
    public Shockwave(Vector2D position, double initialRadius, double maxRadius, double expandSpeed, double thickness, int damage, Color color) {
        this(position, initialRadius, maxRadius, expandSpeed, thickness, damage, color, false);
    }

    // Constructor 2: Đầy đủ (8 tham số - truyền thêm fromPlayer)
    public Shockwave(Vector2D position, double initialRadius, double maxRadius, double expandSpeed, double thickness, int damage, Color color, boolean fromPlayer) {
        this.position = (position != null) ? position.copy() : new Vector2D(0, 0);
        this.currentRadius = initialRadius;
        this.maxRadius = maxRadius;
        this.expandSpeed = expandSpeed;
        this.thickness = thickness;
        this.damage = damage;
        this.color = (color != null) ? color : Color.RED;
        this.fromPlayer = fromPlayer; // Lưu biến fromPlayer
    }

    public void update(GameWorld world, double deltaSeconds) {
        if (!active) return;

        // Lan rộng sóng xung kích
        currentRadius += expandSpeed * deltaSeconds;

        if (currentRadius >= maxRadius) {
            active = false;
            return;
        }

        // Kiem tra va cham voi Player
        Player player = world.getPlayer();
        if (player != null && !hitPlayer && player.getPosition() != null) {
            double dist = position.distance(player.getPosition());

            // Va cham xay ra khi Player nam trong do day (thickness) cua ria song
            double innerBound = currentRadius - thickness / 2.0;
            double outerBound = currentRadius + thickness / 2.0 + player.getRadius();

            if (dist >= innerBound && dist <= outerBound) {
                player.takeDamage(damage);
                hitPlayer = true; // Danh dau da trúng đạn/sóng để không bị trừ máu liên tục
            }
        }
    }

    public void render(GraphicsContext gc, Camera camera) {
        if (!active || gc == null || camera == null) return;

        double screenX = camera.worldToScreenX(position.getX());
        double screenY = camera.worldToScreenY(position.getY());
        double zoom = camera.getZoom();

        double drawRadius = currentRadius * zoom;
        double drawThickness = Math.max(2.0, thickness * zoom);

        gc.save();
        gc.setImageSmoothing(false);

        // Mo dan khi song lan ra xa
        double alpha = Math.max(0.1, 1.0 - (currentRadius / maxRadius));
        gc.setGlobalAlpha(alpha);

        // 1. Ve lop hao quang bao ngoai
        gc.setStroke(color.deriveColor(0, 1, 1.2, 0.4));
        gc.setLineWidth(drawThickness + 6.0 * zoom);
        gc.strokeOval(screenX - drawRadius, screenY - drawRadius, drawRadius * 2, drawRadius * 2);

        // 2. Ve vong song chinh
        gc.setStroke(color);
        gc.setLineWidth(drawThickness);
        gc.strokeOval(screenX - drawRadius, screenY - drawRadius, drawRadius * 2, drawRadius * 2);

        // 3. Ve loi trang phat sang o giua ria song
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(Math.max(1.0, drawThickness * 0.35));
        gc.strokeOval(screenX - drawRadius, screenY - drawRadius, drawRadius * 2, drawRadius * 2);

        gc.restore();
    }

    public boolean isActive() {
        return active;
    }
}