package com.soulknight.buff.effect;

import com.soulknight.buff.Buff;
import com.soulknight.buff.BuffType;
import com.soulknight.engine.Camera;
import com.soulknight.entity.Player;
import javafx.scene.canvas.GraphicsContext;

import java.util.EnumMap;
import java.util.Map;

public final class BuffVisualEffectManager {

    private final Player player;

    private final Map<BuffType, BuffVisualEffect> effects =
            new EnumMap<>(BuffType.class);

    public BuffVisualEffectManager(Player player) {
        this.player = player;
    }

    public void start(BuffType type) {
        if (type == null || player == null) {
            return;
        }

        stop(type);

        BuffVisualEffect effect =
                BuffVisualEffectFactory.create(type);

        if (effect == null) {
            return;
        }

        effect.start(player);
        effects.put(type, effect);
    }

    public void update(Map<BuffType, Buff> activeBuffs,double deltaSeconds) {
        if (activeBuffs == null || effects.isEmpty()) {
            return;
        }

        for (Map.Entry<BuffType, BuffVisualEffect> entry : effects.entrySet()) {
            BuffType type = entry.getKey();
            BuffVisualEffect effect = entry.getValue();
            Buff buff = activeBuffs.get(type);

            if (buff == null || buff.isFinished() || !effect.isActive()) {
                continue;
            }
            effect.update(player, deltaSeconds, buff.getRemainingSeconds(), type.getDurationSeconds());
        }
    }

    public void renderBehind(GraphicsContext graphicsContext, Camera camera, Map<BuffType, Buff> activeBuffs) {
        if (graphicsContext == null || camera == null || activeBuffs == null || effects.isEmpty()) {
            return;
        }

        for (Map.Entry<BuffType, BuffVisualEffect> entry : effects.entrySet()) {
            BuffType type = entry.getKey();
            BuffVisualEffect effect = entry.getValue();
            Buff buff = activeBuffs.get(type);

            if (buff == null || buff.isFinished() || effect == null || !effect.isActive()) {
                continue;
            }

            effect.renderBehind(graphicsContext, camera, player, buff.getRemainingSeconds(), type.getDurationSeconds());
        }
    }
    public void renderFront(GraphicsContext graphicsContext,
            Camera camera, Map<BuffType, Buff> activeBuffs) {
        if (graphicsContext == null || camera == null || activeBuffs == null || effects.isEmpty()) {
            return;
        }

        for (Map.Entry<BuffType, BuffVisualEffect> entry : effects.entrySet()) {

            BuffType type = entry.getKey();
            BuffVisualEffect effect = entry.getValue();
            Buff buff = activeBuffs.get(type);

            if (buff == null || buff.isFinished() || effect == null || !effect.isActive()) {
                continue;
            }

            effect.renderFront(graphicsContext, camera, player,
                    buff.getRemainingSeconds(), type.getDurationSeconds());
        }
    }

    public void notifyPlayerHit() {
        for (BuffVisualEffect effect : effects.values()) {
            if (effect != null && effect.isActive()) {
                effect.notifyPlayerHit();
            }
        }
    }

    public void stop(BuffType type) {
        BuffVisualEffect oldEffect = effects.remove(type);

        if (oldEffect != null) {
            oldEffect.stop();
        }
    }

    public void clear() {
        for (BuffVisualEffect effect : effects.values()) {
            if (effect != null) {
                effect.stop();
            }
        }

        effects.clear();
    }
}