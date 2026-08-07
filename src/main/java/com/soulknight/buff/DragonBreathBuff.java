package com.soulknight.buff;

import com.soulknight.entity.Player;

public final class DragonBreathBuff extends Buff {
    private static final double ATTACK_INTERVAL = 3.0;
    private double attackTimer;

    public DragonBreathBuff(BuffType type) {
        super(type);
    }

    @Override
    protected void onActivate(Player player) {
        attackTimer = 0.0;
    }

    @Override
    protected void onTick(Player player, double deltaSeconds) {
        if (player == null || !player.isAlive() || deltaSeconds <= 0.0) {
            return;
        }

        attackTimer -= deltaSeconds;

        if (attackTimer > 0.0) {
            return;
        }

        attackTimer += ATTACK_INTERVAL;

        if (attackTimer <= 0.0) {
            attackTimer = ATTACK_INTERVAL;
        }

        player.requestDragonBreath();
    }

    @Override
    protected void onRemove(Player player) {
        attackTimer = 0.0;
    }

    public double getAttackTimer() {
        return Math.max(0.0, attackTimer);
    }

    public double getAttackInterval() {
        return ATTACK_INTERVAL;
    }
}