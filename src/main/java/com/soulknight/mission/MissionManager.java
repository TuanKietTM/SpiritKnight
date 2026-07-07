package com.soulknight.mission;

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

    public void setMissionCompletedListener(Runnable missionCompletedListener) {
        this.missionCompletedListener = Objects.requireNonNull(missionCompletedListener);
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

    @Override
    public void onBossDefeated(Boss boss) {
        if (currentMission == null) {
            return;
        }
        currentMission.onBossDefeated(boss);
        checkCompletion();
    }

    public boolean isMissionComplete() {
        return currentMission != null && currentMission.isComplete();
    }

    public String getMissionTitle() {
        return currentMission == null ? "No mission" : currentMission.getName();
    }

    public String getMissionDescription() {
        return currentMission == null ? "" : currentMission.getDescription();
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
