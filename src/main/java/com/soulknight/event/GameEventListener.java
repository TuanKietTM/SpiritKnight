package com.soulknight.event;

import com.soulknight.debuff.DebuffItem;
import com.soulknight.entity.Boss;
import com.soulknight.entity.Enemy;
import com.soulknight.item.Item;

public interface GameEventListener {

    default void onEnemyDefeated(Enemy enemy) {
    }

    void onDebuffSpawned(DebuffItem debuffItem);

    default void onItemCollected(Item item) {
    }

    default void onBossDefeated(Boss boss) {
    }
}
