package com.soulknight.ui;

import com.soulknight.level.LevelManager;
import com.soulknight.mission.MissionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public final class LevelClearScreen {

    @FXML
    private Label lblLevelBanner;

    @FXML
    private Label lblMissionTitle;

    @FXML
    private Label lblProgress;

    public void updateData(LevelManager levelManager, MissionManager missionManager) {
        if (lblLevelBanner != null && levelManager != null) {
            lblLevelBanner.setText(levelManager.getLevelBanner());
        }
        if (lblMissionTitle != null && missionManager != null) {
            lblMissionTitle.setText(missionManager.getMissionTitle());
        }
        if (lblProgress != null && missionManager != null) {
            lblProgress.setText("Progress: " + missionManager.getMissionProgress());
        }
    }
}