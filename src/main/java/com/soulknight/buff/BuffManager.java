package com.soulknight.buff;

import com.soulknight.entity.Player;
import javafx.scene.canvas.GraphicsContext;

import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;
/**
 * quan li buff tren tung player
 */
public class BuffManager {

    private final Player player;

    private final Map<BuffType, Buff> activeBuffs =
            new EnumMap<>(BuffType.class);

    public BuffManager(Player player) {
        this.player = player;
    }

    public void activate(BuffType type) {
        if (type == null) {
            return;
        }

        Buff oldBuff = activeBuffs.remove(type);

        if (oldBuff != null) {
            oldBuff.remove(player);
        }

        Buff newBuff = type.createBuff();
        newBuff.activate(player);

        if (!newBuff.isFinished()) {
            activeBuffs.put(type, newBuff);
        }
    }

    public void tick(double deltaSeconds) {
        Iterator<Map.Entry<BuffType, Buff>> iterator =
                activeBuffs.entrySet().iterator();

        while (iterator.hasNext()) {
            Buff buff = iterator.next().getValue();

            buff.tick(player, deltaSeconds);

            if (buff.isFinished()) {
                buff.remove(player);
                iterator.remove();
            }
        }
    }

    public void render(GraphicsContext graphicsContext) {
        for (Buff buff : activeBuffs.values()) {
            buff.render(graphicsContext, player);
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

    public void clear() {
        for (Buff buff : activeBuffs.values()) {
            buff.remove(player);
        }

        activeBuffs.clear();
    }
}