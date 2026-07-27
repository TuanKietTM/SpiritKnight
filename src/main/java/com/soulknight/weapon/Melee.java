package com.soulknight.weapon;

import com.soulknight.engine.GameWorld;
import com.soulknight.entity.Entity;
import com.soulknight.utils.SoundManager;
import com.soulknight.utils.Vector2D;

public final class Melee extends Weapon {

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

        world.damageEnemiesInRange(owner.getPosition(), range, getDamage());
        // Hieu ung chem truoc mat theo huong ngam, kich thuoc bam theo tam danh
        world.spawnMeleeSlash(owner.getPosition(), aimAngle, range);
        resetCooldown();
    }
}
