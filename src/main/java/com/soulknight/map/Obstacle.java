package com.soulknight.map;

import com.soulknight.engine.Camera;
import com.soulknight.utils.Vector2D;
import javafx.geometry.BoundingBox;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class Obstacle {
    private Vector2D position;
    private double width;
    private double height;
    private int hp;
    private int maxHp;
    private boolean destructible;

    public Obstacle(Vector2D position, double width, double height, int hp, boolean destructible) {
        this.position = position;
        this.width = width;
        this.height = height;
        this.hp = hp;
        this.maxHp = hp;
        this.destructible = destructible;
    }

    public void takeDamage(int amount) {
        if (destructible) {
            this.hp -= amount;
        }
    }

    public boolean isDestroyed() {
        return destructible && hp <= 0;
    }

    // Lấy hộp va chạm BoundingBox
    public BoundingBox getBoundingBox() {
        return new BoundingBox(position.getX(), position.getY(), width, height);
    }

    // Kiểm tra va chạm tròn (cho đạn, player, enemy)
    public boolean intersectsCircle(Vector2D center, double radius) {
        double closestX = Math.max(position.getX(), Math.min(center.getX(), position.getX() + width));
        double closestY = Math.max(position.getY(), Math.min(center.getY(), position.getY() + height));

        double distanceX = center.getX() - closestX;
        double distanceY = center.getY() - closestY;

        return (distanceX * distanceX + distanceY * distanceY) < (radius * radius);
    }

    public void render(GraphicsContext gc, Camera camera) {
        double screenX = camera.worldToScreenX(position.getX());
        double screenY = camera.worldToScreenY(position.getY());
        double zoom = camera.getZoom();

        if (destructible) {
            // Hòm gỗ có thể phá hủy
            gc.setFill(Color.BROWN);
            gc.fillRect(screenX, screenY, width * zoom, height * zoom);
            gc.setStroke(Color.SADDLEBROWN);
            gc.setLineWidth(2.0 * zoom);
            gc.strokeRect(screenX, screenY, width * zoom, height * zoom);
        } else {
            // Cột đá không thể phá hủy
            gc.setFill(Color.GRAY);
            gc.fillRect(screenX, screenY, width * zoom, height * zoom);
            gc.setStroke(Color.DARKGRAY);
            gc.setLineWidth(2.0 * zoom);
            gc.strokeRect(screenX, screenY, width * zoom, height * zoom);
        }
        // Vẽ thanh máu cho vật cản phá hủy được khi bị mất máu
        if (destructible && hp < maxHp && hp > 0) {
            double barWidth = width * zoom;
            double barHeight = 4.0 * zoom;
            double barX = screenX;
            double barY = screenY - 8.0 * zoom; // Vẽ phía trên vật cản

            // Phông nền thanh máu (màu đỏ)
            gc.setFill(Color.RED);
            gc.fillRect(barX, barY, barWidth, barHeight);

            // Phần máu còn lại (màu xanh lá)
            gc.setFill(Color.LIME);
            gc.fillRect(barX, barY, barWidth * ((double) hp / maxHp), barHeight);
        }
    }

    public Vector2D getPosition() { return position; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public boolean isDestructible() { return destructible; }
    public Vector2D getCenter() { return new Vector2D(position.getX() + width / 2.0, position.getY() + height / 2.0); }
}