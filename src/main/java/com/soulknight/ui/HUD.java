package com.soulknight.ui;

import com.soulknight.entity.Player;
import com.soulknight.level.LevelManager;
import com.soulknight.mission.MissionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.event.ActionEvent;

public final class HUD {

    // Thanh HP
    @FXML
    private ProgressBar hpBar;
    @FXML
    private Label hpLabel;

    // Thanh Shield (Tạm thời map với HP hoặc một giá trị ảo nếu Player chưa có thuộc tính shield)
    @FXML
    private ProgressBar shieldBar;
    @FXML
    private Label shieldLabel;

    // Thanh Mana (Năng lượng)
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
    private Label entitiesLabel; // Dùng làm nhãn Coin hiển thị tiền xu

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

        // 1. Cập nhật chỉ số HP
        int currentHp = player.getHealth();
        int maxHp = player.getMaxHealth();
        hpLabel.setText(currentHp + "/" + maxHp);
        hpBar.setProgress(maxHp > 0 ? (double) currentHp / maxHp : 0.0);

        // 2. Cập nhật chỉ số Giáp (Giả lập nếu Player chưa có thuộc tính Shield)
        // Nếu Player của bạn có getShield() và getMaxShield(), hãy đổi sang gọi thực tế nhé!
        int currentShield = 6;
        int maxShield = 6;
        shieldLabel.setText(currentShield + "/" + maxShield);
        shieldBar.setProgress((double) currentShield / maxShield);

        // 3. Cập nhật chỉ số Mana (Giả lập năng lượng)
        int currentMana = 200;
        int maxMana = 200;
        manaLabel.setText(currentMana + "/" + maxMana);
        manaBar.setProgress((double) currentMana / maxMana);

        // 4. Cập nhật các thông tin phụ khác
        levelBannerLabel.setText(levelManager.getLevelBanner());
        missionTitleLabel.setText("Mission: " + missionManager.getMissionTitle());
        missionProgressLabel.setText("Progress: " + missionManager.getMissionProgress());

        // Map số lượng Items thu thập được làm chỉ số Coins của người chơi
        entitiesLabel.setText(String.valueOf(itemCount));

        // Tên vũ khí hiển thị bên trong vòng tròn
        weaponLabel.setText(player.getWeaponName().toUpperCase());
    }
}