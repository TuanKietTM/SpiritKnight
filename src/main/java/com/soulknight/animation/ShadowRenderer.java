package com.soulknight.animation;

import com.soulknight.engine.Camera;
import com.soulknight.utils.Vector2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Class lam hieu ung do bong duoi chan cac doi tuong player , pet , enemy torng game
 */
public final class ShadowRenderer {
    private static final Color SHADOW_INNER_COLOR = Color.rgb(20, 15, 10, 0.55);
    private static final Color GREEN_OUTLINE_COLOR = Color.rgb(50, 220, 90, 0.95);

    private ShadowRenderer() {
    }

    public static void render(GraphicsContext gc, Camera camera, Vector2D position,
                              double shadowWidth, double shadowHeight, double offsetY, boolean hasGreenOutline) {
        if (gc == null || camera == null || position == null) return;

        double zoom = camera.getZoom();
        double pSize = Math.max(2.0, Math.floor(2.5 * zoom));
        double screenX = Math.floor(camera.worldToScreenX(position.getX()) / pSize) * pSize;
        double screenY = Math.floor(camera.worldToScreenY(position.getY() + offsetY) / pSize) * pSize;
        int cols = (int) Math.max(6, Math.round((shadowWidth * zoom) / pSize));
        int rows = (int) Math.max(1, Math.round((shadowHeight * zoom) / pSize));
        if (cols % 2 != 0) cols++;
        if (rows % 2 != 0) rows++;

        double startX = screenX - (cols * pSize) / 2.0;
        double startY = screenY - (rows * pSize) / 2.0;

        gc.save();
        if (hasGreenOutline) {
            gc.setFill(GREEN_OUTLINE_COLOR);
//            ve theo chieu hinh chu nhat tren duoi , trai phai
            gc.fillRect(startX + pSize, startY - pSize, (cols - 2) * pSize, pSize);
            gc.fillRect(startX + pSize, startY + rows * pSize, (cols - 2) * pSize, pSize);
            gc.fillRect(startX - pSize, startY + pSize, pSize, (rows - 2) * pSize);
            gc.fillRect(startX + cols * pSize, startY + pSize, pSize, (rows - 2) * pSize);

//         4 goc cheo
            gc.fillRect(startX, startY, pSize, pSize);
            gc.fillRect(startX + (cols - 1) * pSize, startY, pSize, pSize);
            gc.fillRect(startX, startY + (rows - 1) * pSize, pSize, pSize);
            gc.fillRect(startX + (cols - 1) * pSize, startY + (rows - 1) * pSize, pSize, pSize);
        }
        gc.setFill(SHADOW_INNER_COLOR);
        gc.fillRect(startX, startY + pSize, cols * pSize, (rows - 2) * pSize);
        gc.fillRect(startX + pSize, startY, (cols - 2) * pSize, pSize);
        gc.fillRect(startX + pSize, startY + (rows - 1) * pSize, (cols - 2) * pSize, pSize);

        gc.restore();
    }
    public static void render(GraphicsContext gc, Camera camera, Vector2D position, double shadowWidth, double shadowHeight) {
        render(gc, camera, position, shadowWidth, shadowHeight, 10.0, true);
    }
}