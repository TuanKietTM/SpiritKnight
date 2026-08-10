package com.soulknight.weapon;

import com.soulknight.engine.GameWorld;
import com.soulknight.entity.Entity;
import com.soulknight.utils.SoundManager;
import com.soulknight.utils.Vector2D;
import com.soulknight.weapon.render.IonProjectileRenderer;
import javafx.scene.paint.Color;

import java.util.Random;

/**
 * Sung dien tu Ion.
 *
 * Co che:
 * - Giu nut tan cong de nap nang luong.
 * - Damage, crit, kich thuoc dan va ban kinh no tang theo charge.
 * - Player bi giam toc khi dang charge.
 * - Nha nut tan cong moi ban.
 * - Ion Orb xuyen Enemy nhung khong xuyen terrain/obstacle.
 * - Visual projectile duoc tach sang IonProjectileRenderer, khong dung sprite laser.
 */
public final class IonElectromagneticGun extends Weapon {

    private static final double MAX_CHARGE_TIME = 1.6;

    private static final int MIN_DAMAGE = 18;
    private static final int MAX_DAMAGE = 55;

    private static final double MIN_CRIT_CHANCE = 0.05;
    private static final double MAX_CRIT_CHANCE = 0.40;

    private static final double BULLET_SPEED = 900.0;

    private static final double MIN_BULLET_RADIUS = 5.0;
    private static final double MAX_BULLET_RADIUS = 13.0;

    private static final double MIN_EXPLOSION_RADIUS = 38.0;
    private static final double MAX_EXPLOSION_RADIUS = 82.0;

    // Full charge thi Player con 45% toc do di chuyen.
    private static final double MIN_MOVE_SPEED_MULTIPLIER = 0.45;

    private static final Color ION_COLOR = Color.web("#14E6C1");

    private final Random random = new Random();

    private boolean charging;
    private double chargeTime;
    private Vector2D lastTarget;

    public IonElectromagneticGun(String name, double cooldownSeconds) {
        super(name, MIN_DAMAGE, cooldownSeconds);
    }

    /**
     * Giu Attack de nap nang luong.
     * Neu weapon dang cooldown thi khong bat dau charge moi.
     */
    public void charge(double deltaSeconds, Vector2D targetPosition) {
        if (!isReady() || targetPosition == null) return;

        charging = true;
        chargeTime = Math.min(MAX_CHARGE_TIME, chargeTime + Math.max(0.0, deltaSeconds));
        lastTarget = targetPosition.copy();
    }

    /**
     * Chi ban tai frame nguoi choi vua nha nut Attack.
     */
    public void release(GameWorld world, Entity owner, Vector2D targetPosition) {
        if (!charging || world == null || owner == null) return;

        Vector2D target = targetPosition != null ? targetPosition : lastTarget;

        if (target != null) fireIonOrb(world, owner, target);

        resetCharge();
    }

    /**
     * Tao Ion Orb dua tren muc charge hien tai.
     * Kich thuoc projectile trung voi kich thuoc qua cau charge luc nha tay.
     */
    private void fireIonOrb(GameWorld world, Entity owner, Vector2D targetPosition) {
        double charge = getChargeRatio();

        int damage = getChargedDamage(charge);
        double critChance = lerp(MIN_CRIT_CHANCE, MAX_CRIT_CHANCE, charge);
        double bulletRadius = getOrbRadius(charge);
        double explosionRadius = getExplosionRadius(charge);

        boolean critical = random.nextDouble() < critChance;
        if (critical) damage *= 2;

        Vector2D direction = targetPosition.copy().subtract(owner.getPosition());
        if (direction.length() == 0.0) direction.setX(1.0);

        direction.normalize();

        double angle = Math.atan2(direction.getY(), direction.getX());
        Vector2D velocity = new Vector2D(Math.cos(angle), Math.sin(angle)).scale(BULLET_SPEED);
        Vector2D spawn = owner.getPosition().copy().add(Math.cos(angle) * Gun.MUZZLE_DISTANCE, Math.sin(angle) * Gun.MUZZLE_DISTANCE);

        Bullet bullet = new Bullet(spawn, velocity, damage, bulletRadius, owner, ION_COLOR, true, false);

        // Visual Ion duoc renderer rieng xu ly, Bullet khong con biet ionCharge hay ionProjectile.
        bullet.withRenderer(new IonProjectileRenderer(charge));

        // Ion xuyen Enemy nhung dung lai khi cham obstacle/terrain.
        bullet.setPiercesObstacles(false);

        // Chi luu metadata gameplay, GameWorld se tao Ion explosion rieng khi va cham.
        bullet.withTerrainExplosion(explosionRadius, damage);

        world.addBullet(bullet);

        SoundManager.getInstance().playSFX(getSoundPath());
        resetCooldown();
    }

    public boolean isCharging() {
        return charging;
    }

    public double getChargeRatio() {
        return Math.min(1.0, chargeTime / MAX_CHARGE_TIME);
    }

    public double getChargeTime() {
        return chargeTime;
    }

    public double getMaxChargeTime() {
        return MAX_CHARGE_TIME;
    }

    /**
     * Player dung gia tri nay de ve qua cau dang nap tai dau nong.
     */
    public double getCurrentOrbRadius() {
        return getOrbRadius(getChargeRatio());
    }

    public double getCurrentExplosionRadius() {
        return getExplosionRadius(getChargeRatio());
    }

    /**
     * Toc do Player giam tu 100% xuong 45% theo charge.
     */
    public double getMoveSpeedMultiplier() {
        return lerp(1.0, MIN_MOVE_SPEED_MULTIPLIER, getChargeRatio());
    }

    public Color getIonColor() {
        return ION_COLOR;
    }

    /**
     * Huy charge khi doi weapon hoac reset Player.
     */
    public void cancelCharge() {
        resetCharge();
    }

    private void resetCharge() {
        charging = false;
        chargeTime = 0.0;
        lastTarget = null;
    }

    private int getChargedDamage(double charge) {
        return (int) Math.round(lerp(MIN_DAMAGE, MAX_DAMAGE, charge));
    }

    private double getOrbRadius(double charge) {
        return lerp(MIN_BULLET_RADIUS, MAX_BULLET_RADIUS, charge);
    }

    private double getExplosionRadius(double charge) {
        return lerp(MIN_EXPLOSION_RADIUS, MAX_EXPLOSION_RADIUS, charge);
    }

    @Override
    public void attack(GameWorld world, Entity owner, Vector2D targetPosition) {
        // Ion khong ban truc tiep bang attack(). Player dieu khien charge/release.
    }

    private double lerp(double from, double to, double amount) {
        double t = Math.max(0.0, Math.min(1.0, amount));
        return from + (to - from) * t;
    }
}