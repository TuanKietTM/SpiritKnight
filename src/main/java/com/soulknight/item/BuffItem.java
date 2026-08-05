package com.soulknight.item;

import com.soulknight.buff.BuffType;
import com.soulknight.event.GameEventListener;
import com.soulknight.utils.ResourceLoader;
import com.soulknight.utils.Vector2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import com.soulknight.engine.Camera;

/**
 * cac buff nhat duoc hien thi tren ban do
 */
public class BuffItem extends Item {

    private final BuffType buffType;
    private final Image image;

    public BuffItem(Vector2D position, BuffType buffType, GameEventListener listener) {
        super(buffType.getDisplayName(), position, 18.0, Color.LIMEGREEN, listener);
        this.buffType = buffType;
        this.image = ResourceLoader.image(buffType.getImagePath());
    }

    @Override
    public void render(GraphicsContext graphicsContext, Camera camera) {
        if (isCollected()) {
            return;
        }

        double time = System.nanoTime() / 1_000_000_000.0;
        double bobOffset = Math.sin(time * 3.2) * 3.0;
        double pulse = 1.0 + Math.sin(time * 4.5) * 0.06;

        double screenX = camera.worldToScreenX(getPosition().getX());
        double screenY = camera.worldToScreenY(getPosition().getY()) + bobOffset;
        double size = 36.0 * pulse;

        graphicsContext.save();
        graphicsContext.setGlobalAlpha(0.22);
        graphicsContext.setFill(Color.LIGHTGREEN);
        graphicsContext.fillOval(screenX - 24.0, screenY - 24.0, 48.0, 48.0);
        graphicsContext.restore();

        if (image != null) {
            graphicsContext.drawImage(image, screenX - size / 2.0, screenY - size / 2.0, size, size);
            return;
        }

        super.render(graphicsContext, camera);
    }

    public BuffType getBuffType() {
        return buffType;
    }
}