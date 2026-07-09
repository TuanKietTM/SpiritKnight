package com.soulknight.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;

public final class PauseScreen {

    private Runnable onResumeCallback;
    private Runnable onRestartCallback;
    private Runnable onMainMenuCallback;

    public void setCallbacks(Runnable resume, Runnable restart, Runnable mainMenu) {
        this.onResumeCallback = resume;
        this.onRestartCallback = restart;
        this.onMainMenuCallback = mainMenu;
    }

    @FXML
    private void onResumeClicked(ActionEvent event) {
        if (onResumeCallback != null) onResumeCallback.run();
    }

    @FXML
    private void onRestartClicked(ActionEvent event) {
        if (onRestartCallback != null) onRestartCallback.run();
    }

    @FXML
    private void onMainMenuClicked(ActionEvent event) {
        if (onMainMenuCallback != null) onMainMenuCallback.run();
    }
}