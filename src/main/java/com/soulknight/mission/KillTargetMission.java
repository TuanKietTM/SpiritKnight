package com.soulknight.mission;

import com.soulknight.entity.Boss;
import com.soulknight.entity.Enemy;
import com.soulknight.entity.EnemyArchetype;
import com.soulknight.item.Item;
import java.util.EnumSet;
import java.util.Objects;

public final class KillTargetMission implements Mission {

    private final String name;
    private final String description;
    private final int targetKills;
    private final EnumSet<EnemyArchetype> countedTypes;
    private int kills;

    public KillTargetMission(String name, String description, int targetKills, EnemyArchetype countedType) {
        this(name, description, targetKills, EnumSet.of(Objects.requireNonNull(countedType)));
    }

    public KillTargetMission(String name, String description, int targetKills, EnumSet<EnemyArchetype> countedTypes) {
        this.name = name;
        this.description = description;
        this.targetKills = targetKills;
        this.countedTypes = EnumSet.copyOf(countedTypes);
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
        if (countedTypes.contains(enemy.getArchetype()) && kills < targetKills) {
            kills++;
        }
    }

    @Override
    public void onItemCollected(Item item) {
    }

    @Override
    public void onBossDefeated(Boss boss) {
    }

    @Override
    public boolean isComplete() {
        return kills >= targetKills;
    }

    @Override
    public String getProgressText() {
        return kills + "/" + targetKills + " slain";
    }

    @Override
    public void reset() {
        kills = 0;
    }
}
