package com.soulknight.debuff;
import com.soulknight.event.GameEventListener;
import com.soulknight.utils.Vector2D;
public class DebuffSpawner {


    public static DebuffItem spawnTypeAtIndex(int index, Vector2D position) {
        return spawnTypeAtIndex(index, position, null);
    }

    public static DebuffItem spawnTypeAtIndex(int index, Vector2D position, GameEventListener listener) {
        if (position == null) return null;
        DebuffType[] types = DebuffType.values();
        if (types.length == 0) return null;
        int safeIndex = Math.abs(index) % types.length;
        DebuffType selectedType = types[safeIndex];

        return new DebuffItem(selectedType, position.copy(), listener);
    }
}