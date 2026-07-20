package com.soulknight.entity;

import com.soulknight.engine.GameWorld;
import com.soulknight.utils.Constants;
import com.soulknight.utils.Vector2D;
import com.soulknight.weapon.Gun;
import com.soulknight.weapon.Weapon;
import com.soulknight.entity.PlayerAnimator;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;

public final class Player extends Entity {

    private Weapon weapon = new Gun("Blaster", 12, 0.18, 580.0, 0.0);

    private final PlayerAnimator animator = new PlayerAnimator();
    private boolean isFacingLeft = false;
    private PlayerAnimator.State movementState = PlayerAnimator.State.IDLE;
    private double invulnerabilityTimer = 0.0;
//    thoi gian bat tu khi trung don
    private final double MAX_INVULNERABILITY_TIME = 0.3;

    public Player(Vector2D spawnPoint) {
        super(spawnPoint, Constants.PLAYER_RADIUS, Constants.PLAYER_HEALTH, Color.DODGERBLUE);
    }

    public String getWeaponName() {
        return weapon.getName();
    }

    public void equipWeapon(Weapon weapon) {
        this.weapon = weapon;
    }
//    tao thoi gian bat tu de giam khung lai
    public void takeDamage(int amount){
        if(invulnerabilityTimer>0.0){
            return;
        }
        super.takeDamage(amount);
        this.invulnerabilityTimer=MAX_INVULNERABILITY_TIME;
    }

//    Chu y cac ham xu li su kien ban phim , gọi getInputHandler tu gameworld
    @Override
    public void update(GameWorld world, double deltaSeconds) {
        if(invulnerabilityTimer>0.0){
            invulnerabilityTimer = Math.max(0.0, invulnerabilityTimer - deltaSeconds);
        }
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
            this.movementState = PlayerAnimator.State.RUN;
//            Xu li viec nhin theo huong di
            if (dx < 0) {
                isFacingLeft = true;
            } else if (dx > 0) {
                isFacingLeft = false;
            }

            movement.normalize().scale(Constants.PLAYER_SPEED * deltaSeconds);
            this.move(world, movement.getX(), movement.getY());
        } else {
            this.movementState = PlayerAnimator.State.IDLE;
//            Xu li viec nhin theo chuot de ngam ban
            Vector2D mousePos = world.getMouseWorldPosition();
            if (mousePos != null) {
                double diffX = mousePos.getX() - getPosition().getX();
                isFacingLeft = (diffX < 0);
            }
        }
        animator.update(movementState, deltaSeconds);

        if (world.getInputHandler().isFireHeld() && world.getMouseWorldPosition()!=null) {
            weapon.attack(world, this, world.getMouseWorldPosition());
        }
    }

    @Override
    public void render(javafx.scene.canvas.GraphicsContext graphicsContext, com.soulknight.engine.Camera camera) {
        double worldWidth = 32;
        double worldHeight = 43.0;
        animator.render(
                graphicsContext,
                camera,
                getPosition().getX(),
                getPosition().getY(),
                worldWidth,
                worldHeight,
                getRadius(),
                isFacingLeft
        );
    }
}