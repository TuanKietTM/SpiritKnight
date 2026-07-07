package com.soulknight.weapon;

import com.soulknight.engine.Camera;
import com.soulknight.entity.Entity;
import com.soulknight.utils.Vector2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public final class Bullet {

    private final Vector2D position;
    private final Vector2D velocity;
    private final int damage;
    private final double radius;
    private final Entity owner;
    private final Color color;
    private boolean active = true;

    public Bullet(Vector2D position, Vector2D velocity, int damage, double radius, Entity owner, Color color) {
        this.position = position;
        this.velocity = velocity;
        this.damage = damage;
        this.radius = radius;
        this.owner = owner;
        this.color = color;
    }

    public void update(double deltaSeconds) {
        position.add(velocity.copy().scale(deltaSeconds));
    }

    public void render(GraphicsContext graphicsContext, Camera camera) {
        double screenX = camera.worldToScreenX(position.getX());
        double screenY = camera.worldToScreenY(position.getY());
        graphicsContext.setFill(color);
        graphicsContext.fillOval(screenX - radius, screenY - radius, radius * 2.0, radius * 2.0);
    }

    public boolean intersects(Entity entity) {
        return position.distance(entity.getPosition()) <= radius + entity.getRadius();
    }

    public Vector2D getPosition() {
        return position;
    }

    public int getDamage() {
        return damage;
    }

    public double getRadius() {
        return radius;
    }

    public Entity getOwner() {
        return owner;
    }

    public boolean isActive() {
        return active;
    }

    public void deactivate() {
        active = false;
    }
}
