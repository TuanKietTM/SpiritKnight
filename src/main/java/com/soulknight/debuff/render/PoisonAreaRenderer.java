package com.soulknight.debuff.render;

import com.soulknight.engine.Camera;
import com.soulknight.utils.Vector2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class PoisonAreaRenderer {

    /**
     * Vẽ vùng độc dưới đất theo phong cách Pixel Art
     */
    public static void renderGroundPoison(GraphicsContext gc, Camera camera, Vector2D position, double radius, double stateTime) {
        double screenX = camera.worldToScreenX(position.getX());
        double screenY = camera.worldToScreenY(position.getY());
        double zoom = camera.getZoom();

        double sizeX = radius * 2.2 * zoom;
        double sizeY = radius * 1.4 * zoom;

        gc.save();

        gc.setFill(Color.web("#2D0036"));
        gc.fillRect(screenX - sizeX / 2, screenY - sizeY / 2, sizeX, sizeY);
        gc.setFill(Color.web("#6A0D83"));
        gc.fillRect(screenX - sizeX / 2 + 2, screenY - sizeY / 2 + 2, sizeX - 4, sizeY - 4);
        gc.setFill(Color.web("#9B26B6"));
        gc.fillRect(screenX - sizeX / 4, screenY - sizeY / 4, sizeX / 2, sizeY / 2);
        double bubbleCycle = (stateTime * 4) % Math.PI;
        double bubbleOffset = Math.sin(bubbleCycle) * 3;

        gc.setFill(Color.web("#E066FF")); // Tím sáng
        gc.fillRect(screenX - sizeX * 0.25, screenY - bubbleOffset, 4, 4);
        gc.fillRect(screenX + sizeX * 0.15, screenY - sizeY * 0.2 - (3 - bubbleOffset), 3, 3);
        gc.restore();
    }
}