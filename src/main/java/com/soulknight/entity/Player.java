package com.soulknight.entity;

import com.soulknight.engine.GameWorld;
import com.soulknight.utils.Constants;
import com.soulknight.utils.Vector2D;
import com.soulknight.weapon.Gun;
import com.soulknight.weapon.Weapon;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;

public final class Player extends Entity {

    private Weapon weapon = new Gun("Blaster", 12, 0.18, 580.0, 0.0);

    public Player(Vector2D spawnPoint) {
        super(spawnPoint, Constants.PLAYER_RADIUS, Constants.PLAYER_HEALTH, Color.DODGERBLUE);
    }

    public String getWeaponName() {
        return weapon.getName();
    }

    public void equipWeapon(Weapon weapon) {
        this.weapon = weapon;
    }

    @Override
    public void update(GameWorld world, double deltaSeconds) {
        weapon.tick(deltaSeconds);

        double dx = 0.0;
        double dy = 0.0;

        if (world.getInputHandler().isDown(KeyCode.W) || world.getInputHandler().isDown(KeyCode.UP)) {
            dy -= 1.0;
        }
        if (world.getInputHandler().isDown(KeyCode.S) || world.getInputHandler().isDown(KeyCode.DOWN)) {
            dy += 1.0;
        }
        if (world.getInputHandler().isDown(KeyCode.A) || world.getInputHandler().isDown(KeyCode.LEFT)) {
            dx -= 1.0;
        }
        if (world.getInputHandler().isDown(KeyCode.D) || world.getInputHandler().isDown(KeyCode.RIGHT)) {
            dx += 1.0;
        }

        Vector2D movement = new Vector2D(dx, dy);
        if (movement.length() > 0.0) {
            movement.normalize().scale(Constants.PLAYER_SPEED * deltaSeconds);
            move(world, movement.getX(), movement.getY());
        }

        if (world.getInputHandler().isFireHeld()) {
            weapon.attack(world, this, world.getMouseWorldPosition());
        }
    }

    @Override
    public void render(javafx.scene.canvas.GraphicsContext graphicsContext, com.soulknight.engine.Camera camera) {
        super.render(graphicsContext, camera);
    }
}
