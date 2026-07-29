package com.soulknight.animation;

import com.soulknight.engine.Camera;
import com.soulknight.utils.Vector2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class Particle {
    private Vector2D position;
    private Vector2D velocity;
    private double size;
    private Color color;
    private double lifeTime;
    private double maxLifeTime;

    public Particle(Vector2D position, Vector2D velocity, double size, Color color, double lifeTime) {
        this.position = new Vector2D(position.getX(), position.getY());
        this.velocity = velocity;
        this.size = size;
        this.color = color;
        this.lifeTime = lifeTime;
        this.maxLifeTime = lifeTime;
    }

    public void update(double deltaSeconds) {
        position.setX(position.getX() + velocity.getX() * deltaSeconds);
        position.setY(position.getY() + velocity.getY() * deltaSeconds);
        lifeTime -= deltaSeconds;
    }

    public boolean isDead() {
        return lifeTime <= 0;
    }

    public void render(GraphicsContext gc, Camera camera) {
        if (isDead()) return;

        double screenX = camera.worldToScreenX(position.getX());
        double screenY = camera.worldToScreenY(position.getY());
        double zoom = camera.getZoom();

        double alpha = Math.max(0, lifeTime / maxLifeTime);

        gc.save();
        gc.setGlobalAlpha(alpha);
        gc.setFill(color);
        gc.fillRect(screenX, screenY, size * zoom, size * zoom);
        gc.restore();
    }
}