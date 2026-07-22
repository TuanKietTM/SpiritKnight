package com.soulknight.weapon;

import com.soulknight.engine.GameWorld;
import com.soulknight.entity.Entity;
import com.soulknight.utils.SoundManager;
import com.soulknight.utils.Vector2D;
import javafx.scene.paint.Color;

public final class Gun extends Weapon {

    // Khoang cach tu tam nhan vat den dau nong sung (noi dan bay ra)
    public static final double MUZZLE_DISTANCE = 22.0;

    private final double bulletSpeed;
    private final double spreadRadians;

    public Gun(String name, int damage, double cooldownSeconds, double bulletSpeed, double spreadRadians) {
        super(name, damage, cooldownSeconds);
        this.bulletSpeed = bulletSpeed;
        this.spreadRadians = spreadRadians;
    }

    @Override
    public void attack(GameWorld world, Entity owner, Vector2D targetPosition) {
        if (!isReady()) {
            return;
        }

        SoundManager.getInstance().playSFX("Bullet");
        Vector2D direction = targetPosition.copy().subtract(owner.getPosition());
        if (direction.length() == 0.0) {
            direction.setX(1.0);
        }
        direction.normalize();

        double angle = Math.atan2(direction.getY(), direction.getX()) + spreadRadians;
        Vector2D velocity = new Vector2D(Math.cos(angle), Math.sin(angle)).scale(bulletSpeed);
        // Dan xuat phat tu dau nong sung theo huong ban (khong phai tu tam nhan vat)
        Vector2D spawn = owner.getPosition().copy()
                .add(Math.cos(angle) * MUZZLE_DISTANCE, Math.sin(angle) * MUZZLE_DISTANCE);

        world.addBullet(new Bullet(spawn, velocity, getDamage(), 4.0, owner, Color.GOLD));
        resetCooldown();
    }
}
