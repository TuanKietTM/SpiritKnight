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

        double screenX = camera.worldToScreenX(getPosition().getX());
        double screenY = camera.worldToScreenY(getPosition().getY());

        if (image != null) {
            graphicsContext.drawImage(image, screenX - 18.0, screenY - 18.0, 36.0, 36.0);
            return;
        }

        super.render(graphicsContext, camera);
    }

    public BuffType getBuffType() {
        return buffType;
    }
}