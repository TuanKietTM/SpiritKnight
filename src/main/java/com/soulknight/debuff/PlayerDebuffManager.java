package com.soulknight.debuff;

import com.soulknight.debuff.render.*;
import com.soulknight.engine.Camera;
import com.soulknight.entity.Player;
import javafx.scene.canvas.GraphicsContext;

import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;

/**
 * quan li cac debuff tac dong len player
 */
public class PlayerDebuffManager {

    private final Map<DebuffType, Double> activeDebuffs = new EnumMap<>(DebuffType.class);
    private final Map<DebuffType, DebuffVisualEffect> visualEffects = new EnumMap<>(DebuffType.class);
    private double poisonTimer = 0.0;

    public PlayerDebuffManager() {
        visualEffects.put(DebuffType.POISON, new PoisonVisualEffect());
        visualEffects.put(DebuffType.SLOW, new SlowVisualEffect());
        visualEffects.put(DebuffType.CONFUSION, new ConfusionVisualEffect());
        visualEffects.put(DebuffType.WEAKNESS, new WeaknessVisualEffect());
        visualEffects.put(DebuffType.FREEZE, new FreezeVisualEffect());
    }

    public void applyDebuff(DebuffType type) {
        activeDebuffs.put(type, type.getDuration());
    }

    public boolean hasDebuff(DebuffType type) {
        return activeDebuffs.containsKey(type);
    }

    public void update(Player player, double deltaSeconds) {
        if (activeDebuffs.isEmpty() || player == null || !player.isAlive()) return;
        for (DebuffType type : activeDebuffs.keySet()) {
            DebuffVisualEffect effect = visualEffects.get(type);
            if (effect != null) {
                effect.update(deltaSeconds);
            }
        }

        Iterator<Map.Entry<DebuffType, Double>> iterator = activeDebuffs.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<DebuffType, Double> entry = iterator.next();
            DebuffType type = entry.getKey();
            double timeLeft = entry.getValue() - deltaSeconds;

            if (type == DebuffType.POISON) {
                poisonTimer += deltaSeconds;
                if (poisonTimer >= 1.0) { // moi giay gay 2 sat thuong
                    player.takeDamage(2);
                    poisonTimer = 0.0;
                }
            }

            if (timeLeft <= 0) {
                iterator.remove();
            } else {
                entry.setValue(timeLeft);
            }
        }
    }
// dieu chinh toc do
    public double getSpeedModifier() {
        if (hasDebuff(DebuffType.FREEZE)) {
            return 0.0;
        }
        return hasDebuff(DebuffType.SLOW) ? 0.5 : 1.0;
    }
    public boolean canAttack() {
        return !hasDebuff(DebuffType.FREEZE);
    }

    public double getMovementDirectionMultiplier() {
        return hasDebuff(DebuffType.CONFUSION) ? -1.0 : 1.0;
    }

    public int modifyIncomingDamage(int rawDamage) {
        return hasDebuff(DebuffType.WEAKNESS) ? rawDamage * 2 : rawDamage;
    }
//co che uy quyen / ve len tren player , chia lam 2 phan ve phia truoc va sau ( de phuc vu cho ve hinh tron cua confusion)
    public void renderBack(GraphicsContext gc, Camera camera, Player player) {
        if (activeDebuffs.isEmpty() || player == null) return;

        for (DebuffType type : activeDebuffs.keySet()) {
            DebuffVisualEffect effect = visualEffects.get(type);
            if (effect != null) {
                if (effect instanceof ConfusionVisualEffect confusion) {
                    confusion.renderBack(gc, camera, player);
                }
            }
        }
    }

    public void renderFront(GraphicsContext gc, Camera camera, Player player) {
        if (activeDebuffs.isEmpty() || player == null) return;
        for (DebuffType type : activeDebuffs.keySet()) {
            DebuffVisualEffect effect = visualEffects.get(type);
            if (effect != null) {
                if (effect instanceof ConfusionVisualEffect confusion) {
                    confusion.renderFront(gc, camera, player);
                } else {
                    effect.render(gc, camera, player);
                }
            }
        }
    }
}