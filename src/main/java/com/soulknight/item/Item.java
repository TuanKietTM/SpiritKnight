package com.soulknight.item;

import com.soulknight.engine.Camera;
import com.soulknight.event.GameEventListener;
import com.soulknight.utils.Vector2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public abstract class Item {

    private final String name;
    private final Vector2D position;
    private final double radius;
    private final Color color;
    private final GameEventListener listener;
    private boolean collected;

    protected Item(String name, Vector2D position, double radius, Color color, GameEventListener listener) {
        this.name = name;
        this.position = position;
        this.radius = radius;
        this.color = color;
        this.listener = listener;
    }

    public void render(GraphicsContext graphicsContext, Camera camera) {
        double screenX = camera.worldToScreenX(position.getX());
        double screenY = camera.worldToScreenY(position.getY());
        graphicsContext.setFill(color);
        graphicsContext.fillOval(screenX - radius, screenY - radius, radius * 2.0, radius * 2.0);
    }

    public boolean intersects(Vector2D point, double pointRadius) {
        return position.distance(point) <= radius + pointRadius;
    }

    public void collect() {
        if (collected) {
            return;
        }
        collected = true;
        if (listener != null) {
            listener.onItemCollected(this);
        }
    }

    public boolean isCollected() {
        return collected;
    }

    public String getName() {
        return name;
    }

    public Vector2D getPosition() {
        return position;
    }
}
