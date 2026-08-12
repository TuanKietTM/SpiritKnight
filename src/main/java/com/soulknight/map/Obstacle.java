package com.soulknight.map;

import com.soulknight.engine.Camera;
import com.soulknight.utils.Vector2D;
import javafx.geometry.BoundingBox;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

/**
 * class phu trach vat can
 */
public class Obstacle {
    private Vector2D position;
    private double width;
    private double height;
    private int hp;
    private int maxHp;
    private boolean destructible;

    private Image sprite;

    public Obstacle(Vector2D position, double width, double height, int hp, boolean destructible, Image sprite) {
        this.position = position;
        this.width = width;
        this.height = height;
        this.hp = hp;
        this.maxHp = hp;
        this.destructible = destructible;
        this.sprite = sprite;
    }

    public void takeDamage(int amount) {
        if (destructible) {
            this.hp -= amount;
        }
    }

    public boolean isDestroyed() {
        return destructible && hp <= 0;
    }
    public boolean intersectsCircle(Vector2D center, double radius) {
        double closestX = Math.max(position.getX(), Math.min(center.getX(), position.getX() + width));
        double closestY = Math.max(position.getY(), Math.min(center.getY(), position.getY() + height));

        double distanceX = center.getX() - closestX;
        double distanceY = center.getY() - closestY;

        return (distanceX * distanceX + distanceY * distanceY) < (radius * radius);
    }

    /**
     * Dung y-sorting
     */
    public double getRenderY() {
        return position.getY() + height;
    }

    public void render(GraphicsContext gc, Camera camera) {
        if (sprite == null) return;

        double screenX = camera.worldToScreenX(position.getX());
        double screenY = camera.worldToScreenY(position.getY());
        double zoom = camera.getZoom();
        gc.drawImage(sprite, screenX, screenY, width * zoom, height * zoom);
    }
    /**
     * Ve bong mo duoi chan ( co the cai tien them )
     */
    public void renderShadow(GraphicsContext gc, Camera camera) {
        double zoom = camera.getZoom();
        double screenX = camera.worldToScreenX(position.getX());
        double screenY = camera.worldToScreenY(position.getY());
        double shadowWidth = width * zoom * 0.9;
        double shadowHeight = height * zoom * 0.35;
        double shadowX = screenX + (width * zoom - shadowWidth) / 2.0;
        double shadowY = screenY + (height * zoom) - (shadowHeight / 2.0);

        gc.save();
        gc.setFill(Color.rgb(0, 0, 0, 0.35));
        gc.fillOval(shadowX, shadowY, shadowWidth, shadowHeight);
        gc.restore();
    }
    public Vector2D getPosition() { return position; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public boolean isDestructible() { return destructible; }
    public Image getSprite() { return sprite; }
    public void setSprite(Image sprite) { this.sprite = sprite; }
    public Vector2D getCenter() {
        return new Vector2D(position.getX() + width / 2.0, position.getY() + height / 2.0);
    }
}