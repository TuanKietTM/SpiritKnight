package com.soulknight.entity;

import com.soulknight.engine.Camera;
import com.soulknight.engine.GameWorld;
import com.soulknight.utils.Vector2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public abstract class Entity {

    private final Vector2D position;
    private final double radius;
    private final Color color;
    private final int maxHealth;
    private int health;

    protected Entity(Vector2D position, double radius, int maxHealth, Color color) {
        this.position = position;
        this.radius = radius;
        this.maxHealth = maxHealth;
        this.health = maxHealth;
        this.color = color;
    }

    public abstract void update(GameWorld world, double deltaSeconds);

    public void render(GraphicsContext graphicsContext, Camera camera) {
        double screenX = camera.worldToScreenX(position.getX());
        double screenY = camera.worldToScreenY(position.getY());
        graphicsContext.setFill(color);
        graphicsContext.fillOval(screenX - radius, screenY - radius, radius * 2.0, radius * 2.0);
    }

    public void move(GameWorld world, double deltaX, double deltaY) {
        Vector2D nextPosition = position.copy().add(deltaX, deltaY);
        if (world.canMoveTo(nextPosition, radius)) {
            position.set(nextPosition);
            return;
        }

        Vector2D horizontalMove = position.copy().add(deltaX, 0.0);
        if (world.canMoveTo(horizontalMove, radius)) {
            position.set(horizontalMove);
        }

        Vector2D verticalMove = position.copy().add(0.0, deltaY);
        if (world.canMoveTo(verticalMove, radius)) {
            position.set(verticalMove);
        }
    }

    public boolean isAlive() {
        return health > 0;
    }

    public void takeDamage(int amount) {
        health = Math.max(0, health - amount);
    }

    public Vector2D getPosition() {
        return position;
    }

    public double getRadius() {
        return radius;
    }

    public int getHealth() {
        return health;
    }

    public int getMaxHealth() {
        return maxHealth;
    }
    public void setHealth(int health) {
        this.health = Math.max(0, health);
    }
}
