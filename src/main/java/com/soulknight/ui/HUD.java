package com.soulknight.ui;

import com.soulknight.entity.Player;
import com.soulknight.level.LevelManager;
import com.soulknight.mission.MissionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public final class HUD {

    @FXML
    private Label hpLabel;

    @FXML
    private Label levelBannerLabel;

    @FXML
    private Label missionTitleLabel;

    @FXML
    private Label missionProgressLabel;

    @FXML
    private Label entitiesLabel;

    @FXML
    private Label weaponLabel;

    public void updateData(Player player, LevelManager levelManager, MissionManager missionManager,
                           int enemyCount, int itemCount) {
        if (hpLabel == null) return;

        hpLabel.setText("HP: " + player.getHealth() + "/" + player.getMaxHealth());
        levelBannerLabel.setText(levelManager.getLevelBanner());
        missionTitleLabel.setText("Mission: " + missionManager.getMissionTitle());
        missionProgressLabel.setText("Progress: " + missionManager.getMissionProgress());
        entitiesLabel.setText("Enemies: " + enemyCount + "  Items: " + itemCount);
        weaponLabel.setText("Weapon: " + player.getWeaponName());
    }
}