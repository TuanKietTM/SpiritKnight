package com.soulknight.weapon;

import com.soulknight.weapon.render.ProjectileRenderer;
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

    private ProjectileRenderer projectileRenderer;
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
    // Dan dac biet co the tao vu no khi cham dia hinh.\
    private boolean explodeOnTerrain;
    // Projectile Ion co renderer rieng, khong lien quan den sprite laser.

    // Ion xuyen Enemy nhung khong bat buoc xuyen obstacle.
    private boolean piercesObstacles = true;
    private double terrainExplosionRadius;
    private int terrainExplosionDamage;
    // Dan xuyen: bay qua muc tieu va gay sat thuong nhieu con (moi con mot lan)
    private final boolean piercing;
    // Dan phan tuong cu van mac dinh chi nay 1 lan.
    private final boolean reflective;

    // So lan da nay va gioi han nay. Railgun co the tang maxReflections len 3.
    private int reflectionCount;
    private int maxReflections;

    private final Set<Object> hitTargets;
    private double age;
    private boolean active = true;
    // Biến nâng cấp đạn cho quái đánh xa tiến hóa
    private double dragFactor = 1.0;     // Hệ số cản
    private double maxDistance = 9999.0;    // Tầm bay tối đa
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
        this.position = position;
        this.velocity = velocity;
        this.damage = damage;
        this.radius = radius;
        this.owner = owner;
        this.color = color;
        this.piercing = piercing;
        this.reflective = reflective;

        // Giu nguyen logic cu: reflective=true mac dinh chi nay 1 lan.
        this.maxReflections = reflective ? 1 : 0;

        this.hitTargets = piercing ? new HashSet<>() : null;
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

        // Projectile dac biet tu quyet dinh cach ve.
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
    public Bullet setPiercesObstacles(boolean value) {
        this.piercesObstacles = value;
        return this;
    }

    public boolean piercesObstacles() {
        return piercing && piercesObstacles;
    }

    // Con co the phan tuong neu chua dat gioi han so lan nay.
    public boolean canReflect() {
        return reflective && reflectionCount < maxReflections;
    }

    /**
     * Tang gioi han so lan nay cho cac loai dan dac biet.
     * Dan cu khong goi ham nay nen van giu nguyen gioi han 1 lan.
     */
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

    // Giu ten method cu de GameWorld hien tai khong can sua logic dan thuong.
    // Moi lan cham tuong hop le se tang reflectionCount len 1.
    public void reflectOnce(Vector2D safePosition, boolean flipX, boolean flipY) {
        if (!canReflect() || safePosition == null) return;

        if (!flipX && !flipY) {
            // Cham goc/khong xac dinh duoc truc -> lat ca hai de dan quay dau.
            flipX = true;
            flipY = true;
        }

        if (flipX) velocity.setX(-velocity.getX());
        if (flipY) velocity.setY(-velocity.getY());

        position.set(safePosition.getX(), safePosition.getY());
        reflectionCount++;

        // Giu nguyen logic laser cu: sau moi lan nay co the gay sat thuong lai vao muc tieu da trung.
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
    public Bullet withRenderer(ProjectileRenderer renderer) {
        this.projectileRenderer = renderer;
        return this;
    }
}