package com.soulknight.ui;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public final class Menu {

    public void render(GraphicsContext graphicsContext, double width, double height) {
        graphicsContext.setFill(Color.web("#0b1020"));
        graphicsContext.fillRect(0.0, 0.0, width, height);

        graphicsContext.setFill(Color.WHITE);
        graphicsContext.setFont(Font.font("Arial", 56.0));
        graphicsContext.fillText("Soul Knight", width * 0.34, height * 0.35);

        graphicsContext.setFont(Font.font("Arial", 24.0));
        graphicsContext.fillText("Press ENTER or click to start", width * 0.31, height * 0.45);
        graphicsContext.fillText("WASD to move, mouse to aim, hold left click to fire", width * 0.20, height * 0.52);
    }
}
