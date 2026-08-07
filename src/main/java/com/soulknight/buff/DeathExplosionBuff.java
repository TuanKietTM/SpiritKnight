package com.soulknight.buff;

import com.soulknight.engine.GameWorld;
import com.soulknight.entity.Enemy;
import com.soulknight.entity.Player;

public final class DeathExplosionBuff extends Buff {
    private static final double EXPLOSION_CHANCE = 0.50;

    public DeathExplosionBuff(BuffType type) {
        super(type);
    }

    @Override
    protected void onActivate(Player player) {
    }

    @Override
    protected void onRemove(Player player) {
    }

    @Override
    public void onEnemyKilled(GameWorld world, Player player, Enemy deadEnemy) {
        if (world == null || player == null || deadEnemy == null || deadEnemy.getPosition() == null) {
            return;
        }

        if (Math.random() > EXPLOSION_CHANCE) {
            return;
        }

        int explosionDamage = Math.max(1, (int) Math.round(getType().getValue()));

        world.triggerDeathExplosion(
                deadEnemy.getPosition().copy(),
                explosionDamage
        );
    }
}