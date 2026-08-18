package com.soulknight.animation;

import com.soulknight.engine.Camera;
import com.soulknight.utils.Vector2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Quan li cac hieu ung hat trong game.
 */
public class ParticleManager {

    private static final int MAX_PARTICLES = 600;

    private static final Color[] WOOD_COLORS = {
            Color.rgb(139, 69, 19),
            Color.rgb(160, 82, 45),
            Color.rgb(205, 133, 63),
            Color.rgb(222, 184, 135)
    };

    private static final Color[] FIRE_COLORS = {
            Color.ORANGERED,
            Color.ORANGE,
            Color.GOLD
    };

    private static final Color[] HEAL_COLORS = {
            Color.LIMEGREEN,
            Color.LIGHTGREEN,
            Color.AQUAMARINE
    };

    private final List<Particle> particles = new ArrayList<>();
    private final Random random = new Random();

    public void spawnHitImpact(Vector2D hitPosition) {
        int count = 5 + random.nextInt(3);

        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double speed = randomRange(80.0, 150.0);
            double size = randomRange(2.0, 5.0);
            double life = randomRange(0.08, 0.14);

            Particle particle = new Particle(
                    hitPosition,
                    velocityFromAngle(angle, speed),
                    size,
                    random.nextBoolean() ? Color.ORANGE : Color.GOLD,
                    life
            ).withEndSize(0.0)
                    .withDrag(7.0)
                    .withShape(Particle.Shape.DIAMOND)
                    .withRotation(randomRange(0.0, 45.0), randomRange(-360.0, 360.0));

            addParticle(particle);
        }
    }

    public void spawnWoodDebris(Vector2D centerPosition) {
        int count = 12 + random.nextInt(6);

        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double speed = randomRange(80.0, 180.0);
            double size = randomRange(4.0, 8.0);
            double life = randomRange(0.35, 0.65);

            Particle particle = new Particle(
                    centerPosition,
                    velocityFromAngle(angle, speed),
                    new Vector2D(0.0, 180.0),
                    size,
                    randomRange(1.0, 3.0),
                    randomColor(WOOD_COLORS),
                    life,
                    Particle.Shape.SQUARE
            ).withDrag(1.2)
                    .withRotation(randomRange(0.0, 90.0), randomRange(-300.0, 300.0));

            addParticle(particle);
        }
    }

    public void spawnBloodSplatter(Vector2D position) {
        int count = 7 + random.nextInt(5);

        for (int i = 0; i < count; i++) {
            double angle = randomRange(-Math.PI, 0.0);
            double speed = randomRange(45.0, 100.0);
            double size = randomRange(3.0, 6.0);
            double life = randomRange(0.25, 0.45);

            Particle particle = new Particle(
                    position,
                    velocityFromAngle(angle, speed),
                    new Vector2D(0.0, 140.0),
                    size,
                    randomRange(1.0, 3.0),
                    random.nextBoolean() ? Color.RED : Color.DARKRED,
                    life,
                    Particle.Shape.CIRCLE
            ).withDrag(0.8);

            addParticle(particle);
        }
    }

    /**
     * Vat can bi chem bang vu khi can chien.
     */
    public void spawnMeleeObstacleImpact(Vector2D hitPosition, double slashAngle) {
        int woodCount = 8 + random.nextInt(4);

        for (int i = 0; i < woodCount; i++) {
            double angle = slashAngle + randomRange(-0.4, 0.4);
            double speed = randomRange(120.0, 220.0);
            double size = randomRange(3.0, 6.0);
            double life = randomRange(0.18, 0.3);

            Particle particle = new Particle(
                    hitPosition,
                    velocityFromAngle(angle, speed),
                    new Vector2D(0.0, 120.0),
                    size,
                    1.0,
                    randomColor(WOOD_COLORS),
                    life,
                    Particle.Shape.SQUARE
            ).withDrag(2.0)
                    .withRotation(randomRange(0.0, 90.0), randomRange(-420.0, 420.0));

            addParticle(particle);
        }

        int sparkCount = 5 + random.nextInt(3);

        for (int i = 0; i < sparkCount; i++) {
            double angle = slashAngle + randomRange(-0.8, 0.8);
            double speed = randomRange(100.0, 190.0);
            double size = randomRange(2.0, 4.0);
            double life = randomRange(0.08, 0.14);

            Particle particle = new Particle(
                    hitPosition,
                    velocityFromAngle(angle, speed),
                    size,
                    random.nextBoolean() ? Color.WHITE : Color.GOLD,
                    life
            ).withEndSize(0.0)
                    .withDrag(8.0)
                    .withShape(Particle.Shape.DIAMOND)
                    .withRotation(45.0, randomRange(-500.0, 500.0));

            addParticle(particle);
        }
    }
    public void spawnCoinBurst(Vector2D position, int amount) {
        int count = Math.max(6, Math.min(18, amount / 2 + 5));

        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double speed = randomRange(55.0, 125.0);

            Particle particle = new Particle(
                    position,
                    velocityFromAngle(angle, speed),
                    new Vector2D(0.0, 80.0),
                    randomRange(3.0, 6.0),
                    1.0,
                    random.nextBoolean() ? Color.GOLD : Color.YELLOW,
                    randomRange(0.35, 0.65),
                    Particle.Shape.DIAMOND
            ).withDrag(1.5)
                    .withRotation(45.0, randomRange(-360.0, 360.0));

            addParticle(particle);
        }
    }

    public void update(double deltaSeconds) {
        if (deltaSeconds <= 0.0) return;

        for (Particle particle : particles) {
            particle.update(deltaSeconds);
        }

        particles.removeIf(Particle::isDead);
    }

    public void render(GraphicsContext gc, Camera camera) {
        if (gc == null || camera == null) return;

        for (Particle particle : particles) {
            particle.render(gc, camera);
        }
    }

    public void clear() {
        particles.clear();
    }

    public int getParticleCount() {
        return particles.size();
    }

    private void addParticle(Particle particle) {
        if (particle == null) return;

        while (particles.size() >= MAX_PARTICLES) {
            particles.remove(0);
        }

        particles.add(particle);
    }

    private Vector2D velocityFromAngle(double angle, double speed) {
        return new Vector2D(Math.cos(angle) * speed, Math.sin(angle) * speed);
    }

    private Vector2D offsetPosition(Vector2D position, double offsetX, double offsetY) {
        if (position == null) return new Vector2D(offsetX, offsetY);
        return new Vector2D(position.getX() + offsetX, position.getY() + offsetY);
    }

    private Color randomColor(Color[] colors) {
        if (colors == null || colors.length == 0) return Color.WHITE;
        return colors[random.nextInt(colors.length)];
    }

    private double randomRange(double min, double max) {
        if (max <= min) return min;
        return min + random.nextDouble() * (max - min);
    }
}