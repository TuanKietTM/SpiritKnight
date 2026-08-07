package com.soulknight.buff.status;

import com.soulknight.engine.GameWorld;
import com.soulknight.entity.Enemy;
import javafx.scene.paint.Color;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

public final class EnemyBurnManager {
    private static final double BURN_DURATION = 3.0;
    private static final double TICK_INTERVAL = 0.5;
    private static final int DAMAGE_PER_TICK = 2;

    private final Map<Enemy, BurnState> burningEnemies = new IdentityHashMap<>();

    public void applyBurn(Enemy enemy) {
        if (enemy == null || !enemy.isAlive()) {
            return;
        }

        BurnState state = burningEnemies.get(enemy);

        if (state == null) {
            burningEnemies.put(enemy, new BurnState(BURN_DURATION));
        } else {
            state.remainingSeconds = BURN_DURATION;
        }
    }

    public void update(GameWorld world, double deltaSeconds) {
        if (world == null || deltaSeconds <= 0.0 || burningEnemies.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<Enemy, BurnState>> iterator = burningEnemies.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<Enemy, BurnState> entry = iterator.next();
            Enemy enemy = entry.getKey();
            BurnState state = entry.getValue();

            if (enemy == null || !enemy.isAlive() || enemy.getPosition() == null) {
                iterator.remove();
                continue;
            }

            state.remainingSeconds -= deltaSeconds;
            state.tickTimer -= deltaSeconds;

            while (state.tickTimer <= 0.0 && state.remainingSeconds > 0.0 && enemy.isAlive()) {
                int healthBefore = enemy.getHealth();
                enemy.takeDamage(DAMAGE_PER_TICK);

                int realDamage = healthBefore - enemy.getHealth();

                if (realDamage > 0) {
                    world.getFloatingTextManager().spawnCustom(
                            "-" + realDamage,
                            enemy.getPosition(),
                            Color.ORANGE
                    );
                }

                state.tickTimer += TICK_INTERVAL;
            }

            if (state.remainingSeconds <= 0.0 || !enemy.isAlive()) {
                iterator.remove();
            }
        }
    }

    public void clear() {
        burningEnemies.clear();
    }

    private static final class BurnState {
        private double remainingSeconds;
        private double tickTimer = TICK_INTERVAL;

        private BurnState(double remainingSeconds) {
            this.remainingSeconds = remainingSeconds;
        }
    }
    public boolean isBurning(Enemy enemy) {
        return enemy != null && burningEnemies.containsKey(enemy);
    }
}