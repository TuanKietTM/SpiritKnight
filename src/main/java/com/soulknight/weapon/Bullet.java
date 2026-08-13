package com.soulknight.weapon;

import com.soulknight.weapon.render.ProjectileRenderer;
import com.soulknight.engine.Camera;
import com.soulknight.entity.Entity;
import com.soulknight.entity.Enemy;
import com.soulknight.utils.ResourceLoader;
import com.soulknight.utils.Vector2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.util.HashSet;
import java.util.Set;

public final class Bullet {

    private ProjectileRenderer projectileRenderer;
    // Anh vien dan cua Player
    private static final String SPRITE_PATH = "/assets/effects/dan.png";
    private static final Image SPRITE = ResourceLoader.image(SPRITE_PATH);

    // Anh vien dan cua Quai (Tuy chon: neu ban co file anh dan_quai.png)
    private static final String ENEMY_SPRITE_PATH = "/assets/effects/dan_quai.png";
    private static final Image ENEMY_SPRITE = ResourceLoader.image(ENEMY_SPRITE_PATH);

    // Anh dan laser
    private static final String LASER_SPRITE_PATH = "/assets/effects/danlaser.png";
    private static final Image LASER_SPRITE = ResourceLoader.image(LASER_SPRITE_PATH);
    private static final int LASER_FRAME_COUNT = 8;
    private static final int LASER_LOOP_FIRST = 3;
    private static final int LASER_LOOP_COUNT = 4;
    private static final double LASER_FRAME_DURATION = 0.06;

    private final Vector2D position;
    private final Vector2D velocity;
    private final int damage;
    private final double radius;
    private final Entity owner;
    private final Color color;

    private boolean explodeOnTerrain;
    private boolean piercesObstacles = true;
    private double terrainExplosionRadius;
    private int terrainExplosionDamage;

    private final boolean piercing;
    private final boolean reflective;
    private final boolean soundWave;

    private int reflectionCount;
    private int maxReflections;

    private final Set<Object> hitTargets;
    private double age;
    private boolean active = true;

    private double dragFactor = 1.0;
    private double maxDistance = 9999.0;
    private double distanceTraveled = 0.0;

    public Bullet setDecelerationAndRange(double dragFactor, double maxDistance) {
        this.dragFactor = dragFactor;
        this.maxDistance = maxDistance;
        return this;
    }

    public Bullet(Vector2D position, Vector2D velocity, int damage, double radius, Entity owner, Color color) {
        this(position, velocity, damage, radius, owner, color, false, false);
    }

    public Bullet(Vector2D position, Vector2D velocity, int damage, double radius, Entity owner, Color color,
                  boolean piercing) {
        this(position, velocity, damage, radius, owner, color, piercing, false);
    }

    public Bullet(Vector2D position, Vector2D velocity, int damage, double radius, Entity owner, Color color,
                  boolean piercing, boolean reflective) {
        this(position, velocity, damage, radius, owner, color, piercing, reflective, false);
    }

    public Bullet(Vector2D position, Vector2D velocity, int damage, double radius, Entity owner, Color color,
                  boolean piercing, boolean reflective, boolean soundWave) {
        this.position = position;
        this.velocity = velocity;
        this.damage = damage;
        this.radius = radius;
        this.owner = owner;
        this.color = color;
        this.piercing = piercing;
        this.reflective = reflective;
        this.soundWave = soundWave;

        this.maxReflections = reflective ? 1 : 0;
        this.hitTargets = piercing ? new HashSet<>() : null;
    }

    public boolean isSoundWave() {
        return soundWave;
    }

    /**
     * Kiem tra xem vien dan nay co phai do Quai (Enemy) ban ra hay khong
     */
    public boolean isEnemyBullet() {
        return owner instanceof Enemy;
    }

    public void update(double deltaSeconds) {
        if (!active) return;

        this.age += deltaSeconds;

        if (dragFactor < 1.0) {
            velocity.scale(Math.pow(dragFactor, deltaSeconds * 60.0));
        }

        if (velocity.length() < 25.0) {
            this.deactivate();
            return;
        }

        double stepX = velocity.getX() * deltaSeconds;
        double stepY = velocity.getY() * deltaSeconds;
        double stepDistance = Math.hypot(stepX, stepY);

        distanceTraveled += stepDistance;

        if (maxDistance > 0 && distanceTraveled >= maxDistance) {
            this.deactivate();
            return;
        }

        getPosition().add(stepX, stepY);
    }

    public void render(GraphicsContext gc, Camera camera) {
        if (!active || gc == null || camera == null) return;

        if (projectileRenderer != null) {
            projectileRenderer.render(this, gc, camera);
            return;
        }

        double screenX = camera.worldToScreenX(position.getX());
        double screenY = camera.worldToScreenY(position.getY());

        if (piercing && LASER_SPRITE != null && LASER_SPRITE.getWidth() > 1.0) {
            renderLaser(gc, screenX, screenY, camera.getZoom());
            return;
        }

        double drawSize = radius * 3.0 * camera.getZoom();

        // ========================================================
        // 1. XU LY RIENG CHO DAN CUA QUAI (ENEMY BULLET)
        // ========================================================
        if (isEnemyBullet()) {
            // Neu co file anh dan_quai.png thi ve bang Sprite
            if (ENEMY_SPRITE != null && ENEMY_SPRITE.getWidth() > 1.0) {
                gc.save();
                gc.setImageSmoothing(false);
                gc.drawImage(ENEMY_SPRITE, screenX - drawSize / 2.0, screenY - drawSize / 2.0, drawSize, drawSize);
                gc.restore();
                return;
            }

            // Neu khong co Sprite quai, ve dan mau do/cam ruc ro co lop hao quang (Glow)
            gc.save();
            Color bulletColor = (this.color != null) ? this.color : Color.RED;

            // Ve hao quang phat sang xung quanh viên dan
            gc.setFill(bulletColor.deriveColor(0, 1, 1.2, 0.35));
            gc.fillOval(screenX - radius * 1.5, screenY - radius * 1.5, radius * 3.0, radius * 3.0);

            // Ve loi dan chinh o giua
            gc.setFill(bulletColor);
            gc.fillOval(screenX - radius, screenY - radius, radius * 2.0, radius * 2.0);

            // Ve mot diem sang trang nho o tam de tao cam giac vien dan dang chay
            gc.setFill(Color.WHITE);
            gc.fillOval(screenX - radius * 0.4, screenY - radius * 0.4, radius * 0.8, radius * 0.8);

            gc.restore();
            return;
        }

        // ========================================================
        // 2. XU LY DAN CUA PLAYER (MAC DINH DUNG DAN.PNG)
        // ========================================================
        if (SPRITE != null && SPRITE.getWidth() > 1.0) {
            gc.save();
            gc.setImageSmoothing(false);
            gc.drawImage(SPRITE, screenX - drawSize / 2.0, screenY - drawSize / 2.0, drawSize, drawSize);
            gc.restore();
            return;
        }

        gc.setFill(color);
        gc.fillOval(screenX - radius, screenY - radius, radius * 2.0, radius * 2.0);
    }

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

    public Bullet setPiercesObstacles(boolean value) {
        this.piercesObstacles = value;
        return this;
    }

    public boolean piercesObstacles() {
        return piercing && piercesObstacles;
    }

    public boolean canReflect() {
        return reflective && reflectionCount < maxReflections;
    }

    public Bullet setMaxReflections(int maxReflections) {
        this.maxReflections = reflective ? Math.max(0, maxReflections) : 0;
        return this;
    }

    public int getReflectionCount() {
        return reflectionCount;
    }

    public int getMaxReflections() {
        return maxReflections;
    }

    public void reflectOnce(Vector2D safePosition, boolean flipX, boolean flipY) {
        if (!canReflect() || safePosition == null) return;

        if (!flipX && !flipY) {
            flipX = true;
            flipY = true;
        }

        if (flipX) velocity.setX(-velocity.getX());
        if (flipY) velocity.setY(-velocity.getY());

        position.set(safePosition.getX(), safePosition.getY());
        reflectionCount++;

        if (hitTargets != null) hitTargets.clear();
    }

    public Bullet withTerrainExplosion(double radius, int damage) {
        this.explodeOnTerrain = true;
        this.terrainExplosionRadius = Math.max(0.0, radius);
        this.terrainExplosionDamage = Math.max(0, damage);
        return this;
    }

    public boolean explodesOnTerrain() {
        return explodeOnTerrain;
    }

    public double getTerrainExplosionRadius() {
        return terrainExplosionRadius;
    }

    public int getTerrainExplosionDamage() {
        return terrainExplosionDamage;
    }

    public Vector2D getVelocity() {
        return velocity;
    }

    public double getAge() {
        return age;
    }

    public Color getColor() {
        return color;
    }

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

    public Bullet withRenderer(ProjectileRenderer renderer) {
        this.projectileRenderer = renderer;
        return this;
    }
}