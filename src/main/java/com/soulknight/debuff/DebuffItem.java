package com.soulknight.debuff;

import com.soulknight.debuff.render.PoisonAreaRenderer;
import com.soulknight.engine.Camera;
import com.soulknight.event.GameEventListener;
import com.soulknight.item.Item;
import com.soulknight.utils.Vector2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class DebuffItem extends Item {
    /**
     * Tao cac icon debuff
     */
    private final DebuffType debuffType;
    private double stateTime = 0.0;

    public DebuffItem(DebuffType debuffType, Vector2D position, GameEventListener listener) {
        super(debuffType.getDisplayName(), position, 14.0, debuffType.getColor(), listener);
        this.debuffType = debuffType;
    }

    @Override
    public void update(double deltaSeconds, Vector2D playerPosition) {
        super.update(deltaSeconds, playerPosition);
        if (!isCollected() && deltaSeconds > 0.0) {
            this.stateTime += deltaSeconds; // Cập nhật thời gian animation
        }
    }
    @Override
    public void render(GraphicsContext gc, Camera camera) {
        if (isCollected()) {
            return;
        }

        if (debuffType == DebuffType.POISON) {
            PoisonAreaRenderer.renderGroundPoison(gc, camera, getPosition(), getRadius(), stateTime);
            return;
        }

        double screenX = camera.worldToScreenX(getPosition().getX());
        double screenY = camera.worldToScreenY(getPosition().getY());
        double zoom = camera.getZoom();
        double size = getRadius() * 2.0 * zoom;

        gc.save();

        double offsetY = Math.sin(stateTime * 6.0) * 3.0;

        if (debuffType.getSprite() != null) {
            // Vẽ Sprite LibreSprite
            gc.drawImage(
                    debuffType.getSprite(),
                    screenX - size / 2,
                    screenY - size / 2 + offsetY,
                    size,
                    size
            );
        } else {
            double pulse = 0.8 + 0.2 * Math.sin(stateTime * 5.0);
            gc.setFill(Color.BLACK);
            gc.fillRect(screenX - size / 2 - 2, screenY - size / 2 - 2 + offsetY, size + 4, size + 4);
            gc.setFill(debuffType.getColor());
            gc.setGlobalAlpha(pulse);
            gc.fillRect(screenX - size / 2, screenY - size / 2 + offsetY, size, size);
        }

        gc.restore();
    }

    public DebuffType getDebuffType() {
        return debuffType;
    }
}