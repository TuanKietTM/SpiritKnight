package com.soulknight.ui;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public final class VictoryScreen {

    public void render(GraphicsContext graphicsContext, double width, double height) {
        graphicsContext.setFill(Color.rgb(0, 0, 0, 0.6));
        graphicsContext.fillRect(0.0, 0.0, width, height);

        graphicsContext.setFill(Color.LIGHTGREEN);
        graphicsContext.setFont(Font.font("Arial", 54.0));
        graphicsContext.fillText("Victory", width * 0.40, height * 0.42);

        graphicsContext.setFill(Color.WHITE);
        graphicsContext.setFont(Font.font("Arial", 24.0));
        graphicsContext.fillText("Press ENTER or click to restart", width * 0.31, height * 0.50);
    }
}
