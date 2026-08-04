package com.soulknight.ui;

import com.soulknight.buff.Buff;
import com.soulknight.buff.BuffInventoryManager;
import com.soulknight.buff.BuffType;
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
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import java.util.function.Consumer;

import java.net.URL;
import java.util.EnumMap;
import java.util.Map;

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
    private Label entitiesLabel;

    @FXML
    private Label weaponLabel;

    @FXML
    private Label goldLabel;

    @FXML
    private Label gemsLabel;

    @FXML
    private Label scoreLabel;

    @FXML
    private ImageView weaponIcon;

    @FXML
    private StackPane joystickContainer;

    @FXML
    private Circle joystickThumb;

    @FXML
    private Canvas minimapCanvas;

    @FXML
    private ImageView hpIcon;

    @FXML
    private ImageView shieldIcon;

    @FXML
    private ImageView manaIcon;

    @FXML
    private ImageView goldIcon;

    @FXML
    private ImageView gemIcon;

    @FXML
    private ImageView scoreIcon;

    @FXML
    private HBox topBuffContainer;

    @FXML
    private StackPane activeBuffTimerBox;

    @FXML
    private ImageView activeBuffIcon;

    @FXML
    private Label activeBuffTimerLabel;

    private MinimapRenderer minimapRenderer;
    private Runnable onPauseRequested;
    private Runnable onWeaponSwitchRequested;
    private String currentWeaponImagePath = "";
    private final Map<BuffType, StackPane> buffNodes = new EnumMap<>(BuffType.class);
    private Consumer<BuffType> onBuffUseRequested;

    @FXML
    private void initialize() {
        if (minimapCanvas != null) {
            minimapRenderer = new MinimapRenderer(minimapCanvas);
        }
        hpIcon.setImage(loadHUDIcon("/assets/icon/hp.png"));
        shieldIcon.setImage(loadHUDIcon("/assets/icon/shield.png"));
        manaIcon.setImage(loadHUDIcon("/assets/icon/mana.png"));

        goldIcon.setImage(loadHUDIcon("/assets/icon/gold.png"));
        gemIcon.setImage(loadHUDIcon("/assets/icon/gem.png"));
        scoreIcon.setImage(loadHUDIcon("/assets/icon/score.png"));
    }
    private Image loadHUDIcon(String path) {
        return new Image(getClass().getResourceAsStream(path));
    }

    public void setOnPauseRequested(Runnable callback) {
        onPauseRequested = callback;
    }

    public void setOnWeaponSwitchRequested(Runnable callback) {
        onWeaponSwitchRequested = callback;
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

    public void updateData(GameWorld world, Player player, LevelManager levelManager,
                           MissionManager missionManager, int enemyCount, int itemCount) {

        if (player != null) {
            int currentHp = player.getHealth();
            int maxHp = player.getMaxHealth();

            if (hpLabel != null) {
                hpLabel.setText(currentHp + "/" + maxHp);
            }

            if (hpBar != null) {
                hpBar.setProgress(maxHp > 0 ? (double) currentHp / maxHp : 0.0);
            }

            updateWeaponUI(player);
            updateBuffUI(player);
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

        if (entitiesLabel != null) {
            entitiesLabel.setText(String.valueOf(enemyCount));
        }

        if (world != null) {
            if (goldLabel != null) {
                goldLabel.setText(String.valueOf(world.getGold()));
            }

            if (gemsLabel != null) {
                gemsLabel.setText(String.valueOf(world.getGems()));
            }

            if (scoreLabel != null) {
                scoreLabel.setText(String.valueOf(world.getScore()));
            }
        }

        updateJoystickUI(world);
        updateMinimapUI(world, player);
    }


    private void updateBuffUI(Player player) {
        updateBuffInventoryUI();
        updateActiveBuffUI(player);
    }

    private void updateBuffInventoryUI() {
        if (topBuffContainer == null) {
            return;
        }

        BuffInventoryManager inventory = BuffInventoryManager.getInstance();

        for (BuffType type : BuffType.values()) {
            int quantity = inventory.getQuantity(type);
            StackPane node = buffNodes.get(type);

            if (quantity <= 0) {
                if (node != null) {
                    topBuffContainer.getChildren().remove(node);
                    buffNodes.remove(type);
                }
                continue;
            }

            if (node == null) {
                node = createBuffInventoryNode(type);
                buffNodes.put(type, node);
                topBuffContainer.getChildren().add(node);
            }

            Label quantityLabel = (Label) node.getProperties().get("quantityLabel");
            if (quantityLabel != null) {
                quantityLabel.setText("x" + quantity);
            }
        }
    }

    private StackPane createBuffInventoryNode(BuffType type) {
        StackPane root = new StackPane();
        root.setPrefSize(50, 50);
        root.setMinSize(50, 50);
        root.setMaxSize(50, 50);
        root.getStyleClass().add("hud-buff-item");
        root.setMouseTransparent(false);
        root.setPickOnBounds(true);
        root.setCursor(javafx.scene.Cursor.HAND);

        root.setOnMouseClicked(event -> {
            if (onBuffUseRequested != null) {
                onBuffUseRequested.accept(type);
            }

            event.consume();
        });

        ImageView icon = new ImageView(loadImage(type.getImagePath()));
        icon.setFitWidth(38);
        icon.setFitHeight(38);
        icon.setPreserveRatio(true);
        icon.setSmooth(false);
        icon.setMouseTransparent(true);

        Label quantityLabel = new Label();
        quantityLabel.getStyleClass().add("hud-buff-quantity");
        quantityLabel.setMouseTransparent(true);
        StackPane.setAlignment(quantityLabel, Pos.BOTTOM_RIGHT);

        root.getChildren().addAll(icon, quantityLabel);
        root.getProperties().put("quantityLabel", quantityLabel);
        return root;
    }

    private void updateActiveBuffUI(Player player) {
        if (player == null) {
            setActiveBuffVisible(false);
            return;
        }

        BuffType shownType = null;
        Buff shownBuff = null;

        for (BuffType type : BuffType.values()) {
            Buff activeBuff = player.getBuffManager().getActiveBuff(type);

            if (activeBuff != null && !activeBuff.isFinished() && !type.isInstant()) {
                shownType = type;
                shownBuff = activeBuff;
                break;
            }
        }

        if (shownType == null || shownBuff == null) {
            setActiveBuffVisible(false);
            return;
        }

        setActiveBuffVisible(true);

        if (activeBuffIcon != null) {
            activeBuffIcon.setImage(loadImage(shownType.getImagePath()));
            activeBuffIcon.setSmooth(false);
        }

        if (activeBuffTimerLabel != null) {
            activeBuffTimerLabel.setText(String.format("%.1fs", shownBuff.getRemainingSeconds()));
        }
    }

    private void setActiveBuffVisible(boolean visible) {
        if (activeBuffTimerBox != null) {
            activeBuffTimerBox.setVisible(visible);
            activeBuffTimerBox.setManaged(visible);
        }
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
        String imagePath = matchedType != null ? matchedType.getImagePath() : null;

        if (imagePath != null) {
            if (!imagePath.equals(currentWeaponImagePath)) {
                currentWeaponImagePath = imagePath;
                weaponIcon.setImage(loadImage(imagePath));
            }
        } else {
            currentWeaponImagePath = "";
            weaponIcon.setImage(null);
        }

        weaponIcon.setSmooth(false);
        weaponIcon.setPreserveRatio(true);
    }

    private WeaponType findWeaponTypeByName(String rawName) {
        if (rawName == null) return null;

        String normalizedInput = rawName.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();

        for (WeaponType type : WeaponType.values()) {
            String normalizedDisplayName = type.getDisplayName()
                    .replaceAll("[^a-zA-Z0-9]", "")
                    .toLowerCase();

            if (normalizedDisplayName.equals(normalizedInput)) {
                return type;
            }

            String normalizedEnumName = type.name()
                    .replaceAll("[^a-zA-Z0-9]", "")
                    .toLowerCase();

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

            if (world.getInputHandler().isDown(javafx.scene.input.KeyCode.W)
                    || world.getInputHandler().isDown(javafx.scene.input.KeyCode.UP)) {
                dy -= 1.0;
            }

            if (world.getInputHandler().isDown(javafx.scene.input.KeyCode.S)
                    || world.getInputHandler().isDown(javafx.scene.input.KeyCode.DOWN)) {
                dy += 1.0;
            }

            if (world.getInputHandler().isDown(javafx.scene.input.KeyCode.A)
                    || world.getInputHandler().isDown(javafx.scene.input.KeyCode.LEFT)) {
                dx -= 1.0;
            }

            if (world.getInputHandler().isDown(javafx.scene.input.KeyCode.D)
                    || world.getInputHandler().isDown(javafx.scene.input.KeyCode.RIGHT)) {
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
    public void setOnBuffUseRequested(Consumer<BuffType> callback) {
        onBuffUseRequested = callback;
    }
}