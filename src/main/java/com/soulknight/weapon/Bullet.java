package com.soulknight.weapon;

import com.soulknight.engine.Camera;
import com.soulknight.entity.Entity;
import com.soulknight.utils.ResourceLoader;
import com.soulknight.utils.Vector2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.util.HashSet;
import java.util.Set;

public final class Bullet {

    // Anh vien dan (dung chung cho moi vien), ve thay cho hinh tron to mau
    private static final String SPRITE_PATH = "/assets/effects/dan.png";
    private static final Image SPRITE = ResourceLoader.image(SPRITE_PATH);

    // Anh dan laser: sheet 8 khung hinh xep doc (moi khung vuong), tia huong sang phai
    private static final String LASER_SPRITE_PATH = "/assets/effects/danlaser.png";
    private static final Image LASER_SPRITE = ResourceLoader.image(LASER_SPRITE_PATH);
    private static final int LASER_FRAME_COUNT = 8;
    // Chi lap cac khung tia sang ro net (bo khung mo dau/tan bien) khi dan dang bay
    private static final int LASER_LOOP_FIRST = 3;
    private static final int LASER_LOOP_COUNT = 4;
    private static final double LASER_FRAME_DURATION = 0.06;

    private final Vector2D position;
    private final Vector2D velocity;
    private final int damage;
    private final double radius;
    private final Entity owner;
    private final Color color;
    // Dan xuyen: bay qua muc tieu va gay sat thuong nhieu con (moi con mot lan)
    private final boolean piercing;
    private final Set<Object> hitTargets;
    private double age;
    private boolean active = true;

    public Bullet(Vector2D position, Vector2D velocity, int damage, double radius, Entity owner, Color color) {
        this(position, velocity, damage, radius, owner, color, false);
    }

    public Bullet(Vector2D position, Vector2D velocity, int damage, double radius, Entity owner, Color color,
                  boolean piercing) {
        this.position = position;
        this.velocity = velocity;
        this.damage = damage;
        this.radius = radius;
        this.owner = owner;
        this.color = color;
        this.piercing = piercing;
        this.hitTargets = piercing ? new HashSet<>() : null;
    }

    public void update(double deltaSeconds) {
        age += deltaSeconds;
        position.add(velocity.copy().scale(deltaSeconds));
    }

    public void render(GraphicsContext graphicsContext, Camera camera) {
        double screenX = camera.worldToScreenX(position.getX());
        double screenY = camera.worldToScreenY(position.getY());
        if (piercing && LASER_SPRITE != null && LASER_SPRITE.getWidth() > 1.0) {
            renderLaser(graphicsContext, screenX, screenY, camera.getZoom());
            return;
        }
        // Ve to hon ban kinh va cham mot chut de vien dan nhin ro tren man hinh
        double drawSize = radius * 3.0 * camera.getZoom();
        if (SPRITE != null && SPRITE.getWidth() > 1.0) {
            graphicsContext.save();
            graphicsContext.setImageSmoothing(false);
            graphicsContext.drawImage(SPRITE,
                    screenX - drawSize / 2.0, screenY - drawSize / 2.0, drawSize, drawSize);
            graphicsContext.restore();
        } else {
            // Du phong khi anh chua tai duoc: ve hinh tron nhu cu
            graphicsContext.setFill(color);
            graphicsContext.fillOval(screenX - radius, screenY - radius, radius * 2.0, radius * 2.0);
        }
    }

    // Ve tia laser: khung hinh dong, xoay theo huong bay cua dan
    private void renderLaser(GraphicsContext gc, double screenX, double screenY, double zoom) {
        double drawSize = radius * 6.0 * zoom;
        double frameHeight = LASER_SPRITE.getHeight() / LASER_FRAME_COUNT;
        int frame = LASER_LOOP_FIRST + (int) (age / LASER_FRAME_DURATION) % LASER_LOOP_COUNT;
        double angleDegrees = Math.toDegrees(Math.atan2(velocity.getY(), velocity.getX()));

        gc.save();
        gc.setImageSmoothing(false);
        gc.translate(screenX, screenY);
        gc.rotate(angleDegrees);
        gc.drawImage(LASER_SPRITE,
                0, frame * frameHeight, LASER_SPRITE.getWidth(), frameHeight,
                -drawSize / 2.0, -drawSize / 2.0, drawSize, drawSize);
        gc.restore();
    }

    public boolean intersects(Entity entity) {
        return position.distance(entity.getPosition()) <= radius + entity.getRadius();
    }

    public boolean isPiercing() {
        return piercing;
    }

    // Kiem tra muc tieu da trung tia laser nay chua (tranh cong sat thuong lien tuc)
    public boolean hasAlreadyHit(Object target) {
        return hitTargets != null && hitTargets.contains(target);
    }

    public void markHit(Object target) {
        if (hitTargets != null) {
            hitTargets.add(target);
        }
    }

    public Vector2D getPosition() {
        return position;
    }

    public int getDamage() {
        return damage;
    }

    public double getRadius() {
        return radius;
    }

    public Entity getOwner() {
        return owner;
    }

    public boolean isActive() {
        return active;
    }

    public void deactivate() {
        active = false;
    }
}
