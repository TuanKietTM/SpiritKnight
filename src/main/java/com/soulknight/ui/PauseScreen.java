package com.soulknight.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public final class PauseScreen {

    @FXML
    private ImageView imgCharacterAvatar; // Liên kết tới khung Avatar trong FXML

    private Runnable onResumeCallback;
    private Runnable onSettingCallback;
    private Runnable onMainMenuCallback;

    @FXML
    public void initialize() {
        // Có thể nạp ảnh nhân vật mặc định ở đây nếu muốn
        try {
            imgCharacterAvatar.setImage(new Image(getClass().getResourceAsStream("/assets/sprites/Character.png")));
        } catch (Exception e) {
            // Fallback nếu chưa có file ảnh
        }
    }

    public void setCallbacks(Runnable resume, Runnable setting, Runnable mainMenu) {
        this.onResumeCallback = resume;
        this.onSettingCallback = setting;
        this.onMainMenuCallback = mainMenu;
    }

    @FXML
    private void onResumeClicked(ActionEvent event) {
        if (onResumeCallback != null) onResumeCallback.run();
    }

    @FXML
    private void onSettingClicked(ActionEvent event) {
        if (onSettingCallback != null) onSettingCallback.run();
    }

    @FXML
    private void onMainMenuClicked(ActionEvent event) {
        if (onMainMenuCallback != null) onMainMenuCallback.run();
    }
}