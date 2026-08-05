package com.soulknight.item;

import com.soulknight.engine.Camera;
import com.soulknight.event.GameEventListener;
import com.soulknight.utils.ResourceLoader;
import com.soulknight.utils.Vector2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

public abstract class Item {

    private final String name;
    private final Vector2D position;
    private final double radius;
    private final Color fallbackColor;
    private final GameEventListener listener;
    private final Image image;

    private boolean collected;
    private boolean magnetEnabled;
    private double magnetRadius = 135.0;
    private double magnetSpeed = 260.0;
    private double age;
    private double spawnOffsetY;
    private double spawnVelocityY = -34.0;

    protected Item(String name, Vector2D position, double radius, Color color, GameEventListener listener) {
        this(name, position, radius, color, listener, null);
    }

    protected Item(String name, Vector2D position, double radius, Color color,
                   GameEventListener listener, String imagePath) {
        this.name = name;
        this.position = position;
        this.radius = radius;
        this.fallbackColor = color;
        this.listener = listener;
        this.image = imagePath == null || imagePath.isBlank() ? null : ResourceLoader.image(imagePath);
    }

    public void update(double deltaSeconds, Vector2D playerPosition) {
        if (collected || deltaSeconds <= 0.0) {
            return;
        }

        age += deltaSeconds;
        updateSpawnMotion(deltaSeconds);

        if (!magnetEnabled || playerPosition == null || age < 0.12) {
            return;
        }

        double dx = playerPosition.getX() - position.getX();
        double dy = playerPosition.getY() - position.getY();
        double distance = Math.hypot(dx, dy);

        if (distance <= 0.001 || distance > magnetRadius) {
            return;
        }

        double distanceFactor = 1.0 - Math.min(1.0, distance / magnetRadius);
        double speed = magnetSpeed * (0.45 + distanceFactor * 1.55);
        double step = Math.min(distance, speed * deltaSeconds);

        position.add(dx / distance * step, dy / distance * step);
    }

    private void updateSpawnMotion(double deltaSeconds) {
        if (Math.abs(spawnOffsetY) < 0.01 && spawnVelocityY == 0.0) {
            return;
        }

        spawnVelocityY += 145.0 * deltaSeconds;
        spawnOffsetY += spawnVelocityY * deltaSeconds;

        if (spawnOffsetY >= 0.0) {
            spawnOffsetY = 0.0;
            spawnVelocityY = 0.0;
        }
    }

    public void render(GraphicsContext graphicsContext, Camera camera) {
        double screenX = camera.worldToScreenX(position.getX());
        double screenY = camera.worldToScreenY(position.getY() + spawnOffsetY);

        double pulse = 1.0 + Math.sin(age * 5.0) * 0.05;
        double size = radius * 2.0 * pulse;

        if (image != null && image.getWidth() > 0.0) {
            graphicsContext.setImageSmoothing(false);
            graphicsContext.drawImage(image, screenX - size / 2.0, screenY - size / 2.0, size, size);
            return;
        }

        graphicsContext.setFill(fallbackColor);
        graphicsContext.fillOval(screenX - size / 2.0,
                screenY - size / 2.0, size, size);
    }

    public boolean intersects(Vector2D point, double pointRadius) {
        return point != null && position.distance(point) <= radius + pointRadius;
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

    protected void enableMagnet(double radius, double speed) {
        magnetEnabled = true;
        magnetRadius = Math.max(0.0, radius);
        magnetSpeed = Math.max(0.0, speed);
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

    public double getRadius() {
        return radius;
    }

}