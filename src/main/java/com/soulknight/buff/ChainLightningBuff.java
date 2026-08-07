package com.soulknight.buff;

import com.soulknight.engine.GameWorld;
import com.soulknight.entity.Enemy;
import com.soulknight.entity.Player;
import com.soulknight.utils.Vector2D;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Buff set chuoi.
 *
 * Khi Player gay damage:
 * - Neu cooldown da san sang, set se kich hoat.
 * - Gay them damage set len muc tieu.
 * - Chain toi toi da 2 ke dich gan muc tieu.
 * - Sau khi kich hoat se bat dau cooldown.
 */
public final class ChainLightningBuff extends Buff {

    private static final double COOLDOWN_SECONDS = 2.5;
    private static final double CHAIN_RADIUS = 135.0;
    private static final int MAX_EXTRA_TARGETS = 2;

    private double cooldownRemaining;

    public ChainLightningBuff(BuffType type) {
        super(type);
    }

    @Override
    protected void onActivate(Player player) {
        cooldownRemaining = 0.0;
    }

    /**
     * Khong override tick() vi tick() trong Buff la final.
     * Buff.tick() se tu goi onTick() moi frame.
     */
    @Override
    protected void onTick(Player player, double deltaSeconds) {
        if (cooldownRemaining <= 0.0) {
            return;
        }

        cooldownRemaining = Math.max(0.0, cooldownRemaining - deltaSeconds);
    }

    @Override
    protected void onRemove(Player player) {
        cooldownRemaining = 0.0;
    }

    @Override
    public void onDamageDealt(GameWorld world, Player player, Enemy primaryTarget, int damage) {
        if (world == null || player == null || primaryTarget == null || primaryTarget.getPosition() == null || damage <= 0) {
            return;
        }

        // Dang cooldown thi khong kich hoat
        if (cooldownRemaining > 0.0) {
            return;
        }

        List<Enemy> targets = findTargets(world, primaryTarget);

        if (targets.isEmpty()) {
            return;
        }

        int lightningDamage =
                Math.max(1, (int) Math.round(getType().getValue()));

        /*
         * Gay damage set len enemy
         */
        for (Enemy enemy : targets) {
            if (enemy == null || !enemy.isAlive() || enemy.getPosition() == null) {
                continue;
            }

            int healthBefore = enemy.getHealth();
            enemy.takeDamage(lightningDamage);

            int realDamage = healthBefore - enemy.getHealth();

            if (realDamage > 0) {
                world.getFloatingTextManager().spawnCustom("-" + realDamage,
                                enemy.getPosition(), javafx.scene.paint.Color.VIOLET);
            }
        }

        // Tao effect set tim
        world.spawnChainLightning(player.getPosition(), targets);

        // Bat dau cooldown
        cooldownRemaining = COOLDOWN_SECONDS;
    }

    /**
     * Tim muc tieu chinh + toi da 2 Enemy gan nhat.
     */
    private List<Enemy> findTargets(GameWorld world, Enemy primaryTarget) {
        List<Enemy> result = new ArrayList<>();

        Vector2D origin = primaryTarget.getPosition();

        if (origin == null) {
            return result;
        }

        /*
         * Neu muc tieu chinh van song sau don danh goc,
         * no se nhan them damage set.
         */
        if (primaryTarget.isAlive()) {
            result.add(primaryTarget);
        }

        List<Enemy> nearby = new ArrayList<>();

        for (Enemy enemy : world.getEnemies()) {
            if (enemy == null || enemy == primaryTarget || !enemy.isAlive() || enemy.getPosition() == null) {
                continue;
            }

            double distance = origin.distance(enemy.getPosition());

            if (distance <= CHAIN_RADIUS) {
                nearby.add(enemy);
            }
        }

        /*
         * Sap xep Enemy gan nhat truoc.
         */
        nearby.sort(Comparator.comparingDouble(enemy -> origin.distance(enemy.getPosition())));

        int count = Math.min(MAX_EXTRA_TARGETS, nearby.size());

        for (int i = 0; i < count; i++) {
            result.add(nearby.get(i));
        }

        return result;
    }

    public double getCooldownRemaining() {
        return cooldownRemaining;
    }

    public double getCooldownSeconds() {
        return COOLDOWN_SECONDS;
    }

    public boolean isCooldownReady() {
        return cooldownRemaining <= 0.0;
    }
}