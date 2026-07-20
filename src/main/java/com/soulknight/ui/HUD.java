package com.soulknight.ui;

import com.soulknight.entity.Player;
import com.soulknight.level.LevelManager;
import com.soulknight.mission.MissionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.event.ActionEvent;

public final class HUD {

    @FXML
    private ProgressBar hpBar;
    @FXML
    private Label hpLabel;

    @FXML
    private ProgressBar shieldBar;
    @FXML
    private Label shieldLabel;

    @FXML
    private ProgressBar manaBar;
    @FXML
    private Label manaLabel;

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

    private Runnable onPauseRequested;

    public void setOnPauseRequested(Runnable callback) {
        this.onPauseRequested = callback;
    }

    @FXML
    private void onPauseButtonClicked(ActionEvent event) {
        if (onPauseRequested != null) {
            onPauseRequested.run();
        }
    }

    public void updateData(Player player, LevelManager levelManager, MissionManager missionManager,
                           int enemyCount, int itemCount) {
        if (hpLabel == null) return;
//        cap nhat HP
        int currentHp = player.getHealth();
        int maxHp = player.getMaxHealth();
        hpLabel.setText(currentHp + "/" + maxHp);
        hpBar.setProgress(maxHp > 0 ? (double) currentHp / maxHp : 0.0);

//        cap nhat chi so giap
        int currentShield = 6;
        int maxShield = 6;
        shieldLabel.setText(currentShield + "/" + maxShield);
        shieldBar.setProgress((double) currentShield / maxShield);

//        cap nhat mana
        int currentMana = 200;
        int maxMana = 200;
        manaLabel.setText(currentMana + "/" + maxMana);
        manaBar.setProgress((double) currentMana / maxMana);

//       thong tin khac
        levelBannerLabel.setText(levelManager.getLevelBanner());
        missionTitleLabel.setText("Mission: " + missionManager.getMissionTitle());
        missionProgressLabel.setText("Progress: " + missionManager.getMissionProgress());

        entitiesLabel.setText(String.valueOf(itemCount));

//        vu khi va ten trong vong tron
        weaponLabel.setText(player.getWeaponName().toUpperCase());
    }
}