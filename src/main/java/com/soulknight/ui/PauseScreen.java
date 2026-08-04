package com.soulknight.ui;

import com.soulknight.buff.BuffInventoryManager;
import com.soulknight.buff.BuffType;
import com.soulknight.entity.Player;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

import java.net.URL;
import java.util.List;

public final class PauseScreen {

    @FXML private ImageView imgCharacterAvatar;

    @FXML private StackPane buffSlot1;
    @FXML private StackPane buffSlot2;
    @FXML private StackPane buffSlot3;
    @FXML private StackPane buffSlot4;
    @FXML private StackPane buffSlot5;
    @FXML private StackPane buffSlot6;
    @FXML private StackPane buffSlot7;
    @FXML private StackPane buffSlot8;
    @FXML private StackPane buffSlot9;
    @FXML private StackPane buffSlot10;
    @FXML private StackPane buffSlot11;
    @FXML private StackPane buffSlot12;

    private Runnable onResumeCallback;
    private Runnable onSettingCallback;
    private Runnable onMainMenuCallback;

    @FXML
    public void initialize() {
        try {
            imgCharacterAvatar.setImage(new Image(
                    getClass().getResourceAsStream("/assets/sprites/Character.png")
            ));
        } catch (Exception ignored) {
        }

        hideAllBuffSlots();
    }

    public void setPlayer(Player player) {
        refreshBuffSlots();
    }

    public void refreshBuffSlots() {
        hideAllBuffSlots();

        List<StackPane> slots = getBuffSlots();
        BuffInventoryManager inventory = BuffInventoryManager.getInstance();
        int slotIndex = 0;

        for (BuffType type : BuffType.values()) {
            int quantity = inventory.getQuantity(type);

            if (quantity <= 0 || slotIndex >= slots.size()) {
                continue;
            }

            fillBuffSlot(slots.get(slotIndex++), type, quantity);
        }
    }

    private void fillBuffSlot(StackPane slot, BuffType type, int quantity) {
        if (slot == null) {
            return;
        }

        slot.getChildren().clear();
        slot.setVisible(true);
        slot.setManaged(true);

        if (!slot.getStyleClass().contains("buff-slot")) {
            slot.getStyleClass().add("buff-slot");
        }

        if (!slot.getStyleClass().contains("slot-filled")) {
            slot.getStyleClass().add("slot-filled");
        }

        ImageView icon = new ImageView(loadImage(type.getImagePath()));
        icon.setFitWidth(40);
        icon.setFitHeight(40);
        icon.setPreserveRatio(true);
        icon.setSmooth(false);
        icon.setMouseTransparent(true);

        Label quantityLabel = new Label("x" + quantity);
        quantityLabel.getStyleClass().add("buff-slot-quantity");
        quantityLabel.setMouseTransparent(true);

        StackPane.setAlignment(quantityLabel, Pos.BOTTOM_RIGHT);

        slot.getChildren().addAll(icon, quantityLabel);
    }

    private void hideAllBuffSlots() {
        for (StackPane slot : getBuffSlots()) {
            if (slot == null) {
                continue;
            }

            slot.getChildren().clear();
            slot.getStyleClass().remove("slot-filled");
            slot.setVisible(false);
            slot.setManaged(false);
        }
    }

    private List<StackPane> getBuffSlots() {
        return List.of(
                buffSlot1, buffSlot2, buffSlot3, buffSlot4, buffSlot5, buffSlot6,
                buffSlot7, buffSlot8, buffSlot9, buffSlot10, buffSlot11, buffSlot12
        );
    }

    private Image loadImage(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }

        URL resource = getClass().getResource(path);
        return resource == null ? null : new Image(resource.toExternalForm(), false);
    }

    public void setCallbacks(Runnable resume, Runnable setting, Runnable mainMenu) {
        this.onResumeCallback = resume;
        this.onSettingCallback = setting;
        this.onMainMenuCallback = mainMenu;
    }

    @FXML
    private void onResumeClicked(javafx.event.ActionEvent event) {
        if (onResumeCallback != null) onResumeCallback.run();
    }

    @FXML
    private void onSettingClicked(javafx.event.ActionEvent event) {
        if (onSettingCallback != null) onSettingCallback.run();
    }

    @FXML
    private void onMainMenuClicked(javafx.event.ActionEvent event) {
        if (onMainMenuCallback != null) onMainMenuCallback.run();
    }
}