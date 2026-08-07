package com.soulknight.buff.effect.combat;

import com.soulknight.engine.Camera;
import com.soulknight.utils.Vector2D;
import javafx.scene.canvas.GraphicsContext;

import java.util.ArrayList;
import java.util.List;

/** Quan li cac effect combat ngan han. */
public final class CombatEffectManager {

    private final List<CombatEffect> effects = new ArrayList<>();

    public void spawnPurpleChainLightning(List<Vector2D> points) {
        if (points == null || points.size() < 2) return;
        effects.add(new PurpleChainLightningEffect(points));
    }

    public void update(double deltaSeconds) {
        for (CombatEffect effect : effects) {
            effect.update(deltaSeconds);
        }
        effects.removeIf(effect -> effect == null || effect.isFinished());
    }

    public void render(GraphicsContext graphicsContext, Camera camera) {
        if (graphicsContext == null || camera == null) return;

        for (CombatEffect effect : effects) {
            if (effect != null && !effect.isFinished()) {
                effect.render(graphicsContext, camera);
            }
        }
    }
    public void spawnDeathFireExplosion(Vector2D position) {
        if (position == null) {
            return;
        }
        effects.add(new DeathFireExplosionEffect(position));
    }
    public void spawnDragonBreath(Vector2D origin, double dirX, double dirY, double range, double halfAngle) {
        if (origin == null) {
            return;
        }

        effects.add(new DragonBreathEffect(origin, dirX, dirY, range, halfAngle));
    }
    public void spawnDragonExplosion(
            Vector2D position
    ) {
        if (position == null) {
            return;
        }
        effects.add(new DragonBreathExplosionEffect(position));
    }

    public void clear() {
        effects.clear();
    }
}