package com.soulknight.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;

public final class SettingScreen {

    @FXML
    private Slider bgmSlider;
    @FXML
    private Slider sfxSlider;
    @FXML
    private Label lblBgmValue;
    @FXML
    private Label lblSfxValue;

    private Runnable onCloseCallback;

    private java.util.function.Consumer<Double> onBgmChanged;
    private java.util.function.Consumer<Double> onSfxChanged;

    @FXML
    public void initialize() {
//         thay doi am thanh
        bgmSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            int percentage = newValue.intValue();
            lblBgmValue.setText(percentage + "%");
            if (onBgmChanged != null) {
                onBgmChanged.accept(newValue.doubleValue() / 100.0); //  0.0 -> 1.0
            }
        });

        //  SFX
        sfxSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            int percentage = newValue.intValue();
            lblSfxValue.setText(percentage + "%");
            if (onSfxChanged != null) {
                onSfxChanged.accept(newValue.doubleValue() / 100.0);
            }
        });
    }

    public void setup(Runnable closeCallback,
                      java.util.function.Consumer<Double> bgmListener,
                      java.util.function.Consumer<Double> sfxListener,
                      double currentBgm, double currentSfx) {
        this.onCloseCallback = closeCallback;
        this.onBgmChanged = bgmListener;
        this.onSfxChanged = sfxListener;

        bgmSlider.setValue(currentBgm * 100);
        sfxSlider.setValue(currentSfx * 100);
    }

    @FXML
    private void onCloseClicked(ActionEvent event) {
        if (onCloseCallback != null) {
            onCloseCallback.run();
        }
    }
}