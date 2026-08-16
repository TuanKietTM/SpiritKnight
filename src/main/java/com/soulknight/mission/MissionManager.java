package com.soulknight.mission;

import com.soulknight.debuff.DebuffItem;
import com.soulknight.entity.Boss;
import com.soulknight.entity.Enemy;
import com.soulknight.event.GameEventListener;
import com.soulknight.item.Item;
import java.util.Objects;

public final class MissionManager implements GameEventListener {

    private Mission currentMission;
    private Runnable missionCompletedListener = () -> {
    };
    private boolean completionNotified;

    public void setMission(Mission mission) {
        this.currentMission = Objects.requireNonNull(mission);
        this.currentMission.reset();
        completionNotified = false;
    }

    public Mission getCurrentMission() {
        return currentMission;
    }


    @Override
    public void onEnemyDefeated(Enemy enemy) {
        if (currentMission == null) {
            return;
        }
        currentMission.onEnemyDefeated(enemy);
        checkCompletion();
    }

    @Override
    public void onItemCollected(Item item) {
        if (currentMission == null) {
            return;
        }
        currentMission.onItemCollected(item);
        checkCompletion();
    }

    public void onDebuffSpawned(DebuffItem debuffItem) {
    }

    public String getMissionTitle() {
        return currentMission == null ? "No mission" : currentMission.getName();
    }

    public String getMissionProgress() {
        return currentMission == null ? "" : currentMission.getProgressText();
    }

    private void checkCompletion() {
        if (!completionNotified && currentMission != null && currentMission.isComplete()) {
            completionNotified = true;
            missionCompletedListener.run();
        }
    }
}
