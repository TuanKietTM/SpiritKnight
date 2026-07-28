package com.soulknight.weapon;

import com.soulknight.engine.GameWorld;
import com.soulknight.entity.Entity;
import com.soulknight.entity.Player;
import com.soulknight.utils.SoundManager;
import com.soulknight.utils.Vector2D;

public final class Melee extends Weapon {

    // Nua goc quet cua nhat chem (radian): tong pham vi 120 do huong ve muc tieu,
    // khop voi cung chem cua hieu ung slash_effect
    private static final double HALF_ARC = Math.toRadians(60.0);

    private final double range;

    public Melee(String name, int damage, double cooldownSeconds, double range) {
        super(name, damage, cooldownSeconds);
        this.range = range;
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
            SoundManager.getInstance().playSFX("Sword_Swing");
        }

        // Huong chem tu nhan vat den muc tieu ngam
        Vector2D direction = targetPosition.copy().subtract(owner.getPosition());
        double aimAngle = (direction.length() == 0.0)
                ? 0.0
                : Math.atan2(direction.getY(), direction.getX());

        // Chi gay sat thuong trong hinh quat huong ve muc tieu, khong quet 360 do
        world.damageEnemiesInArc(owner.getPosition(), aimAngle, range, HALF_ARC, getDamage());
        // Hieu ung chem bam theo nhan vat, kiem trong sprite sheet thay cho kiem dang cam
        world.spawnMeleeSlash(owner, aimAngle);
        // An kiem dang cam trong luc vung chem de khong ve 2 thanh kiem chong nhau
        if (owner instanceof Player player) {
            player.startMeleeSwing();
        }
        resetCooldown();
    }
}
