package com.soulknight.ui;

import com.soulknight.engine.GameWorld;
import com.soulknight.engine.TouchpadJoystick;
import com.soulknight.entity.Player;
import com.soulknight.level.LevelManager;
import com.soulknight.mission.MissionManager;
import com.soulknight.utils.Vector2D;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;

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

    @FXML
    private StackPane joystickContainer;
    @FXML
    private Circle joystickThumb;

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

    // Cập nhật chữ ký hàm nhận thêm GameWorld để lấy dữ liệu Joystick
    public void updateData(GameWorld world, Player player, LevelManager levelManager, MissionManager missionManager,
                           int enemyCount, int itemCount) {
        if (hpLabel == null) return;

        // 1. Cập nhật HP
        if (player != null) {
            int currentHp = player.getHealth();
            int maxHp = player.getMaxHealth();
            hpLabel.setText(currentHp + "/" + maxHp);
            hpBar.setProgress(maxHp > 0 ? (double) currentHp / maxHp : 0.0);

            // Cập nhật tên vũ khí
            if (player.getWeaponName() != null && weaponLabel != null) {
                weaponLabel.setText(player.getWeaponName().toUpperCase());
            }
        }

        // 2. Cập nhật giáp (Shield)
        if (shieldLabel != null && shieldBar != null) {
            int currentShield = 6;
            int maxShield = 6;
            shieldLabel.setText(currentShield + "/" + maxShield);
            shieldBar.setProgress((double) currentShield / maxShield);
        }

        // 3. Cập nhật Mana
        if (manaLabel != null && manaBar != null) {
            int currentMana = 200;
            int maxMana = 200;
            manaLabel.setText(currentMana + "/" + maxMana);
            manaBar.setProgress((double) currentMana / maxMana);
        }

        // 4. Các thông tin nhiệm vụ & Level
        if (levelManager != null && levelBannerLabel != null) {
            levelBannerLabel.setText(levelManager.getLevelBanner());
        }
        if (missionManager != null) {
            if (missionTitleLabel != null) missionTitleLabel.setText("Mission: " + missionManager.getMissionTitle());
            if (missionProgressLabel != null) missionProgressLabel.setText("Progress: " + missionManager.getMissionProgress());
        }
        if (entitiesLabel != null) {
            entitiesLabel.setText(String.valueOf(itemCount));
        }

        // 5. Cập nhật vị trí Joystick
        updateJoystickUI(world);
    }

    private void updateJoystickUI(GameWorld world) {
        // An toàn: Nếu FXML chưa gán Joystick hoặc world null thì bỏ qua không báo lỗi
        if (joystickContainer == null || joystickThumb == null || world == null) return;

        if (world.getInputHandler() == null) return;

        TouchpadJoystick joystick = world.getInputHandler().getTouchpadJoystick();

        if (joystick != null && world.getInputHandler().isTouchpadModeEnabled() && joystick.isActive()) {
            joystickContainer.setVisible(true);

            Vector2D dir = joystick.getMoveDirection();
            double maxOffset = 35.0; // Độ lệch tối đa của nút Joystick

            joystickThumb.setTranslateX(dir.getX() * maxOffset);
            joystickThumb.setTranslateY(dir.getY() * maxOffset);
        } else {
            // Trả nút về vị trí trung tâm khi không thao tác
            joystickThumb.setTranslateX(0.0);
            joystickThumb.setTranslateY(0.0);
        }
    }
}