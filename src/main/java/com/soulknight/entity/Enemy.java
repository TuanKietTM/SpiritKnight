package com.soulknight.entity;

import com.soulknight.entity.Boss;
import com.soulknight.engine.GameWorld;
import com.soulknight.event.GameEventListener;
import com.soulknight.utils.Vector2D;
import com.soulknight.weapon.Weapon;
import javafx.scene.paint.Color;

public class Enemy extends Entity {

    private final EnemyArchetype archetype;
    private final double moveSpeed;
    private final int contactDamage;
    private final Weapon rangedWeapon;
    private final GameEventListener eventListener;
    private double attackCooldown;
    private boolean defeatNotified;

    public Enemy(EnemyArchetype archetype, Vector2D spawnPoint, double radius, int health, double moveSpeed,
                 int contactDamage, Weapon rangedWeapon, GameEventListener eventListener) {
        super(spawnPoint, radius, health, colorFor(archetype));
        this.archetype = archetype;
        this.moveSpeed = moveSpeed;
        this.contactDamage = contactDamage;
        this.rangedWeapon = rangedWeapon;
        this.eventListener = eventListener;
    }

    @Override
    public void update(GameWorld world, double deltaSeconds) {
        attackCooldown = Math.max(0.0, attackCooldown - deltaSeconds);

        Vector2D direction = world.getPlayer().getPosition().copy().subtract(getPosition());
        if (direction.length() > 0.0) {
            direction.normalize().scale(moveSpeed * deltaSeconds);
            move(world, direction.getX(), direction.getY());
        }

        if (rangedWeapon != null) {
            rangedWeapon.tick(deltaSeconds);
            if (world.getPlayer().isAlive()) {
                rangedWeapon.attack(world, this, world.getPlayer().getPosition());
            }
        } else if (getPosition().distance(world.getPlayer().getPosition()) <= 26.0 && attackCooldown <= 0.0) {
            world.getPlayer().takeDamage(contactDamage);
            attackCooldown = 0.9;
        }
    }

    @Override
    public void takeDamage(int amount) {
        super.takeDamage(amount);
        if (!isAlive() && !defeatNotified) {
            defeatNotified = true;
            if (eventListener != null) {
                if (archetype == EnemyArchetype.GRAND_KNIGHT) {
                    eventListener.onBossDefeated((Boss) this);
                } else {
                    eventListener.onEnemyDefeated(this);
                }
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

    protected static Color colorFor(EnemyArchetype archetype) {
        return switch (archetype) {
            case SLIME -> Color.FORESTGREEN;
            case SKELETON_ARCHER -> Color.LIGHTGRAY;
            case ELITE_MINION -> Color.DARKRED;
            case GRAND_KNIGHT -> Color.PURPLE;
        };
    }
}
