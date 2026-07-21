package com.soulknight.ui;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import java.util.function.Consumer;

public final class SettingScreen {

    @FXML
    private Slider bgmSlider;
    @FXML
    private Slider sfxSlider;
    @FXML
    private Label lblBgmValue;
    @FXML
    private Label lblSfxValue;

    // ChoiceBox chọn chế độ điều khiển
    @FXML
    private ChoiceBox<String> controlModeChoice;

    private Runnable onCloseCallback;
    private Consumer<Double> onBgmChanged;
    private Consumer<Double> onSfxChanged;
    private Consumer<Boolean> onTouchpadModeChanged; // true = Touchpad / Auto-Aim, false = Keyboard & Mouse

    @FXML
    public void initialize() {
        // che do dieu khien
        if (controlModeChoice != null) {
            controlModeChoice.setItems(FXCollections.observableArrayList(
                    "KEYBOARD & MOUSE",
                    "TOUCHPAD / JOYSTICK"
            ));

            controlModeChoice.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
                if (onTouchpadModeChanged != null && newValue != null) {
                    // Index 1 là Touchpad, Index 0 là Keyboard & Mouse
                    boolean isTouchpad = (newValue.intValue() == 1);
                    onTouchpadModeChanged.accept(isTouchpad);
                }
            });
        }

        // thay doi BGM
        bgmSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            int percentage = newValue.intValue();
            lblBgmValue.setText(percentage + "%");
            if (onBgmChanged != null) {
                onBgmChanged.accept(newValue.doubleValue() / 100.0); // Chuyển đổi từ 0 -> 100 thành 0.0 -> 1.0
            }
        });

        // change SFX
        sfxSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            int percentage = newValue.intValue();
            lblSfxValue.setText(percentage + "%");
            if (onSfxChanged != null) {
                onSfxChanged.accept(newValue.doubleValue() / 100.0);
            }
        });
    }


    public void setup(Runnable closeCallback,
                      Consumer<Double> bgmListener,
                      Consumer<Double> sfxListener,
                      Consumer<Boolean> touchpadListener,
                      double currentBgm,
                      double currentSfx,
                      boolean isTouchpadMode) {
        this.onCloseCallback = closeCallback;
        this.onBgmChanged = bgmListener;
        this.onSfxChanged = sfxListener;
        this.onTouchpadModeChanged = touchpadListener;

        bgmSlider.setValue(currentBgm * 100);
        sfxSlider.setValue(currentSfx * 100);
        lblBgmValue.setText((int)(currentBgm * 100) + "%");
        lblSfxValue.setText((int)(currentSfx * 100) + "%");

        if (controlModeChoice != null) {
            controlModeChoice.getSelectionModel().select(isTouchpadMode ? 1 : 0);
        }
    }


    public void setup(Runnable closeCallback,
                      Consumer<Double> bgmListener,
                      Consumer<Double> sfxListener,
                      double currentBgm,
                      double currentSfx) {
        setup(closeCallback, bgmListener, sfxListener, null, currentBgm, currentSfx, false);
    }

    @FXML
    private void onCloseClicked(ActionEvent event) {
        if (onCloseCallback != null) {
            onCloseCallback.run();
        }
    }
}