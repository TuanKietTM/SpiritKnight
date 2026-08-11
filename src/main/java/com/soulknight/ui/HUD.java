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
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

import java.net.URL;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

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
    private HBox activeBuffContainer;

    private MinimapRenderer minimapRenderer;

    private Runnable onPauseRequested;
    private Runnable onWeaponSwitchRequested;
    private Consumer<BuffType> onBuffUseRequested;

    // luu path anh vu khi hien tai de khong load lai image moi frame
    private String currentWeaponImagePath = "";

    private final Map<BuffType, StackPane> buffNodes =
            new EnumMap<>(BuffType.class);

    @FXML
    private void initialize() {

        if (minimapCanvas != null) {
            minimapRenderer = new MinimapRenderer(minimapCanvas);
        }

        if (hpIcon != null) {
            hpIcon.setImage(loadHUDIcon("/assets/icon/hp.png"));
        }

        if (shieldIcon != null) {
            shieldIcon.setImage(loadHUDIcon("/assets/icon/shield.png"));
        }

        if (manaIcon != null) {
            manaIcon.setImage(loadHUDIcon("/assets/icon/mana.png"));
        }

        if (goldIcon != null) {
            goldIcon.setImage(loadHUDIcon("/assets/icon/gold.png"));
        }

        if (gemIcon != null) {
            gemIcon.setImage(loadHUDIcon("/assets/icon/gem.png"));
        }

        if (scoreIcon != null) {
            scoreIcon.setImage(loadHUDIcon("/assets/icon/score.png"));
        }

        if (weaponIcon != null) {
            weaponIcon.setSmooth(false);
            weaponIcon.setPreserveRatio(true);
        }
    }

    private Image loadHUDIcon(String path) {

        if (path == null || path.isBlank()) {
            return null;
        }

        URL resource = getClass().getResource(path);

        if (resource == null) {
            System.err.println("Khong tim thay HUD icon: " + path);
            return null;
        }

        return new Image(resource.toExternalForm(), false);
    }

    public void setOnPauseRequested(Runnable callback) {
        onPauseRequested = callback;
    }

    public void setOnWeaponSwitchRequested(Runnable callback) {
        onWeaponSwitchRequested = callback;
    }

    public void setOnBuffUseRequested(Consumer<BuffType> callback) {
        onBuffUseRequested = callback;
    }

    @FXML
    private void onPauseButtonClicked(ActionEvent event) {

        if (onPauseRequested != null) {
            onPauseRequested.run();
        }
    }

    @FXML
    private void onWeaponButtonClicked(MouseEvent event) {

        if (event == null) {
            return;
        }

        // chi doi vu khi bang chuot trai
        if (event.getButton() != MouseButton.PRIMARY) {
            return;
        }

        // tranh click tren HUD bi truyen xuong Canvas gameplay
        event.consume();

        if (onWeaponSwitchRequested != null) {
            onWeaponSwitchRequested.run();
        }
    }

    public void updateData(GameWorld world,
                           Player player,
                           LevelManager levelManager,
                           MissionManager missionManager,
                           int enemyCount,
                           int itemCount) {

        updatePlayerUI(player);
        updateWorldUI(world, enemyCount);

        updateJoystickUI(world);
        updateMinimapUI(world, player);
    }

    private void updatePlayerUI(Player player) {

        if (player == null) {
            clearPlayerUI();
            return;
        }

        int currentHp = player.getHealth();
        int maxHp = player.getMaxHealth();

        if (hpLabel != null) {
            hpLabel.setText(currentHp + "/" + maxHp);
        }

        if (hpBar != null) {
            hpBar.setProgress(maxHp > 0 ? (double) currentHp / maxHp : 0.0);
        }

        /*
         * Shield va mana hien tai van dang dung gia tri HUD co dinh
         * de giu nguyen logic cu cua project.
         */
        updateShieldUI(player);
        updateManaUI(player);
        updateWeaponUI(player);
        updateBuffUI(player);
    }

    private void clearPlayerUI() {

        if (hpLabel != null) {
            hpLabel.setText("0/0");
        }

        if (hpBar != null) {
            hpBar.setProgress(0.0);
        }

        clearWeaponUI();

        if (activeBuffContainer != null) {
            activeBuffContainer.getChildren().clear();
        }
    }

    private void updateShieldUI(Player player) {
        if (shieldLabel == null || shieldBar == null) return;

        if (player == null) {
            shieldLabel.setText("0/0");
            shieldBar.setProgress(0.0);
            return;
        }

        int current = player.getShield();
        int max = player.getMaxShield();

        shieldLabel.setText(current + "/" + max);
        shieldBar.setProgress(max > 0 ? (double) current / max : 0.0);
    }

    private void updateManaUI(Player player) {
        if (manaLabel == null || manaBar == null) return;

        if (player == null) {
            manaLabel.setText("0/0");
            manaBar.setProgress(0.0);
            return;
        }

        int current = (int) Math.round(player.getMana());
        int max = (int) Math.round(player.getMaxMana());

        manaLabel.setText(current + "/" + max);
        manaBar.setProgress(max > 0 ? (double) current / max : 0.0);
    }

    private void updateWorldUI(GameWorld world, int enemyCount) {

        if (entitiesLabel != null) {
            entitiesLabel.setText(String.valueOf(Math.max(0, enemyCount)));
        }

        if (world == null) {
            return;
        }

        if (goldLabel != null) {
            goldLabel.setText(String.valueOf(world.getGold()));
        }

        if (gemsLabel != null) {
            gemsLabel.setText(String.valueOf(world.getGems()));
        }

        if (scoreLabel != null) {
            scoreLabel.setText(
                    String.valueOf(world.getScore())
            );
        }
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
                if (node != null) {topBuffContainer.getChildren().remove(node);
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
        /*
         * Xu ly ngay khi nhan chuot de tranh click bi
         * truyen xuong gameplay.
         */
        root.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
                    if (event.getButton() != MouseButton.PRIMARY) {
                        return;
                    }
                    event.consume();
                    if (onBuffUseRequested != null) {
                        onBuffUseRequested.accept(type);
                    }
                    playBuffClickAnimation(root);
                }
        );

        root.addEventFilter(
                MouseEvent.MOUSE_RELEASED,
                event -> {
                    if (event.getButton() == MouseButton.PRIMARY) {
                        event.consume();
                    }
                }
        );

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

    private void playBuffClickAnimation(StackPane node) {
        if (node == null) {
            return;
        }

        javafx.animation.ScaleTransition transition = new javafx.animation.ScaleTransition(
                        javafx.util.Duration.millis(90), node);
        transition.setFromX(1.0);
        transition.setFromY(1.0);
        transition.setToX(0.82);
        transition.setToY(0.82);
        transition.setCycleCount(2);
        transition.setAutoReverse(true);
        transition.play();
    }
    private void updateActiveBuffUI(Player player) {
        if (activeBuffContainer == null) {
            return;
        }
        activeBuffContainer.getChildren().clear();
        if (player == null) {
            return;
        }
        for (BuffType type : BuffType.values()) {
            Buff activeBuff = player.getBuffManager().getActiveBuff(type);

            if (activeBuff == null || activeBuff.isFinished() || type.isInstant()) {
                continue;
            }
            activeBuffContainer.getChildren().add(createActiveBuffNode(type, activeBuff));
        }
    }

    private VBox createActiveBuffNode(BuffType type, Buff buff) {
        ImageView icon = new ImageView(loadImage(type.getImagePath()));
        icon.setFitWidth(40);
        icon.setFitHeight(40);
        icon.setPreserveRatio(true);
        icon.setSmooth(false);
        icon.setMouseTransparent(true);
        Label timerLabel = new Label(String.format("%.1fs", buff.getRemainingSeconds()));
        timerLabel.getStyleClass().add("active-buff-time");
        timerLabel.setMouseTransparent(true);
        VBox box = new VBox(3, icon, timerLabel);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("active-buff-box");
        box.setMouseTransparent(true);
        return box;
    }
    public void useBuffByIndex(int index) {
        if (index < 0 || onBuffUseRequested == null) {
            return;
        }

        int currentIndex = 0;
        BuffInventoryManager inventory = BuffInventoryManager.getInstance();
        for (BuffType type : BuffType.values()) {
            if (inventory.getQuantity(type) <= 0) {
                continue;
            }

            if (currentIndex == index) {
                onBuffUseRequested.accept(type);
                StackPane node = buffNodes.get(type);
                if (node != null) {
                    playBuffClickAnimation(node);
                }

                return;
            }

            currentIndex++;
        }
    }

    private void updateMinimapUI(GameWorld world, Player player) {
        if (minimapCanvas == null) {
            return;
        }

        if (minimapRenderer == null) {
            minimapRenderer = new MinimapRenderer(minimapCanvas);
        }
        minimapRenderer.render(world, player);
    }

    /*
     * HUD chi hien thi weapon hien tai cua Player.
     *
     * Viec quan ly Slot 1 / Slot 2 nam o GameWorld va
     * WeaponSelectionManager, HUD khong tu doi loadout.
     */
    private void updateWeaponUI(Player player) {

        if (player == null) {
            clearWeaponUI();
            return;
        }

        String currentName = player.getWeaponName();
        if (currentName == null || currentName.isBlank()) {
            clearWeaponUI();
            return;
        }

        if (weaponLabel != null) {
            weaponLabel.setText("");
            weaponLabel.setVisible(false);
            weaponLabel.setManaged(false);
        }

        if (weaponIcon == null) {
            return;
        }

        WeaponType matchedType = findWeaponTypeByName(currentName);
        if (matchedType == null) {

            /*
             * Van giu ten weapon tren HUD neu khong map duoc WeaponType,
             * chi xoa icon de tranh hien anh cua weapon cu.
             */
            currentWeaponImagePath = "";
            weaponIcon.setImage(null);
            return;
        }
        String imagePath = matchedType.getImagePath();
        if (imagePath == null || imagePath.isBlank()) {
            currentWeaponImagePath = "";
            weaponIcon.setImage(null);
            return;
        }

        /*
         * Chi load image khi weapon thuc su thay doi.
         * updateData duoc goi lien tuc nen khong duoc new Image moi frame.
         */
        if (!imagePath.equals(currentWeaponImagePath)) {
            Image image = loadImage(imagePath);
            if (image == null) {
                currentWeaponImagePath = "";
                weaponIcon.setImage(null);
                return;
            }
            currentWeaponImagePath = imagePath;
            weaponIcon.setImage(image);
        }
        weaponIcon.setSmooth(false);
        weaponIcon.setPreserveRatio(true);
    }

    private void clearWeaponUI() {
        currentWeaponImagePath = "";
        if (weaponLabel != null) {
            weaponLabel.setText("");
        }
        if (weaponIcon != null) {
            weaponIcon.setImage(null);
        }
    }

    private WeaponType findWeaponTypeByName(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            return null;
        }
        String normalizedInput = normalizeWeaponName(rawName);

        for (WeaponType type : WeaponType.values()) {
            String normalizedDisplayName = normalizeWeaponName(type.getDisplayName());
            if (normalizedDisplayName.equals(normalizedInput)) {
                return type;
            }

            String normalizedEnumName = normalizeWeaponName(type.name());
            if (normalizedEnumName.equals(normalizedInput)) {
                return type;
            }
        }

        return null;
    }
    private String normalizeWeaponName(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
    }
    private void updateJoystickUI(GameWorld world) {
        if (joystickContainer == null || joystickThumb == null || world == null) {
            return;
        }
        if (world.getInputHandler() == null) {
            return;
        }
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
        if (path == null || path.isBlank()) {
            return null;
        }
        URL resource = getClass().getResource(path);
        if (resource == null) {
            System.err.println("Khong tim thay image: " + path);
            return null;
        }
        return new Image(resource.toExternalForm(), false);
    }
}