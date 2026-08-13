package com.soulknight.entity;

import com.soulknight.engine.GameWorld;
import com.soulknight.event.GameEventListener;
import com.soulknight.utils.Constants;
import com.soulknight.utils.Vector2D;
import com.soulknight.weapon.Bullet;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

public class Boss extends Enemy {

    private final int maxHealth;
    private int currentPhase = 1;
    private double patternTimer = 0.0;
    private double attackCooldown = 0.0;
    private double spiralAngle = 0.0;

    public Boss(Vector2D spawnPoint, int health, int contactDamage, GameEventListener eventListener) {
        // Tận dụng constructor của Enemy (dùng RANGED_ELITE hoặc một Archetype phù hợp)
        super(EnemyArchetype.RANGED_ELITE, spawnPoint, Constants.ENEMY_RADIUS * 2.2, health, 50.0, contactDamage, null, eventListener);
        this.maxHealth = health;
    }

    @Override
    public void update(GameWorld world, double deltaSeconds) {
        // Cập nhật Phase dựa trên tỉ lệ máu hiện tại
        double healthPercent = (double) getHealth() / maxHealth;
        if (healthPercent <= 0.3) {
            currentPhase = 3;
        } else if (healthPercent <= 0.6) {
            currentPhase = 2;
        } else {
            currentPhase = 1;
        }

        patternTimer += deltaSeconds;
        attackCooldown = Math.max(0.0, attackCooldown - deltaSeconds);

        // AI di chuyển và bắn đạn dạng Pattern
        if (world.getPlayer() != null && world.getPlayer().getPosition() != null) {
            Vector2D playerPos = world.getPlayer().getPosition();

            // Di chuyển chậm về phía Player
            Vector2D dir = playerPos.copy().subtract(getPosition());
            if (dir.length() > 0) {
                dir.normalize();
                double moveSpeed = (currentPhase == 3) ? 75.0 : 45.0; // Phase 3 chạy nhanh hơn
                move(world, dir.getX() * moveSpeed * deltaSeconds, dir.getY() * moveSpeed * deltaSeconds);
            }

            // Xả đạn theo Phase
            if (attackCooldown <= 0.0) {
                executePattern(world, playerPos);
            }
        }

        super.update(world, deltaSeconds);
    }

    private void executePattern(GameWorld world, Vector2D playerPos) {
        switch (currentPhase) {
            case 1 -> {
                // Phase 1: Bắn vòng tròn 12 viên đạn
                spawnRingBullets(world, 12, 220.0);
                attackCooldown = 1.8;
            }
            case 2 -> {
                // Phase 2: Bắn xoắn ốc (Spiral)
                spawnSpiralBullet(world, 260.0);
                attackCooldown = 0.15; // Bắn liên tục
            }
            case 3 -> {
                // Phase 3: Cuồng nộ - Kết hợp Bắn Shotgun về phía Player + Vòng tròn
                spawnShotgunSpread(world, playerPos, 7, 320.0);
                if (patternTimer % 1.0 < 0.2) {
                    spawnRingBullets(world, 16, 240.0);
                }
                attackCooldown = 0.8;
            }
        }
    }

    private void spawnRingBullets(GameWorld world, int count, double speed) {
        double angleStep = 360.0 / count;
        for (int i = 0; i < count; i++) {
            double rad = Math.toRadians(i * angleStep);
            Vector2D velocity = new Vector2D(Math.cos(rad), Math.sin(rad)).scale(speed);
            Bullet bullet = new Bullet(getPosition().copy(), velocity, getContactDamage(), 8.0, this, Color.PURPLE);
            world.addBullet(bullet);
        }
    }

    private void spawnSpiralBullet(GameWorld world, double speed) {
        spiralAngle += 22.5;
        double rad = Math.toRadians(spiralAngle);
        Vector2D velocity = new Vector2D(Math.cos(rad), Math.sin(rad)).scale(speed);
        Bullet bullet = new Bullet(getPosition().copy(), velocity, getContactDamage(), 7.0, this, Color.ORANGE);
        world.addBullet(bullet);
    }

    private void spawnShotgunSpread(GameWorld world, Vector2D targetPos, int count, double speed) {
        Vector2D dir = targetPos.copy().subtract(getPosition());
        if (dir.length() > 0) dir.normalize();

        double baseAngle = Math.atan2(dir.getY(), dir.getX());
        double spread = Math.toRadians(40.0); // Bán kính xòe 40 độ
        double startAngle = baseAngle - spread / 2.0;
        double step = spread / (count - 1);

        for (int i = 0; i < count; i++) {
            double angle = startAngle + i * step;
            Vector2D velocity = new Vector2D(Math.cos(angle), Math.sin(angle)).scale(speed);
            Bullet bullet = new Bullet(getPosition().copy(), velocity, getContactDamage(), 8.0, this, Color.RED);
            world.addBullet(bullet);
        }
    }

    public int getMaxHealth() { return maxHealth; }
    public int getCurrentPhase() { return currentPhase; }

    public void setCurrentPhase(int phase) {
        this.currentPhase = phase;
    }
}