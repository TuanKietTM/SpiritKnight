package com.soulknight.weapon;

import com.soulknight.engine.GameWorld;
import com.soulknight.entity.Entity;
import com.soulknight.utils.Vector2D;
import javafx.scene.paint.Color;

import java.util.Random;

public class Wand extends Weapon {

    private final double bulletSpeed;
    private static final int RAY_COUNT = 3;
    private static final double RAY_SPREAD = 25.0;
    private static final double RAY_LENGTH = 250.0;

    private static final Color[] RAY_COLORS = new Color[]{
            Color.RED, Color.ORANGE, Color.YELLOW,
            Color.GREEN, Color.CYAN, Color.DODGERBLUE, Color.MAGENTA
    };
    private final Random random = new Random();

    public Wand(String name, int damage, double cooldownSeconds, double bulletSpeed) {
        super(name, damage, cooldownSeconds);
        this.bulletSpeed = bulletSpeed;
    }

    @Override
    public void attack(GameWorld world, Entity owner, Vector2D targetPosition) {
        if (world == null || owner == null || targetPosition == null) return;


        int calculatedDamage = getDamage() ;

        Vector2D origin = owner.getPosition().copy();
        Vector2D dir = targetPosition.subtract(origin);
        if (dir.length() == 0) dir = new Vector2D(1, 0);
        Vector2D normalizedDir = dir.normalize();

        double baseAngle = Math.atan2(normalizedDir.getY(), normalizedDir.getX());

        Color chosenColor = RAY_COLORS[random.nextInt(RAY_COLORS.length)];
        double startAngle = baseAngle - Math.toRadians(RAY_SPREAD / 2.0);
        double angleStep = RAY_COUNT > 1 ? Math.toRadians(RAY_SPREAD) / (RAY_COUNT - 1) : 0;
        for (int i = 0; i < RAY_COUNT; i++) {
            double currentAngle = startAngle + (i * angleStep);

            Vector2D rayEnd = new Vector2D(
                    origin.getX() + Math.cos(currentAngle) * RAY_LENGTH,
                    origin.getY() + Math.sin(currentAngle) * RAY_LENGTH
            );

            world.addRainbowRay(origin, rayEnd, 10.0, 0.20, chosenColor);
        }

        // Tất cả quái trong vùng chùm tia quét qua đều chịu sát thương tức thì
        world.damageEnemiesInArc(origin, baseAngle, RAY_LENGTH, Math.toRadians(RAY_SPREAD), calculatedDamage);

        // 3Phóng thêm đạn chính để bồi thêm sát thương đơn mục tiêu
        Bullet rainbowBullet = new Bullet(
                origin.copy(),
                normalizedDir.scale(bulletSpeed),
                calculatedDamage,
                14.0,
                owner,
                chosenColor,
                true
        );
        world.addBullet(rainbowBullet);
    }
}