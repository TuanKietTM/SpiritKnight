package com.soulknight.mission;

import com.soulknight.entity.Boss;
import com.soulknight.entity.Enemy;
import com.soulknight.item.Item;

public interface Mission {

    String getName();

    String getDescription();

    void onEnemyDefeated(Enemy enemy);

    void onItemCollected(Item item);

    void onBossDefeated(Boss boss);

    boolean isComplete();

    String getProgressText();

    void reset();
}
