package com.soulknight.event;

import com.soulknight.entity.Enemy;
import com.soulknight.item.Item;

public interface GameEventListener {

    default void onEnemyDefeated(Enemy enemy) {
    }
    default void onItemCollected(Item item) {
    }
}
