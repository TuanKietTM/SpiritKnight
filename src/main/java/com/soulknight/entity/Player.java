package com.soulknight.entity;

import com.soulknight.engine.GameWorld;
import com.soulknight.utils.Constants;
import com.soulknight.utils.Vector2D;
import com.soulknight.weapon.Gun;
import com.soulknight.weapon.Weapon;
import com.soulknight.entity.PlayerAnimator;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;

import java.util.List;

public final class Player extends Entity {

    private Weapon weapon = new Gun("Blaster", 12, 0.18, 580.0, 0.0);

    private final PlayerAnimator animator = new PlayerAnimator();
    private boolean isFacingLeft = false;
    private PlayerAnimator.State movementState = PlayerAnimator.State.IDLE;
    private double invulnerabilityTimer = 0.0;
    // Thời gian bất tử khi trúng đòn
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

    // Tạo thời gian bất tử để giảm đòn đánh liên tục
    @Override
    public void takeDamage(int amount) {
        if (invulnerabilityTimer > 0.0) {
            return;
        }
        super.takeDamage(amount);
        this.invulnerabilityTimer = MAX_INVULNERABILITY_TIME;
    }

    @Override
    public void update(GameWorld world, double deltaSeconds) {
        if (invulnerabilityTimer > 0.0) {
            invulnerabilityTimer = Math.max(0.0, invulnerabilityTimer - deltaSeconds);
        }
        weapon.tick(deltaSeconds);

        double dx = 0.0;
        double dy = 0.0;

        boolean isTouchpad = world.getInputHandler().isTouchpadModeEnabled();

        //  Kiểm tra di chuyển bằng Touchpad Joystick (360 do)
        Vector2D touchpadDir = world.getInputHandler().getTouchpadJoystick().getMoveDirection();

        if (touchpadDir.length() > 0.0) {
            dx = touchpadDir.getX();
            dy = touchpadDir.getY();
        } else {
            // Neu khong dung touchpad dung WASD , mui ten
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
        }

        // Xử lý di chuyển
        Vector2D movement = new Vector2D(dx, dy);
        if (movement.length() > 0.0) {
            this.movementState = PlayerAnimator.State.RUN;

            if (touchpadDir.length() == 0.0) {
                movement.normalize();
            }

            movement.scale(Constants.PLAYER_SPEED * deltaSeconds);
            this.move(world, movement.getX(), movement.getY());
        } else {
            this.movementState = PlayerAnimator.State.IDLE;
        }

        // 3. Xử lý hướng nhìn (Facing) và Ngắm bắn (Aiming)
        Vector2D attackTargetPos = null;

        if (isTouchpad) {
            // MODE  TOUCHPAD: AUTO-AIM & AUTO-FACE
            Enemy nearestEnemy = findNearestEnemy(world.getEnemies());

            if (nearestEnemy != null) {
                attackTargetPos = nearestEnemy.getPosition();
                // Quay mặt về phía kẻ địch đang bị khóa mục tiêu
                double diffX = attackTargetPos.getX() - getPosition().getX();
                isFacingLeft = (diffX < 0);
            } else {
                // Nếu không có kẻ địch xung quanh, quay mặt theo hướng di chuyển Joystick
                if (dx < 0) {
                    isFacingLeft = true;
                } else if (dx > 0) {
                    isFacingLeft = false;
                }
            }
        } else {
            //  MODE MOUSE & KEYBOARD
            Vector2D mousePos = world.getMouseWorldPosition();
            if (mousePos != null) {
                attackTargetPos = mousePos;
//               Quay mat knight theo con tro chuot
                double diffX = mousePos.getX() - getPosition().getX();
                isFacingLeft = (diffX < 0);
            } else if (movement.length() > 0.0) {
                if (dx < 0) isFacingLeft = true;
                else if (dx > 0) isFacingLeft = false;
            }
        }

        // Xu ly tan cong
        if (world.getInputHandler().isFireHeld() && attackTargetPos != null) {
            weapon.attack(world, this, attackTargetPos);
        }

        animator.update(movementState, deltaSeconds);
    }

    /**
     * Thuat ton tim kiem ke thu gan nhat
     */
    private Enemy findNearestEnemy(List<Enemy> enemies) {
        if (enemies == null || enemies.isEmpty()) {
            return null;
        }

        Enemy nearest = null;
        double minDistanceSq = Double.MAX_VALUE;

        for (Enemy enemy : enemies) {
            if (enemy == null || !enemy.isAlive()) {
                continue;
            }

            double distSq = getPosition().distanceSquared(enemy.getPosition());
            if (distSq < minDistanceSq) {
                minDistanceSq = distSq;
                nearest = enemy;
            }
        }

        return nearest;
    }

    @Override
    public void render(javafx.scene.canvas.GraphicsContext graphicsContext, com.soulknight.engine.Camera camera) {
        double worldWidth = 32.0;
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

    public double getSpeed() {
        return Constants.PLAYER_SPEED;
    }
}