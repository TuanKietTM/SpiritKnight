package com.soulknight.entity;

import com.soulknight.engine.Camera;
import com.soulknight.engine.GameWorld;
import com.soulknight.event.GameEventListener;
import com.soulknight.utils.SoundManager;
import com.soulknight.utils.Vector2D;
import com.soulknight.weapon.Bullet;
import com.soulknight.weapon.render.IonProjectileRenderer;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class Boss extends Enemy {

    private final int maxHealth;
    private int currentPhase = 1;
    private double patternTimer = 0.0;
    private double attackCooldown = 0.0;
    private double spiralAngle = 0.0;
    private double shockwaveTimer = 0.0;

    private double stepSoundTimer = 0.0;
    private boolean playedDeathSound = false;

    private final BossAnimator bossAnimator;
    private boolean isMoving = false;

    public Boss(Vector2D spawnPoint, int health, int contactDamage, GameEventListener eventListener) {
        super(EnemyArchetype.RANGED_ELITE, spawnPoint, 30, health, 50.0, contactDamage, null, eventListener);
        this.maxHealth = health;
        this.bossAnimator = new BossAnimator();
    }

    @Override
    public void update(GameWorld world, double deltaSeconds) {
        if (!isAlive()) {
            bossAnimator.update(BossAnimator.State.DIE, deltaSeconds);

            if (!playedDeathSound) {
                SoundManager.getInstance().playSFX("boss_die");
                playedDeathSound = true;
            }
            return;
        }


        double healthPercent = (double) getHealth() / maxHealth;
        int oldPhase = currentPhase;

        if (healthPercent <= 0.3) {
            currentPhase = 3;
        } else if (healthPercent <= 0.6) {
            currentPhase = 2;
        } else {
            currentPhase = 1;
        }

        if (currentPhase > oldPhase) {
            SoundManager.getInstance().playSFX("boss_roar");
        }

        patternTimer += deltaSeconds;
        shockwaveTimer += deltaSeconds;
        attackCooldown = Math.max(0.0, attackCooldown - deltaSeconds);
        stepSoundTimer += deltaSeconds;

        isMoving = false;

        if (world.getPlayer() != null && world.getPlayer().getPosition() != null) {
            Vector2D playerPos = world.getPlayer().getPosition();

            setFacingLeft(playerPos.getX() < getPosition().getX());

            Vector2D dir = playerPos.copy().subtract(getPosition());
            if (dir.length() > 0) {
                dir.normalize();
                double moveSpeed = (currentPhase == 3) ? 75.0 : 45.0;

                Vector2D oldPos = getPosition().copy();
                move(world, dir.getX() * moveSpeed * deltaSeconds, dir.getY() * moveSpeed * deltaSeconds);

                if (getPosition().distance(oldPos) > 0.1) {
                    isMoving = true;
                    if (stepSoundTimer >= 0.4) {
                        SoundManager.getInstance().playSFX("boss_step");
                        stepSoundTimer = 0.0;
                    }
                }
            }

            if (attackCooldown <= 0.0) {
                executePattern(world, playerPos);
            }
        }

        BossAnimator.State animState = isMoving ? BossAnimator.State.RUN : BossAnimator.State.IDLE;
        bossAnimator.update(animState, deltaSeconds);

        if (world.getEnemies() != null) {
            separateFromOtherEnemies(world, world.getEnemies(), deltaSeconds);
        }
    }

    @Override
    public void render(GraphicsContext gc, Camera camera) {
        double renderWidth = getRadius() * 4.0;
        double renderHeight = getRadius() * 4.0;

        bossAnimator.render(
                gc,
                camera,
                getPosition().getX(),
                getPosition().getY(),
                renderWidth,
                renderHeight,
                getRadius(),
                isFacingLeft()
        );
    }

    private void executePattern(GameWorld world, Vector2D playerPos) {
        switch (currentPhase) {
            case 1 -> {
                spawnRingIonBullets(world, 12, 220.0, 0.6, Color.PURPLE);
                spawnShockwave(world, 160.0, 300.0, 16.0, getContactDamage(), Color.RED);
                spawnScratchArea(world, 2, 80.0, 25.0, 3.0, 4);
                attackCooldown = 1.8;
            }
            case 2 -> {
                spawnSpiralIonBullet(world, 260.0, 0.8, Color.ORANGE);
                if (shockwaveTimer >= 3.0) {
                    spawnShockwave(world, 200.0, 380.0, 18.0, getContactDamage(), Color.ORANGE);
                    spawnScratchArea(world, 4, 120.0, 30.0, 4.0, 6);
                    shockwaveTimer = 0.0;
                }
                attackCooldown = 0.15;
            }
            case 3 -> {
                spawnShotgunIonSpread(world, playerPos, 7, 320.0, 1.0, Color.RED);
                if (patternTimer % 1.0 < 0.2) {
                    spawnRingIonBullets(world, 16, 240.0, 0.7, Color.DEEPPINK);
                }
                if (shockwaveTimer >= 2.5) {
                    spawnShockwave(world, 280.0, 420.0, 24.0, (int)(getContactDamage() * 1.5), Color.DARKRED);
                    spawnScratchMark(world, playerPos.copy(), 35.0, 5.0, 10);
                    spawnScratchArea(world, 6, 160.0, 35.0, 5.0, 8);
                    shockwaveTimer = 0.0;
                }
                attackCooldown = 0.8;
            }
        }
    }

    private void spawnShockwave(GameWorld world, double maxRadius, double expandSpeed, double thickness, int damage, Color color) {
        if (world != null && getPosition() != null) {
            SoundManager.getInstance().playSFX("boss_stomp");

            world.spawnShockwave(
                    getPosition().copy(),
                    maxRadius,
                    expandSpeed,
                    thickness,
                    damage,
                    color,
                    false
            );
        }
    }

    private void spawnIonBullet(GameWorld world, Vector2D velocity, double radius, double chargeRatio, Color color) {
        Bullet bullet = new Bullet(
                getPosition().copy(),
                velocity,
                getContactDamage(),
                radius,
                this,
                color,
                true,
                false
        );

        bullet.withRenderer(new IonProjectileRenderer(chargeRatio));
        bullet.setPiercesObstacles(false);
        bullet.withTerrainExplosion(radius * 4.0, getContactDamage());

        world.addBullet(bullet);
    }

    private void spawnRingIonBullets(GameWorld world, int count, double speed, double chargeRatio, Color color) {
        SoundManager.getInstance().playSFX("boss_shoot_ring");

        double angleStep = 360.0 / count;
        for (int i = 0; i < count; i++) {
            double rad = Math.toRadians(i * angleStep);
            Vector2D velocity = new Vector2D(Math.cos(rad), Math.sin(rad)).scale(speed);
            spawnIonBullet(world, velocity, 10.0, chargeRatio, color);
        }
    }

    private void spawnSpiralIonBullet(GameWorld world, double speed, double chargeRatio, Color color) {
        SoundManager.getInstance().playSFX("boss_shoot_laser");

        spiralAngle += 22.5;
        double rad = Math.toRadians(spiralAngle);
        Vector2D velocity = new Vector2D(Math.cos(rad), Math.sin(rad)).scale(speed);
        spawnIonBullet(world, velocity, 8.0, chargeRatio, color);
    }

    private void spawnShotgunIonSpread(GameWorld world, Vector2D targetPos, int count, double speed, double chargeRatio, Color color) {

        SoundManager.getInstance().playSFX("boss_shotgun");

        Vector2D dir = targetPos.copy().subtract(getPosition());
        if (dir.length() > 0) dir.normalize();

        double baseAngle = Math.atan2(dir.getY(), dir.getX());
        double spread = Math.toRadians(40.0);
        double startAngle = baseAngle - spread / 2.0;
        double step = spread / (count - 1);

        for (int i = 0; i < count; i++) {
            double angle = startAngle + i * step;
            Vector2D velocity = new Vector2D(Math.cos(angle), Math.sin(angle)).scale(speed);
            spawnIonBullet(world, velocity, 12.0, chargeRatio, color);
        }
    }
    private void spawnScratchMark(GameWorld world, Vector2D position, double radius, double lifetime, int dps) {
        if (world != null && position != null) {
            world.spawnScratchMark(position, radius, lifetime, dps);
        }
    }
    private void spawnScratchArea(GameWorld world, int count, double areaRadius, double markRadius, double lifetime, int dps) {
        if (world == null || getPosition() == null) return;

        for (int i = 0; i < count; i++) {
            double offsetX = (Math.random() - 0.5) * 2.0 * areaRadius;
            double offsetY = (Math.random() - 0.5) * 2.0 * areaRadius;
            Vector2D spawnPos = getPosition().copy().add(new Vector2D(offsetX, offsetY));

            world.spawnScratchMark(spawnPos, markRadius, lifetime, dps);
        }
    }

    public int getMaxHealth() { return maxHealth; }
    public int getCurrentPhase() { return currentPhase; }
    public BossAnimator getBossAnimator() {
        return bossAnimator;
    }
    public boolean isDeathAnimationFinished() {
        return bossAnimator != null && bossAnimator.isDeathAnimationFinished();
    }
}