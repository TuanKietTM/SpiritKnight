package com.soulknight.entity;

import com.soulknight.engine.Camera;
import com.soulknight.engine.GameWorld;
import com.soulknight.event.GameEventListener;
import com.soulknight.map.Obstacle;
import com.soulknight.utils.Vector2D;
import com.soulknight.weapon.Weapon;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.List;

public class Enemy extends Entity {

    private final EnemyArchetype archetype;
    private final double moveSpeed;
    private final int contactDamage;
    private final Weapon rangedWeapon;
    private final GameEventListener eventListener;
    private double attackCooldown;
    private boolean defeatNotified;

    private final EnemyAnimator animator;
    private boolean isFacingLeft = false;

    public Enemy(EnemyArchetype archetype, Vector2D spawnPoint, double radius, int health, double moveSpeed,
                 int contactDamage, Weapon rangedWeapon, GameEventListener eventListener) {
        super(spawnPoint, radius, health, colorFor(archetype));
        this.archetype = archetype;
        this.moveSpeed = moveSpeed;
        this.contactDamage = contactDamage;
        this.rangedWeapon = rangedWeapon;
        this.eventListener = eventListener;
        this.animator = new EnemyAnimator(archetype);
    }

    @Override
    public void update(GameWorld world, double deltaSeconds) {
        attackCooldown = Math.max(0.0, attackCooldown - deltaSeconds);
        if (rangedWeapon != null) {
            rangedWeapon.tick(deltaSeconds);
        }

        if (world.getEnemies() != null) {
            separateFromOtherEnemies(world, world.getEnemies(), deltaSeconds);
        }

        Vector2D playerPos = world.getPlayer().getPosition();
        Vector2D enemyPos = getPosition();
        double distanceToPlayer = enemyPos.distance(playerPos);

//      Tu xoay mat vao player
        double diffX = playerPos.getX() - enemyPos.getX();
        this.isFacingLeft = (diffX < 0);

//        Chia tung loai quai
        switch (archetype) {
            case SLIME -> standardChaseAndContact(world, playerPos, deltaSeconds);
            case SKELETON_ARCHER -> guardianAI(world, playerPos, distanceToPlayer, deltaSeconds);
            case ELITE_MINION -> wildBoarAI(world, playerPos, distanceToPlayer, deltaSeconds);
        }

        // Cập nhật khung hình Animator
        animator.update(deltaSeconds);
    }

    public void move(GameWorld world, double dx, double dy) {
        if (dx == 0 && dy == 0) return;

        Vector2D currentPos = getPosition();

        Vector2D newPos = new Vector2D(currentPos.getX() + dx, currentPos.getY() + dy);
        if (world.canMoveTo(newPos, getRadius())) {
            currentPos.set(newPos);
            return;
        }

        Vector2D posXOnly = new Vector2D(currentPos.getX() + dx, currentPos.getY());
        if (world.canMoveTo(posXOnly, getRadius())) {
            currentPos.set(posXOnly);
            return;
        }

        Vector2D posYOnly = new Vector2D(currentPos.getX(), currentPos.getY() + dy);
        if (world.canMoveTo(posYOnly, getRadius())) {
            currentPos.set(posYOnly);
        }
    }

    @Override
    public void render(GraphicsContext gc, Camera camera) {
        double renderWidth = getRadius() * 2.5;
        double renderHeight = getRadius() * 2.5;

        animator.render(
                gc,
                camera,
                getPosition().getX(),
                getPosition().getY(),
                renderWidth,
                renderHeight,
                getRadius(),
                isFacingLeft
        );
    }



    public void separateFromOtherEnemies(GameWorld world, List<Enemy> allEnemies, double deltaSeconds) {
        for (Enemy other : allEnemies) {
            if (other == this || !other.isAlive()) continue;

            double dist = getPosition().distance(other.getPosition());
            double minDist = this.getRadius() + other.getRadius();

            if (dist < minDist && dist > 0) {
                Vector2D pushDir = getPosition().copy().subtract(other.getPosition());
                pushDir.normalize();

                double overlap = minDist - dist;
                double pushSpeed = 80.0;

                this.move(world, pushDir.getX() * overlap * pushSpeed * deltaSeconds,
                        pushDir.getY() * overlap * pushSpeed * deltaSeconds);
            }
        }

        List<Obstacle> obstacles = world.getObstacles();
        if (obstacles != null) {
            for (Obstacle obstacle : obstacles) {
                if (obstacle.intersectsCircle(getPosition(), getRadius())) {
                    Vector2D obsCenter = obstacle.getCenter();
                    Vector2D pushDir = getPosition().copy().subtract(obsCenter);
                    if (pushDir.length() == 0) {
                        pushDir = new Vector2D(1.0, 0.0);
                    }
                    pushDir.normalize();

                    double pushDistance = 150.0 * deltaSeconds;
                    getPosition().add(pushDir.getX() * pushDistance, pushDir.getY() * pushDistance);
                }
            }
        }
    }

    private void guardianAI(GameWorld world, Vector2D playerPos, double distance, double deltaSeconds) {
        Vector2D toPlayer = playerPos.copy().subtract(getPosition());

        if (distance < 120.0) {
            Vector2D escapeDirection = toPlayer.copy().scale(-1.0);
            escapeDirection.add(new Vector2D(-toPlayer.getY(), toPlayer.getX()).scale(0.5));

            if (escapeDirection.length() > 0.0) {
                escapeDirection.normalize().scale(moveSpeed * 1.2 * deltaSeconds);
                move(world, escapeDirection.getX(), escapeDirection.getY());
            }

            if (distance <= 35.0 && attackCooldown <= 0.0) {
                world.getPlayer().takeDamage(contactDamage + 2);
                this.attackCooldown = 1.2;
            }
        } else {
            List<Obstacle> obstacles = world.getObstacles();
            if (obstacles != null && !obstacles.isEmpty() && distance < 280.0) {
                coverAI(world, playerPos, deltaSeconds);
            } else if (distance > 250.0) {
                Vector2D walkDir = toPlayer.copy();
                if (walkDir.length() > 0.0) {
                    walkDir.normalize().scale(moveSpeed * deltaSeconds);
                    move(world, walkDir.getX(), walkDir.getY());
                }
            }

            if (rangedWeapon != null && world.getPlayer().isAlive()) {
                rangedWeapon.attack(world, this, playerPos);
            }
        }
    }

    private void wildBoarAI(GameWorld world, Vector2D playerPos, double distance, double deltaSeconds) {
        Vector2D direction = playerPos.copy().subtract(getPosition());
        double currentSpeed = this.moveSpeed;

        double minAllowedDistance = world.getPlayer().getRadius() + this.getRadius() + 4.0;

        if (distance <= 180.0) {
            currentSpeed = this.moveSpeed * 2.2;
        }

        if (distance > minAllowedDistance) {
            if (direction.length() > 0.0) {
                direction.normalize().scale(currentSpeed * deltaSeconds);
                move(world, direction.getX(), direction.getY());
            }
        } else {
            Vector2D pushOut = getPosition().copy().subtract(playerPos);
            if (pushOut.length() > 0.0) {
                pushOut.normalize().scale(currentSpeed * 0.8 * deltaSeconds);
                move(world, pushOut.getX(), pushOut.getY());
            }
        }

        if (distance <= minAllowedDistance + 3.0 && attackCooldown <= 0.0) {
            world.getPlayer().takeDamage(contactDamage);
            this.attackCooldown = 1.0;
        }
    }

    private void standardChaseAndContact(GameWorld world, Vector2D playerPos, double deltaSeconds) {
        Vector2D direction = playerPos.copy().subtract(getPosition());
        double distance = direction.length();

        double minAllowedDistance = world.getPlayer().getRadius() + this.getRadius() + 5.0;

        if (distance > minAllowedDistance) {
            if (distance > 0.0) {
                direction.normalize().scale(moveSpeed * deltaSeconds);
                move(world, direction.getX(), direction.getY());
            }
        } else {
            Vector2D pushOut = getPosition().copy().subtract(playerPos);
            if (pushOut.length() > 0.0) {
                pushOut.normalize().scale(40.0 * deltaSeconds);
                move(world, pushOut.getX(), pushOut.getY());
            }
        }

        if (distance <= minAllowedDistance + 2.0 && attackCooldown <= 0.0) {
            world.getPlayer().takeDamage(contactDamage);
            attackCooldown = 0.9;
        }
    }

    public void coverAI(GameWorld world, Vector2D playerPos, double deltaSeconds) {
        List<Obstacle> obstacles = world.getObstacles();

        if (obstacles == null || obstacles.isEmpty()) {
            standardChaseAndContact(world, playerPos, deltaSeconds);
            return;
        }

        Obstacle closestObs = null;
        double minDistance = Double.MAX_VALUE;
        for (Obstacle obs : obstacles) {
            double dist = getPosition().distance(obs.getCenter());
            if (dist < minDistance) {
                minDistance = dist;
                closestObs = obs;
            }
        }

        if (closestObs != null) {
            Vector2D obsCenter = closestObs.getCenter();
            Vector2D awayFromPlayer = obsCenter.copy().subtract(playerPos).normalize();

            double obsRadius = Math.max(closestObs.getWidth(), closestObs.getHeight()) / 2.0;
            double safeHideDistance = obsRadius + getRadius() + 15.0;

            Vector2D coverPoint = obsCenter.copy().add(
                    awayFromPlayer.getX() * safeHideDistance,
                    awayFromPlayer.getY() * safeHideDistance
            );

            double distToCover = getPosition().distance(coverPoint);
            if (distToCover > 10.0 && world.canMoveTo(coverPoint, getRadius())) {
                Vector2D moveDir = coverPoint.copy().subtract(getPosition()).normalize();
                double moveStep = getSpeed() * deltaSeconds;
                this.move(world, moveDir.getX() * moveStep, moveDir.getY() * moveStep);
            }
        }
    }

    @Override
    public void takeDamage(int amount) {
        super.takeDamage(amount);
        if (!isAlive() && !defeatNotified) {
            defeatNotified = true;
            if (eventListener != null) {
                eventListener.onEnemyDefeated(this);
            }
        }
    }

    public EnemyArchetype getArchetype() {
        return archetype;
    }

    public int getContactDamage() {
        return contactDamage;
    }

    public Weapon getRangedWeapon() {
        return rangedWeapon;
    }

    public double getSpeed() {
        return moveSpeed;
    }

    protected static Color colorFor(EnemyArchetype archetype) {
        return switch (archetype) {
            case SLIME -> Color.BLUE;
            case SKELETON_ARCHER -> Color.GRAY;
            case ELITE_MINION -> Color.RED;
            default -> Color.RED;
        };
    }
}