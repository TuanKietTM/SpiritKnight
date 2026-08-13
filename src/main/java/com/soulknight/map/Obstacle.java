package com.soulknight.map;

import com.soulknight.engine.Camera;
import com.soulknight.utils.Vector2D;
import com.soulknight.weapon.Bullet;
import javafx.geometry.BoundingBox;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

/**
 * class phu trach vat can
 */
public class Obstacle {
    private final Vector2D position;
    private final double width;
    private final double height;

    // Tinh chinh Hitbox thuc te cua vat can
    private double offsetX = 0;
    private double offsetY = 0;
    private double hitboxWidth;
    private double hitboxHeight;

    private int hp;
    private final int maxHp;
    private final boolean destructible;
    private Image sprite;

    public Obstacle(Vector2D position, double width, double height, int hp, boolean destructible, Image sprite) {
        this.position = position;
        this.width = width;
        this.height = height;
        this.hp = hp;
        this.maxHp = hp;
        this.destructible = destructible;
        this.sprite = sprite;
        this.hitboxWidth = width * 0.8;
        this.hitboxHeight = height * 0.8;
        this.offsetX = (width - hitboxWidth) / 2.0;
        this.offsetY = (height - hitboxHeight) / 2.0;
    }

    // Phuong thuc thiet lap Hitbox tuy chinh theo tung loai Sprite
    public void setCustomHitbox(double offsetX, double offsetY, double hitboxWidth, double hitboxHeight) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.hitboxWidth = hitboxWidth;
        this.hitboxHeight = hitboxHeight;
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
        double minX = position.getX() + offsetX;
        double maxX = minX + hitboxWidth;
        double minY = position.getY() + offsetY;
        double maxY = minY + hitboxHeight;

        // Tim diem tren Hitbox gan tam dan nhat
        double closestX = Math.max(minX, Math.min(center.getX(), maxX));
        double closestY = Math.max(minY, Math.min(center.getY(), maxY));

        double distanceX = center.getX() - closestX;
        double distanceY = center.getY() - closestY;

        return (distanceX * distanceX + distanceY * distanceY) <= (radius * radius);
    }

    /**
     * Kiem tra va cham AABB nguyen ban (giong het cach kiem tra voi Enemy/Player)
     * Chuyen tu intersectsCircle sang AABB de va cham "1 phat an ngay"
     */
    public boolean intersects(Bullet bullet) {
        if (bullet == null) return false;

        Vector2D bulletPos = bullet.getPosition();
        double r = bullet.getRadius();

        // Lay toa do hop va cham cua Bullet (xem bulletPos la tam)
        double bMinX = bulletPos.getX() - r;
        double bMaxX = bulletPos.getX() + r;
        double bMinY = bulletPos.getY() - r;
        double bMaxY = bulletPos.getY() + r;

        // Toa do hop va cham cua Obstacle
        double oMinX = position.getX();
        double oMaxX = position.getX() + width;
        double oMinY = position.getY();
        double oMaxY = position.getY() + height;

        // Kiem tra 2 hop chu nhat co de len nhau khong
        return (bMinX <= oMaxX && bMaxX >= oMinX && bMinY <= oMaxY && bMaxY >= oMinY);
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

    public Vector2D getPosition() {
        return position;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public boolean isDestructible() {
        return destructible;
    }

    public Image getSprite() {
        return sprite;
    }

    public void setSprite(Image sprite) {
        this.sprite = sprite;
    }

    public Vector2D getCenter() {
        return new Vector2D(position.getX() + width / 2.0, position.getY() + height / 2.0);
    }
}