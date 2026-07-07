package com.soulknight.mission;

import com.soulknight.entity.Boss;
import com.soulknight.entity.Enemy;
import com.soulknight.item.Item;

public final class BossDefeatMission implements Mission {

    private final String name;
    private final String description;
    private boolean bossDefeated;

    public BossDefeatMission(String name, String description) {
        this.name = name;
        this.description = description;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void onEnemyDefeated(Enemy enemy) {
    }

    @Override
    public void onItemCollected(Item item) {
    }

    @Override
    public void onBossDefeated(Boss boss) {
        bossDefeated = true;
    }

    @Override
    public boolean isComplete() {
        return bossDefeated;
    }

    @Override
    public String getProgressText() {
        return bossDefeated ? "Grand Knight defeated" : "Grand Knight alive";
    }

    @Override
    public void reset() {
        bossDefeated = false;
    }
}
