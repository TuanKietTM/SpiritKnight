package com.soulknight.ui;

import com.soulknight.engine.GameWorld;
import com.soulknight.pet.PetSelectionManager;
import com.soulknight.pet.PetType;
import com.soulknight.utils.SoundManager;
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
    private AnimationTimer shopAnimTimer;
    private double elapsedTime = 0;
    private long lastTime = 0;

//  render cho tung pet
    private final List<PetCanvasRenderer> activeRenderers = new ArrayList<>();

    private record PetCanvasRenderer(PetType pet, Canvas canvas, Image idleSheet) {}

    @FXML
    public void initialize() {
        tabPet.setOnAction(e -> switchTab(tabPet, this::loadPetShop));
        tabWeapon.setOnAction(e -> switchTab(tabWeapon, () -> loadPlaceholderCategory("WEAPON", "Coming soon")));
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

    /**
     * Cho pet vao cac grid
     */
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

        // Tạo Canvas hiển thị Animation Sprite Sheet
        Canvas canvas = new Canvas(48, 48);
        Image idleSheet = loadImage(pet.getIdleImagePath());

        if (idleSheet != null) {
            activeRenderers.add(new PetCanvasRenderer(pet, canvas, idleSheet));
        }

        Label nameLabel = new Label(pet.getDisplayName());
        nameLabel.getStyleClass().add("item-card-title");

        card.getChildren().addAll(canvas, nameLabel);

        // Click chọn thẻ Pet
        // 🔥 CLICK VÀO THẺ PET: Phát tiếng kêu của Pet đó
        card.setOnMouseClicked(e -> {
            // 1. Phát tiếng kêu đặc trưng của Pet nếu có
            if (pet.getSoundPath() != null && !pet.getSoundPath().isBlank()) {
                SoundManager.getInstance().playSFX(pet.getSoundPath());
            } else {
                SoundManager.getInstance().playSFX("button");
            }
            itemGrid.getChildren().forEach(n -> n.getStyleClass().remove("item-card-selected"));
            card.getStyleClass().add("item-card-selected");

            this.selectedPet = pet;
            selectedItemName.setText(pet.getDisplayName());
            selectedItemDesc.setText("PACE" + pet.getMoveSpeed() + " | Supporter");

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

    /**
     * Vong lao animation
     */
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
            double drawX = (canvas.getWidth() - pet.getRenderWidth()) / 2.0;
            double drawY = (canvas.getHeight() - pet.getRenderHeight()) / 2.0;

            gc.drawImage(
                    sheet,
                    sx, sy, sw, sh,
                    drawX, drawY, pet.getRenderWidth(), pet.getRenderHeight()
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