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
    // Dan xuyen (laser): bay qua quai, gay sat thuong nhieu con
    private boolean piercingBullets;

    public Gun(String name, int damage, double cooldownSeconds, double bulletSpeed, double spreadRadians) {
        super(name, damage, cooldownSeconds);
        this.bulletSpeed = bulletSpeed;
        this.spreadRadians = spreadRadians;
    }

    // Bat che do dan xuyen, tra ve chinh no de tien goi noi tiep
    public Gun withPiercing() {
        this.piercingBullets = true;
        return this;
    }

    @Override
    public void attack(GameWorld world, Entity owner, Vector2D targetPosition) {
        if (!isReady()) {
            return;
        }

        String sound = getSoundPath();
        if (sound != null && !sound.isBlank()) {
            SoundManager.getInstance().playSFX(sound);
        } else {
            SoundManager.getInstance().playSFX("Bullet");
        }
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

        // Kiểm tra tên vũ khí để xác định là súng sóng âm (phương án tạm thời)
        boolean isSoundWaveGun = getName().equals("Sound Wave Gun");
        world.addBullet(new Bullet(spawn, velocity, getDamage(), 6.0, owner,
                isSoundWaveGun ? Color.PURPLE : (piercingBullets ? Color.DEEPSKYBLUE : Color.GOLD),
                piercingBullets, piercingBullets, isSoundWaveGun));
        resetCooldown();
    }
}