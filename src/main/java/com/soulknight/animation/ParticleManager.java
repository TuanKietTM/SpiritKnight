package com.soulknight.animation;

import com.soulknight.engine.Camera;
import com.soulknight.utils.SoundManager;
import com.soulknight.utils.Vector2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Quan li cac hieu ung hat trong game
 */
public class ParticleManager {
    private final List<Particle> particles = new ArrayList<>();
    private final Random random = new Random();
    final SoundManager sound = SoundManager.getInstance();
    private static final Color[] WOOD_COLORS = new Color[]{
            Color.rgb(139, 69, 19),
            Color.rgb(160, 82, 45),
            Color.rgb(205, 133, 63),
            Color.rgb(222, 184, 135)
    };

    public void spawnHitImpact(Vector2D hitPosition) {
       sound.playSFX("attack_box");
        int count = 4 + random.nextInt(3);

        for (int i = 0; i < count; i++) {
            double vx = (random.nextDouble() - 0.5) * 120.0;
            double vy = (random.nextDouble() - 0.5) * 120.0;
            double size = 3.0 + random.nextDouble() * 2.0;
            Color color = random.nextBoolean() ? Color.ORANGE : Color.GOLD;
            double life = 0.08 + random.nextDouble() * 0.04;

            particles.add(new Particle(hitPosition, new Vector2D(vx, vy), size, color, life));
        }
    }
    public void spawnWoodDebris(Vector2D centerPosition) {
        int count = 12 + random.nextInt(6);
        for (int i = 0; i < count; i++) {
            double speed = 80.0 + random.nextDouble() * 100.0;
            double angle = random.nextDouble() * Math.PI * 2;
            double vx = Math.cos(angle) * speed;
            double vy = Math.sin(angle) * speed;

            double size = 4.0 + random.nextDouble() * 4.0;
            Color woodColor = WOOD_COLORS[random.nextInt(WOOD_COLORS.length)];
            double life = 0.3 + random.nextDouble() * 0.3;

            particles.add(new Particle(centerPosition, new Vector2D(vx, vy), size, woodColor, life));
        }
    }
    public void spawnBloodSplatter(Vector2D position) {
        int count = 6 + random.nextInt(4);
        for (int i = 0; i < count; i++) {
            double vx = (random.nextDouble() - 0.5) * 80.0;
            double vy = (random.nextDouble() - 0.5) * 80.0;
            double size = 3.0 + random.nextDouble() * 3.0;
            double life = 0.2 + random.nextDouble() * 0.2;

            particles.add(new Particle(position, new Vector2D(vx, vy), size, Color.RED, life));
        }
    }
    /**
     * Vat can bi chem bang vu khi can chien
     */
    public void spawnMeleeObstacleImpact(Vector2D hitPosition, double slashAngle) {
        int woodCount = 8 + random.nextInt(4);
        for (int i = 0; i < woodCount; i++) {
            double spreadAngle = slashAngle + (random.nextDouble() - 0.5) * 0.8;
            double speed = 120.0 + random.nextDouble() * 100.0; // Bay nhanh hơn vỡ thông thường

            double vx = Math.cos(spreadAngle) * speed;
            double vy = Math.sin(spreadAngle) * speed;

            double size = 3.0 + random.nextDouble() * 3.0;
            Color woodColor = WOOD_COLORS[random.nextInt(WOOD_COLORS.length)];
            double life = 0.15 + random.nextDouble() * 0.1;

            particles.add(new Particle(hitPosition, new Vector2D(vx, vy), size, woodColor, life));
        }
        int sparkCount = 5 + random.nextInt(3);
        for (int i = 0; i < sparkCount; i++) {
            double vx = (random.nextDouble() - 0.5) * 160.0;
            double vy = (random.nextDouble() - 0.5) * 160.0;
            double size = 2.0 + random.nextDouble() * 2.0;
            Color color = random.nextBoolean() ? Color.WHITE : Color.GOLD;
            double life = 0.08 + random.nextDouble() * 0.04;

            particles.add(new Particle(hitPosition, new Vector2D(vx, vy), size, color, life));
        }
    }

    public void update(double deltaSeconds) {
        for (Particle p : particles) {
            p.update(deltaSeconds);
        }
        particles.removeIf(Particle::isDead);
    }

    public void render(GraphicsContext gc, Camera camera) {
        for (Particle p : particles) {
            p.render(gc, camera);
        }
    }

    public void clear() {
        particles.clear();
    }
}