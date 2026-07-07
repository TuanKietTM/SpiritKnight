package com.soulknight.ui;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public final class GameOverScreen {

    public void render(GraphicsContext graphicsContext, double width, double height) {
        graphicsContext.setFill(Color.rgb(0, 0, 0, 0.5));
        graphicsContext.fillRect(0.0, 0.0, width, height);

        graphicsContext.setFill(Color.CRIMSON);
        graphicsContext.setFont(Font.font("Arial", 54.0));
        graphicsContext.fillText("Game Over", width * 0.38, height * 0.42);

        graphicsContext.setFill(Color.WHITE);
        graphicsContext.setFont(Font.font("Arial", 24.0));
        graphicsContext.fillText("Press ENTER or click to restart", width * 0.31, height * 0.5);
    }
}
