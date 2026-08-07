package com.soulknight.buff;

import com.soulknight.engine.GameWorld;
import com.soulknight.entity.Enemy;
import com.soulknight.entity.Player;
import javafx.scene.canvas.GraphicsContext;

/**
 * Quan li vòn doi hieu ung : kich hoat , update , het thoi gian , xoa hieu ung
 */
public abstract class Buff {

    private final BuffType type;
    private double remainingSeconds;
    private boolean active;
    private boolean finished;

    protected Buff(BuffType type) {
        this.type = type;
        this.remainingSeconds = type.getDurationSeconds();
    }

    public final void activate(Player player) {
        if (active) {
            return;
        }

        active = true;
        onActivate(player);

        if (type.isInstant()) {
            finished = true;
        }
    }

    public final void tick(Player player, double deltaSeconds) {
        if (!active || finished || type.isInstant()) {
            return;
        }

        remainingSeconds = Math.max(0.0, remainingSeconds - deltaSeconds);
        onTick(player, deltaSeconds);

        if (remainingSeconds <= 0.0) {
            finished = true;
        }
    }

    public final void remove(Player player) {
        if (!active) {
            return;
        }

        onRemove(player);
        active = false;
    }
    public void onDamageDealt(GameWorld world, Player player, Enemy target, int damage) {
        // Mac dinh khong lam gi
    }
    public void onEnemyKilled(GameWorld world, Player player, Enemy deadEnemy) {
        // Mac dinh buff khong xu ly khi Enemy chet
    }

    public void render(GraphicsContext graphicsContext, Player player) {
    }

    protected abstract void onActivate(Player player);

    protected void onTick(Player player, double deltaSeconds) {
    }

    protected abstract void onRemove(Player player);

    public BuffType getType() {
        return type;
    }

    public double getRemainingSeconds() {
        return remainingSeconds;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isFinished() {
        return finished;
    }
}