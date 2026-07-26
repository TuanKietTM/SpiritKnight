package com.soulknight.ui;

import com.soulknight.engine.GameWorld;
import com.soulknight.pet.PetSelectionManager;
import com.soulknight.pet.PetType;
import com.soulknight.utils.SoundManager;
import com.soulknight.weapon.WeaponSelectionManager;
import com.soulknight.weapon.WeaponType;
import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ShopController {

    @FXML private Button closeButton;
    @FXML private Label coinLabel;

    @FXML private Button tabPet;
    @FXML private Button tabWeapon;
    @FXML private Button tabHero;
    @FXML private Button tabUpgrade;

    @FXML private FlowPane itemGrid;
    @FXML private Label selectedItemName;
    @FXML private Label selectedItemDesc;
    @FXML private Button actionButton;

    private Runnable onCloseCallback;
    private GameWorld gameWorld;
    private PetType selectedPet = null;
    private WeaponType selectedWeapon = null;
    private AnimationTimer shopAnimTimer;
    private double elapsedTime = 0;
    private long lastTime = 0;
    private static final double SHOP_CANVAS_SIZE = 80.0;
    private static final double SHOP_PET_SIZE = 64.0;

    private final List<PetCanvasRenderer> activeRenderers = new ArrayList<>();

    private record PetCanvasRenderer(PetType pet, Canvas canvas, Image idleSheet) {}

    @FXML
    public void initialize() {
        tabPet.setOnAction(e -> switchTab(tabPet, this::loadPetShop));
        tabWeapon.setOnAction(e -> switchTab(tabWeapon, this::loadWeaponShop));
        tabHero.setOnAction(e -> switchTab(tabHero, () -> loadPlaceholderCategory("CHARACTER", "Unlocked Knight, Paladin...")));
        tabUpgrade.setOnAction(e -> switchTab(tabUpgrade, () -> loadPlaceholderCategory("UPGRADE", "UP")));

        closeButton.setOnAction(e -> {
            SoundManager.getInstance().playSFX("button");
            stopAnimation();
            if (onCloseCallback != null) onCloseCallback.run();
        });

        setupAnimationLoop();
    }

    public void setup(GameWorld world, Runnable onClose) {
        this.gameWorld = world;
        this.onCloseCallback = onClose;

        switchTab(tabPet, this::loadPetShop);
        startAnimation();
    }

    private void switchTab(Button selectedTab, Runnable loadContent) {
        SoundManager.getInstance().playSFX("button");

        tabPet.getStyleClass().remove("tab-active");
        tabWeapon.getStyleClass().remove("tab-active");
        tabHero.getStyleClass().remove("tab-active");
        tabUpgrade.getStyleClass().remove("tab-active");

        selectedTab.getStyleClass().add("tab-active");
        loadContent.run();
    }

    private void loadPetShop() {
        activeRenderers.clear();
        itemGrid.getChildren().clear();

        PetType equippedPet = PetSelectionManager.getInstance().getSelectedPet();

        for (PetType pet : PetType.values()) {
            if (!pet.hasPet()) continue;

            VBox card = createPetCard(pet, equippedPet);
            itemGrid.getChildren().add(card);
        }
    }

    private VBox createPetCard(PetType pet, PetType equippedPet) {
        VBox card = new VBox(6);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("item-card");

        boolean isEquipped = (pet == equippedPet);
        if (isEquipped) {
            card.getStyleClass().add("item-card-equipped");
        }
        Canvas canvas = new Canvas(SHOP_CANVAS_SIZE, SHOP_CANVAS_SIZE);
        Image idleSheet = loadImage(pet.getIdleImagePath());

        if (idleSheet != null) {
            activeRenderers.add(new PetCanvasRenderer(pet, canvas, idleSheet));
        }

        Label nameLabel = new Label(pet.getDisplayName());
        nameLabel.getStyleClass().add("item-card-title");

        card.getChildren().addAll(canvas, nameLabel);

        card.setOnMouseClicked(e -> {
            if (pet.getSoundPath() != null && !pet.getSoundPath().isBlank()) {
                SoundManager.getInstance().playSFX(pet.getSoundPath());
            } else {
                SoundManager.getInstance().playSFX("button");
            }
            itemGrid.getChildren().forEach(n -> n.getStyleClass().remove("item-card-selected"));
            card.getStyleClass().add("item-card-selected");

            this.selectedPet = pet;
            selectedItemName.setText(pet.getDisplayName());
            selectedItemDesc.setText("PACE: " + pet.getMoveSpeed() + " | Supporter");

            actionButton.setVisible(true);

            boolean isCurrentEquipped = PetSelectionManager.getInstance().isSelected(pet);
            if (isCurrentEquipped) {
                actionButton.setText("Equipment");
                actionButton.setDisable(true);
            } else {
                actionButton.setText("CHOSE PET");
                actionButton.setDisable(false);
                actionButton.setOnAction(evt -> {
                    if (pet.getSoundPath() != null && !pet.getSoundPath().isBlank()) {
                        SoundManager.getInstance().playSFX(pet.getSoundPath());
                    } else {
                        SoundManager.getInstance().playSFX("button");
                    }
                    PetSelectionManager.getInstance().selectPet(pet);
                    if (gameWorld != null) {
                        gameWorld.equipPet(pet);
                    }
                    loadPetShop();
                });
            }
        });

        return card;
    }

    private void loadWeaponShop() {
        activeRenderers.clear();
        itemGrid.getChildren().clear();

        WeaponType equippedWeapon = WeaponSelectionManager.getInstance().getSelectedWeapon();

        for (WeaponType weapon : WeaponType.values()) {
            VBox card = createWeaponCard(weapon, equippedWeapon);
            itemGrid.getChildren().add(card);
        }
    }

    private VBox createWeaponCard(WeaponType weapon, WeaponType equippedWeapon) {
        VBox card = new VBox(6);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("item-card");

        boolean isEquipped = (weapon == equippedWeapon);
        if (isEquipped) {
            card.getStyleClass().add("item-card-equipped");
        }

        Canvas canvas = new Canvas(SHOP_CANVAS_SIZE, SHOP_CANVAS_SIZE);
        Image weaponImage = loadImage(weapon.getImagePath());
        drawWeaponIcon(canvas, weaponImage);

        Label nameLabel = new Label(weapon.getDisplayName());
        nameLabel.getStyleClass().add("item-card-title");

        card.getChildren().addAll(canvas, nameLabel);

        card.setOnMouseClicked(e -> {
            SoundManager.getInstance().playSFX("button");
            itemGrid.getChildren().forEach(n -> n.getStyleClass().remove("item-card-selected"));
            card.getStyleClass().add("item-card-selected");

            this.selectedWeapon = weapon;
            selectedItemName.setText(weapon.getDisplayName());
            selectedItemDesc.setText(buildWeaponDesc(weapon));

            actionButton.setVisible(true);

            boolean isCurrentEquipped = WeaponSelectionManager.getInstance().isSelected(weapon);
            if (isCurrentEquipped) {
                actionButton.setText("Equipment");
                actionButton.setDisable(true);
            } else {
                actionButton.setText("CHOSE WEAPON");
                actionButton.setDisable(false);
                actionButton.setOnAction(evt -> {
                    SoundManager.getInstance().playSFX("button");
                    WeaponSelectionManager.getInstance().selectWeapon(weapon);
                    if (gameWorld != null) {
                        gameWorld.equipWeapon(weapon);
                    }
                    loadWeaponShop();
                });
            }
        });

        return card;
    }

    private String buildWeaponDesc(WeaponType weapon) {
        String type = weapon.isRanged() ? "Gun" : "Melee";
        return "DMG: " + weapon.getDamage()
                + " | CD: " + weapon.getCooldownSeconds() + "s"
                + " | " + type;
    }

    // Ve anh vu khi (tinh) len canvas, giu ti le va khong lam mo pixel
    private void drawWeaponIcon(Canvas canvas, Image image) {
        if (image == null || image.getWidth() <= 0) return;

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        gc.setImageSmoothing(false);

        double scale = Math.min(
                SHOP_PET_SIZE / image.getWidth(),
                SHOP_PET_SIZE / image.getHeight()
        );
        double drawW = image.getWidth() * scale;
        double drawH = image.getHeight() * scale;
        double drawX = (canvas.getWidth() - drawW) / 2.0;
        double drawY = (canvas.getHeight() - drawH) / 2.0;

        gc.drawImage(image, drawX, drawY, drawW, drawH);
    }

    private void setupAnimationLoop() {
        shopAnimTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (lastTime == 0) {
                    lastTime = now;
                    return;
                }

                double deltaSeconds = (now - lastTime) / 1_000_000_000.0;
                lastTime = now;
                elapsedTime += deltaSeconds;

                renderPetAnimations();
            }
        };
    }

    private void renderPetAnimations() {
        for (PetCanvasRenderer renderer : activeRenderers) {
            PetType pet = renderer.pet();
            Canvas canvas = renderer.canvas();
            Image sheet = renderer.idleSheet();

            GraphicsContext gc = canvas.getGraphicsContext2D();
            gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

            int totalFrames = pet.getIdleFrameCount();
            if (totalFrames <= 0) totalFrames = 1;

            int currentFrame = (int) (elapsedTime / pet.getFrameDuration()) % totalFrames;
            double sx = currentFrame * pet.getFrameWidth();
            double sy = 0;
            double sw = pet.getFrameWidth();
            double sh = pet.getFrameHeight();
            double drawX = (canvas.getWidth() - SHOP_PET_SIZE) / 2.0;
            double drawY = (canvas.getHeight() - SHOP_PET_SIZE) / 2.0;

            gc.setImageSmoothing(false);
            gc.drawImage(sheet, sx, sy, sw, sh,
                    drawX, drawY, SHOP_PET_SIZE, SHOP_PET_SIZE
            );
        }
    }

    private void startAnimation() {
        lastTime = 0;
        shopAnimTimer.start();
    }

    private void stopAnimation() {
        if (shopAnimTimer != null) {
            shopAnimTimer.stop();
        }
    }

    private void loadPlaceholderCategory(String title, String desc) {
        activeRenderers.clear();
        itemGrid.getChildren().clear();
        selectedItemName.setText(title);
        selectedItemDesc.setText(desc);
        actionButton.setVisible(false);
    }

    private Image loadImage(String path) {
        if (path == null || path.isBlank()) return null;
        URL resource = getClass().getResource(path);
        if (resource == null) return null;
        return new Image(resource.toExternalForm(), false);
    }
}