package com.soulknight.mission;

import com.soulknight.entity.Boss;
import com.soulknight.entity.Enemy;
import com.soulknight.item.EnergyCrystal;
import com.soulknight.item.Item;

public final class CollectItemMission implements Mission {

    private final String name;
    private final String description;
    private final int targetItems;
    private int collectedItems;

    public CollectItemMission(String name, String description, int targetItems) {
        this.name = name;
        this.description = description;
        this.targetItems = targetItems;
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
        if (item instanceof EnergyCrystal && collectedItems < targetItems) {
            collectedItems++;
        }
    }

    @Override
    public void onBossDefeated(Boss boss) {
    }

    @Override
    public boolean isComplete() {
        return collectedItems >= targetItems;
    }

    @Override
    public String getProgressText() {
        return collectedItems + "/" + targetItems + " collected";
    }

    @Override
    public void reset() {
        collectedItems = 0;
    }
}
