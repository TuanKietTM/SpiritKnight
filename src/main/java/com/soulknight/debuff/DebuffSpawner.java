package com.soulknight.debuff;

import com.soulknight.event.GameEventListener;
import com.soulknight.map.MapManager;
import com.soulknight.map.Room;
import com.soulknight.utils.Vector2D;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DebuffSpawner {

    private static final Random RANDOM = new Random();
    private static final double DEBUFF_ITEM_RADIUS = 14.0; // Bán kính va chạm của DebuffItem

    /**
     * Sinh ra cac debuff ngau nhien
     */
    public static List<DebuffItem> spawnWaveDebuffs(MapManager mapManager, Room room, int count, GameEventListener listener) {
        List<DebuffItem> debuffs = new ArrayList<>();

        if (mapManager == null || room == null || count <= 0) {
            return debuffs;
        }

        DebuffType[] types = DebuffType.values();

        for (int i = 0; i < count; i++) {
//            dung thuat toan kiem tra o nao co the di duoc
            Vector2D spawnPos = mapManager.findRandomWalkablePositionInRoom(room, RANDOM, DEBUFF_ITEM_RADIUS);

            if (spawnPos != null) {
                DebuffType randomType = types[RANDOM.nextInt(types.length)];
                debuffs.add(new DebuffItem(randomType, spawnPos, listener));
            }
        }

        return debuffs;
    }
    public static DebuffItem spawnAtPosition(Vector2D position, GameEventListener listener) {
        if (position == null) return null;
        DebuffType[] types = DebuffType.values();
        DebuffType randomType = types[RANDOM.nextInt(types.length)];
        return new DebuffItem(randomType, position.copy(), listener);
    }
}