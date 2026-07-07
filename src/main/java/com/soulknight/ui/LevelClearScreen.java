package com.soulknight.ui;

import com.soulknight.level.LevelManager;
import com.soulknight.mission.MissionManager;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public final class LevelClearScreen {

    public void render(GraphicsContext graphicsContext, double width, double height, LevelManager levelManager, MissionManager missionManager) {
        graphicsContext.setFill(Color.rgb(0, 0, 0, 0.55));
        graphicsContext.fillRect(0.0, 0.0, width, height);

        graphicsContext.setFill(Color.GOLD);
        graphicsContext.setFont(Font.font("Arial", 42.0));
        graphicsContext.fillText("Portal Unlocked", width * 0.36, height * 0.40);

        graphicsContext.setFill(Color.WHITE);
        graphicsContext.setFont(Font.font("Arial", 22.0));
        graphicsContext.fillText(levelManager.getLevelBanner(), width * 0.34, height * 0.48);
        graphicsContext.fillText(missionManager.getMissionTitle(), width * 0.35, height * 0.53);
        graphicsContext.fillText("Progress: " + missionManager.getMissionProgress(), width * 0.35, height * 0.58);
        graphicsContext.fillText("Walk into the portal to continue", width * 0.32, height * 0.66);
    }
}
