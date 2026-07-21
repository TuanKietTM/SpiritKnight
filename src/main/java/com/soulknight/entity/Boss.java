package com.soulknight.entity;

import com.soulknight.engine.GameWorld;
import com.soulknight.event.GameEventListener;
import com.soulknight.utils.Constants;
import com.soulknight.utils.Vector2D;
import com.soulknight.weapon.Bullet;
import com.soulknight.weapon.Gun;
import com.soulknight.weapon.Weapon;
import java.util.Random;
import javafx.scene.paint.Color;

public final class Boss extends Enemy {

    private final Weapon volleyWeapon = new Gun("Boss Cannon", 18, 0.8, 420.0, 0.0);
    private final Random random = new Random();
    private double volleyTimer;

    public Boss(Vector2D spawnPoint, int health, int damage, GameEventListener eventListener) {
        super(EnemyArchetype.GRAND_KNIGHT, spawnPoint, Constants.BOSS_RADIUS, health, 95.0, damage, null, eventListener);
    }


    @Override
    public void update(GameWorld world, double deltaSeconds) {
        super.update(world, deltaSeconds);
        volleyTimer += deltaSeconds;
        volleyWeapon.tick(deltaSeconds);

        if (volleyTimer >= 1.2 && world.getPlayer().isAlive()) {
            volleyTimer = 0.0;
            fireVolley(world);
        }
    }

    private void fireVolley(GameWorld world) {
        Vector2D target = world.getPlayer().getPosition();
        Vector2D direction = target.copy().subtract(getPosition()).normalize();
        double baseAngle = Math.atan2(direction.getY(), direction.getX());
        int bulletCount = 6;

        for (int i = 0; i < bulletCount; i++) {
            double spread = baseAngle + (-0.45 + (0.9 / (bulletCount - 1)) * i);
            Vector2D velocity = new Vector2D(Math.cos(spread), Math.sin(spread)).scale(360.0);
            world.addBullet(new Bullet(getPosition().copy(), velocity, 14, 4.0, this, Color.MEDIUMPURPLE));
        }
    }

    @Override
    public void render(javafx.scene.canvas.GraphicsContext graphicsContext, com.soulknight.engine.Camera camera) {
        double screenX = camera.worldToScreenX(getPosition().getX());
        double screenY = camera.worldToScreenY(getPosition().getY());
        graphicsContext.setFill(Color.PURPLE);
        graphicsContext.fillOval(screenX - getRadius(), screenY - getRadius(), getRadius() * 2.0, getRadius() * 2.0);
    }
}
