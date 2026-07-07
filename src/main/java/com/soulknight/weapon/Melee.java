package com.soulknight.weapon;

import com.soulknight.engine.GameWorld;
import com.soulknight.entity.Entity;
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

        world.damageEnemiesInRange(owner.getPosition(), range, getDamage());
        resetCooldown();
    }
}
