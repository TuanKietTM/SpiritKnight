package com.soulknight.ui;

import com.soulknight.engine.GameWorld;
import com.soulknight.engine.TouchpadJoystick;
import com.soulknight.entity.Player;
import com.soulknight.level.LevelManager;
import com.soulknight.mission.MissionManager;
import com.soulknight.utils.Vector2D;
import com.soulknight.weapon.WeaponType;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;

import java.net.URL;

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

    // ImageView hiển thị icon vũ khí trên HUD
    @FXML
    private ImageView weaponIcon;

    @FXML
    private StackPane joystickContainer;
    @FXML
    private Circle joystickThumb;

    @FXML
    private Canvas minimapCanvas;
    private MinimapRenderer minimapRenderer;

    private Runnable onPauseRequested;
    private Runnable onWeaponSwitchRequested;
    private String currentWeaponImagePath = "";

    @FXML
    private void initialize() {
        if (minimapCanvas != null) {
            this.minimapRenderer = new MinimapRenderer(minimapCanvas);
        }
    }

    public void setOnPauseRequested(Runnable callback) {
        this.onPauseRequested = callback;
    }

    public void setOnWeaponSwitchRequested(Runnable callback) {
        this.onWeaponSwitchRequested = callback;
    }

    @FXML
    private void onPauseButtonClicked(ActionEvent event) {
        if (onPauseRequested != null) {
            onPauseRequested.run();
        }
    }

    @FXML
    private void onWeaponButtonClicked(MouseEvent event) {
        if (onWeaponSwitchRequested != null) {
            onWeaponSwitchRequested.run();
        }
    }

    public void updateData(GameWorld world, Player player, LevelManager levelManager, MissionManager missionManager,
                           int enemyCount, int itemCount) {
        if (hpLabel == null) return;

        if (player != null) {
            int currentHp = player.getHealth();
            int maxHp = player.getMaxHealth();
            hpLabel.setText(currentHp + "/" + maxHp);
            hpBar.setProgress(maxHp > 0 ? (double) currentHp / maxHp : 0.0);

            updateWeaponUI(player);
        }

        if (shieldLabel != null && shieldBar != null) {
            int currentShield = 6;
            int maxShield = 6;
            shieldLabel.setText(currentShield + "/" + maxShield);
            shieldBar.setProgress((double) currentShield / maxShield);
        }

        if (manaLabel != null && manaBar != null) {
            int currentMana = 200;
            int maxMana = 200;
            manaLabel.setText(currentMana + "/" + maxMana);
            manaBar.setProgress((double) currentMana / maxMana);
        }

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

        updateJoystickUI(world);

//      cap nhat render mini map
        updateMinimapUI(world, player);
    }

    private void updateMinimapUI(GameWorld world, Player player) {
        if (minimapCanvas == null) return;
        if (minimapRenderer == null) {
            minimapRenderer = new MinimapRenderer(minimapCanvas);
        }
        minimapRenderer.render(world, player);
    }

    private void updateWeaponUI(Player player) {
        String currentName = player.getWeaponName();
        if (currentName == null || currentName.isBlank()) return;

        if (weaponLabel != null) {
            weaponLabel.setText(currentName.toUpperCase());
        }

        if (weaponIcon == null) return;

        WeaponType matchedType = findWeaponTypeByName(currentName);

        String imagePath = null;
        if (matchedType != null) {
            imagePath = matchedType.getImagePath();
        }
        if (imagePath != null) {
            if (!imagePath.equals(currentWeaponImagePath)) {
                currentWeaponImagePath = imagePath;
                Image img = loadImage(imagePath);
                weaponIcon.setImage(img);
            }
        } else {
            currentWeaponImagePath = "";
        }

        weaponIcon.setSmooth(false);
        weaponIcon.setPreserveRatio(true);
    }

    // Tìm thông tin vũ khí trong WeaponType
    private WeaponType findWeaponTypeByName(String rawName) {
        if (rawName == null) return null;

        String normalizedInput = rawName.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();

        for (WeaponType type : WeaponType.values()) {
            String normalizedDisplayName = type.getDisplayName().replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
            if (normalizedDisplayName.equals(normalizedInput)) {
                return type;
            }

            String normalizedEnumName = type.name().replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
            if (normalizedEnumName.equals(normalizedInput)) {
                return type;
            }
        }

        return null;
    }

    private void updateJoystickUI(GameWorld world) {
        if (joystickContainer == null || joystickThumb == null || world == null) return;
        if (world.getInputHandler() == null) return;

        Vector2D dir = new Vector2D(0, 0);
        double maxOffset = 35.0;

        if (world.getInputHandler().isTouchpadModeEnabled()) {
            TouchpadJoystick joystick = world.getInputHandler().getTouchpadJoystick();
            if (joystick != null && joystick.isActive()) {
                dir = joystick.getMoveDirection();
            }
        } else {
            double dx = 0.0;
            double dy = 0.0;

            if (world.getInputHandler().isDown(javafx.scene.input.KeyCode.W) || world.getInputHandler().isDown(javafx.scene.input.KeyCode.UP)) {
                dy -= 1.0;
            }
            if (world.getInputHandler().isDown(javafx.scene.input.KeyCode.S) || world.getInputHandler().isDown(javafx.scene.input.KeyCode.DOWN)) {
                dy += 1.0;
            }
            if (world.getInputHandler().isDown(javafx.scene.input.KeyCode.A) || world.getInputHandler().isDown(javafx.scene.input.KeyCode.LEFT)) {
                dx -= 1.0;
            }
            if (world.getInputHandler().isDown(javafx.scene.input.KeyCode.D) || world.getInputHandler().isDown(javafx.scene.input.KeyCode.RIGHT)) {
                dx += 1.0;
            }

            Vector2D moveDir = new Vector2D(dx, dy);
            if (moveDir.length() > 0.0) {
                moveDir.normalize();
                dir = moveDir;
            }
        }

        if (dir.length() > 0.0) {
            joystickContainer.setVisible(true);
            joystickThumb.setTranslateX(dir.getX() * maxOffset);
            joystickThumb.setTranslateY(dir.getY() * maxOffset);
        } else {
            joystickThumb.setTranslateX(0.0);
            joystickThumb.setTranslateY(0.0);
        }
    }

    private Image loadImage(String path) {
        if (path == null || path.isBlank()) return null;
        URL resource = getClass().getResource(path);
        if (resource == null) return null;
        return new Image(resource.toExternalForm(), false);
    }
}