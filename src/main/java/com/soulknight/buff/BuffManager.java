package com.soulknight.buff;

import com.soulknight.buff.effect.BuffVisualEffectManager;
import com.soulknight.engine.Camera;
import com.soulknight.entity.Player;
import com.soulknight.engine.GameWorld;
import com.soulknight.entity.Enemy;
import javafx.scene.canvas.GraphicsContext;

import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Quan li buff tren tung Player.
 * Chi quan li logic buff, phan hieu ung duoc giao cho BuffVisualEffectManager.
 * Ve hieu ung duoc chia lam 2 phan phia truoc va phia sau
 */
public class BuffManager {

    private final Player player;

    private final Map<BuffType, Buff> activeBuffs =
            new EnumMap<>(BuffType.class);

    // Quan li hieu ung hinh anh cua buff
    private final BuffVisualEffectManager visualEffectManager;

    public BuffManager(Player player) {
        this.player = player;
        this.visualEffectManager = new BuffVisualEffectManager(player);
    }

    /**
     * Kich hoat buff.
     */
    public void activate(BuffType type) {
        if (type == null) {
            return;
        }

        Buff oldBuff = activeBuffs.remove(type);

        if (oldBuff != null) {
            oldBuff.remove(player);
            visualEffectManager.stop(type);
        }

        Buff newBuff = type.createBuff();
        newBuff.activate(player);

        /*
         * Heal la instant buff nen se khong dua vao activeBuffs.
         */
        if (!newBuff.isFinished()) {
            activeBuffs.put(type, newBuff);
            visualEffectManager.start(type);
        }
    }

    /**
     * Update buff moi frame.
     */
    public void tick(double deltaSeconds) {
        Iterator<Map.Entry<BuffType, Buff>> iterator =
                activeBuffs.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BuffType, Buff> entry = iterator.next();
            BuffType type = entry.getKey();
            Buff buff = entry.getValue();
            buff.tick(player, deltaSeconds);
            if (buff.isFinished()) {
                buff.remove(player);
                visualEffectManager.stop(type);
                iterator.remove();
            }
        }

        visualEffectManager.update(activeBuffs, deltaSeconds);
    }

    /**
     * Render buff effect. render 2 nua cho hieu ung de dam bao quy tac Y-posision
     * tao cam giac cho chan thuc
     */
    public void renderBehind(
            GraphicsContext graphicsContext,
            Camera camera
    ) {
        visualEffectManager.renderBehind(graphicsContext, camera, activeBuffs);
    }

    public void renderFront(GraphicsContext graphicsContext, Camera camera) {
        visualEffectManager.renderFront(graphicsContext, camera, activeBuffs
        );
    }
    /**
     * Goi khi Player bi trung don.
     */
    public void notifyPlayerHit() {
        visualEffectManager.notifyPlayerHit();
    }
    public void notifyDamageDealt(
            GameWorld world,
            Enemy target,
            int damage
    ) {
        if (world == null
                || target == null
                || damage <= 0) {
            return;
        }

        /*
         * Tao ban copy de Buff co the thay doi trang thai
         * ma khong anh huong iterator.
         */
        for (Buff buff : List.copyOf(activeBuffs.values())) {
            if (buff == null || buff.isFinished()) {
                continue;
            }

            buff.onDamageDealt(world, player, target, damage);
        }
    }

    public boolean isActive(BuffType type) {
        return activeBuffs.containsKey(type);
    }

    public Buff getActiveBuff(BuffType type) {
        return activeBuffs.get(type);
    }

    public Map<BuffType, Buff> getActiveBuffs() {
        return Map.copyOf(activeBuffs);
    }

    /**
     * Xoa toan bo buff.
     */
    public void clear() {

        for (Buff buff : activeBuffs.values()) {
            buff.remove(player);
        }

        activeBuffs.clear();

        visualEffectManager.clear();
    }
}