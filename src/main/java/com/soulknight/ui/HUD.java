package com.soulknight.ui;

import com.soulknight.entity.Player;
import com.soulknight.level.LevelManager;
import com.soulknight.mission.MissionManager;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public final class HUD {

    public void render(GraphicsContext graphicsContext, Player player, LevelManager levelManager, MissionManager missionManager,
                       int enemyCount, int itemCount, double width, double height) {
        graphicsContext.setFill(Color.rgb(0, 0, 0, 0.55));
        graphicsContext.fillRect(0.0, height - 86.0, width, 86.0);

        graphicsContext.setFill(Color.WHITE);
        graphicsContext.setFont(Font.font("Consolas", 20.0));
        graphicsContext.fillText("HP: " + player.getHealth() + "/" + player.getMaxHealth(), 24.0, height - 52.0);
        graphicsContext.fillText(levelManager.getLevelBanner(), 220.0, height - 52.0);
        graphicsContext.fillText("Mission: " + missionManager.getMissionTitle(), 24.0, height - 26.0);
        graphicsContext.fillText("Progress: " + missionManager.getMissionProgress(), 420.0, height - 26.0);
        graphicsContext.fillText("Enemies: " + enemyCount + "  Items: " + itemCount, 760.0, height - 26.0);
        graphicsContext.fillText("Weapon: " + player.getWeaponName(), 980.0, height - 26.0);
    }
}
