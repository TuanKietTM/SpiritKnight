package com.soulknight.weapon;

import com.soulknight.engine.GameWorld;
import com.soulknight.entity.Entity;
import com.soulknight.utils.SoundManager;
import com.soulknight.utils.Vector2D;
import javafx.scene.paint.Color;

import java.util.Random;

/**
 * Loai vu khi dac biet
 * khi an vao se nap nang luong
 * nap xong tha ra moi ban
 * nap cang lau thi muc do cong pha cang cao

 */
public final class PrototypeRailgun extends Weapon {

    private static final double MAX_CHARGE_TIME = 1.8;
    private static final double PIERCE_THRESHOLD = 0.50;

    private static final int MIN_DAMAGE = 12;
    private static final int MAX_DAMAGE = 42;

    private static final double MIN_SPEED = 680.0;
    private static final double MAX_SPEED = 360.0;

    private static final double MIN_RADIUS = 4.0;
    private static final double MAX_RADIUS = 10.0;

    private static final int MAX_PROJECTILES = 8;
    private static final int MAX_BOUNCES = 3;

    private static final double MIN_CRIT_CHANCE = 0.05;
    private static final double MAX_CRIT_CHANCE = 0.35;

    private static final double MAX_SPREAD = Math.toRadians(34.0);

    private final Random random = new Random();

    private boolean charging;
    private double chargeTime;
    private Vector2D lastTarget;

    public PrototypeRailgun(String name, double cooldownSeconds) {
        super(name, MIN_DAMAGE, cooldownSeconds);
    }

    // Bat dau hoac tiep tuc nap nang luong.
    public void charge(double deltaSeconds, Vector2D targetPosition) {
        if (!isReady() || targetPosition == null) return;

        charging = true;
        chargeTime = Math.min(MAX_CHARGE_TIME, chargeTime + Math.max(0.0, deltaSeconds));
        lastTarget = targetPosition.copy();
    }

    // Nha nut tan cong moi thuc su ban.
    public void release(GameWorld world, Entity owner, Vector2D targetPosition) {
        if (!charging || world == null || owner == null) return;

        Vector2D target = targetPosition != null ? targetPosition : lastTarget;

        if (target != null) fireChargedShot(world, owner, target);

        charging = false;
        chargeTime = 0.0;
        lastTarget = null;
    }

    private void fireChargedShot(GameWorld world, Entity owner, Vector2D targetPosition) {
        double charge = getChargeRatio();

        int projectileCount = getProjectileCount(charge);
        int damage = getChargedDamage(charge);
        double radius = lerp(MIN_RADIUS, MAX_RADIUS, charge);
        double speed = lerp(MIN_SPEED, MAX_SPEED, charge);
        double critChance = lerp(MIN_CRIT_CHANCE, MAX_CRIT_CHANCE, charge);

        boolean piercing = charge >= PIERCE_THRESHOLD;

        Vector2D direction = targetPosition.copy().subtract(owner.getPosition());

        if (direction.length() == 0.0) direction.setX(1.0);

        direction.normalize();

        double baseAngle = Math.atan2(direction.getY(), direction.getX());

        for (int i = 0; i < projectileCount; i++) {
            double angle = getProjectileAngle(baseAngle, i, projectileCount, charge);
            int finalDamage = rollCriticalDamage(damage, critChance);

            Vector2D velocity = new Vector2D(Math.cos(angle), Math.sin(angle)).scale(speed);
            Vector2D spawn = owner.getPosition().copy().add(
                    Math.cos(angle) * Gun.MUZZLE_DISTANCE,
                    Math.sin(angle) * Gun.MUZZLE_DISTANCE
            );

            Bullet bullet = new Bullet(
                    spawn,
                    velocity,
                    finalDamage,
                    radius,
                    owner,
                    Color.DEEPSKYBLUE,
                    piercing,
                    true
            );

            // Can them method nay vao Bullet.
            bullet.setMaxReflections(MAX_BOUNCES);

            world.addBullet(bullet);
        }

        SoundManager.getInstance().playSFX(getSoundPath());
        resetCooldown();
    }

    private double getProjectileAngle(double baseAngle, int index, int count, double charge) {
        if (count <= 1) return baseAngle;

        double spread = MAX_SPREAD * charge;
        double start = -spread / 2.0;
        double step = spread / (count - 1);

        return baseAngle + start + step * index;
    }

    private int getProjectileCount(double charge) {
        if (charge >= 1.0) return MAX_PROJECTILES;

        int count = 1 + (int) Math.floor(charge * (MAX_PROJECTILES - 1));
        return Math.max(1, Math.min(MAX_PROJECTILES, count));
    }

    private int getChargedDamage(double charge) {
        return (int) Math.round(lerp(MIN_DAMAGE, MAX_DAMAGE, charge));
    }

    private int rollCriticalDamage(int damage, double critChance) {
        if (random.nextDouble() < critChance) return damage * 2;
        return damage;
    }

    public double getChargeRatio() {
        return Math.min(1.0, chargeTime / MAX_CHARGE_TIME);
    }

    public boolean isCharging() {
        return charging;
    }

    @Override
    public void attack(GameWorld world, Entity owner, Vector2D targetPosition) {
        // Railgun khong ban truc tiep bang attack().
        // Player se goi charge() khi giu va release() khi nha.
    }

    private double lerp(double from, double to, double amount) {
        return from + (to - from) * Math.max(0.0, Math.min(1.0, amount));
    }
}