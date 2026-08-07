package com.soulknight.buff.effect;

import com.soulknight.engine.Camera;
import com.soulknight.entity.Player;
import com.soulknight.utils.Vector2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Hieu ung Speed Buff theo phong cach pixel art.
 * Dung cac mau vet gio hinh hoc co dinh va chi render phia sau Player.
 */
public final class SpeedWindEffect implements BuffVisualEffect {

    private static final double SPAWN_INTERVAL = 0.08;
    private static final double MIN_LIFE = 0.18;
    private static final double MAX_LIFE = 0.30;
    private static final double MIN_SPEED = 58.0;
    private static final double MAX_SPEED = 88.0;
    private static final double BACK_OFFSET_MIN = 5.0;
    private static final double BACK_OFFSET_MAX = 13.0;
    private static final double SIDE_OFFSET = 8.0;
    private static final double BASE_PIXEL_SIZE = 2.0;

    private static final int[][][] WIND_PATTERNS = {
            {{0, 0}, {1, 0}, {2, 0}, {3, 0}},
            {{0, 0}, {1, 0}, {2, 0}, {2, 1}, {3, 1}},
            {{0, 0}, {1, 0}, {1, 1}, {2, 1}, {3, 1}},
            {{0, 0}, {1, 0}, {2, 0}, {3, 0}, {1, -1}}
    };

    private final List<WindParticle> particles = new ArrayList<>();
    private final Random random = new Random();

    private boolean active;
    private double spawnTimer;
    private double previousPlayerX;
    private double previousPlayerY;
    private boolean hasPreviousPosition;
    private double moveDirX;
    private double moveDirY;

    @Override
    public void start(Player player) {
        active = player != null;
        particles.clear();
        spawnTimer = 0.0;
        moveDirX = 0.0;
        moveDirY = 0.0;
        hasPreviousPosition = false;

        if (player != null && player.getPosition() != null) {
            previousPlayerX = player.getPosition().getX();
            previousPlayerY = player.getPosition().getY();
            hasPreviousPosition = true;
        }
    }

    @Override
    public void update(Player player, double deltaSeconds, double remainingSeconds, double durationSeconds) {
        if (!active || player == null || player.getPosition() == null || deltaSeconds <= 0.0) {
            return;
        }

        updateMovementDirection(player);
        updateParticles(deltaSeconds);

        if (Math.hypot(moveDirX, moveDirY) < 0.05) {
            return;
        }

        spawnTimer -= deltaSeconds;
        if (spawnTimer > 0.0) {
            return;
        }

        spawnTimer = SPAWN_INTERVAL;
        spawnWindParticle(player);

        if (random.nextDouble() < 0.25) {
            spawnWindParticle(player);
        }
    }

    private void updateMovementDirection(Player player) {
        double currentX = player.getPosition().getX();
        double currentY = player.getPosition().getY();

        if (!hasPreviousPosition) {
            previousPlayerX = currentX;
            previousPlayerY = currentY;
            hasPreviousPosition = true;
            return;
        }

        double dx = currentX - previousPlayerX;
        double dy = currentY - previousPlayerY;
        previousPlayerX = currentX;
        previousPlayerY = currentY;

        double length = Math.hypot(dx, dy);
        if (length <= 0.001) {
            moveDirX = 0.0;
            moveDirY = 0.0;
            return;
        }

        moveDirX = dx / length;
        moveDirY = dy / length;
    }

    private void spawnWindParticle(Player player) {
        Vector2D position = player.getPosition();
        if (position == null) {
            return;
        }

        double perpendicularX = -moveDirY;
        double perpendicularY = moveDirX;
        double sideOffset = randomRange(-SIDE_OFFSET, SIDE_OFFSET);
        double backOffset = randomRange(BACK_OFFSET_MIN, BACK_OFFSET_MAX);

        double x = position.getX() - moveDirX * backOffset + perpendicularX * sideOffset;
        double y = position.getY() - moveDirY * backOffset + perpendicularY * sideOffset;

        double speed = randomRange(MIN_SPEED, MAX_SPEED);
        double velocityX = -moveDirX * speed;
        double velocityY = -moveDirY * speed;

        double spread = randomRange(-10.0, 10.0);
        velocityX += perpendicularX * spread;
        velocityY += perpendicularY * spread;

        double life = randomRange(MIN_LIFE, MAX_LIFE);
        int patternIndex = random.nextInt(WIND_PATTERNS.length);

        particles.add(new WindParticle(x, y, velocityX, velocityY, life, patternIndex));
    }

    private void updateParticles(double deltaSeconds) {
        Iterator<WindParticle> iterator = particles.iterator();

        while (iterator.hasNext()) {
            WindParticle particle = iterator.next();
            particle.life -= deltaSeconds;

            if (particle.life <= 0.0) {
                iterator.remove();
                continue;
            }

            particle.x += particle.velocityX * deltaSeconds;
            particle.y += particle.velocityY * deltaSeconds;

            double damping = Math.max(0.0, 1.0 - 3.2 * deltaSeconds);
            particle.velocityX *= damping;
            particle.velocityY *= damping;
        }
    }

    @Override
    public void renderBehind(GraphicsContext gc, Camera camera, Player player,
                             double remainingSeconds, double durationSeconds) {
        if (!active || gc == null || camera == null || particles.isEmpty()) {
            return;
        }

        gc.save();
        gc.setImageSmoothing(false);

        for (WindParticle particle : particles) {
            renderParticle(gc, camera, particle);
        }

        gc.restore();
    }

    @Override
    public void renderFront(GraphicsContext gc, Camera camera, Player player,
                            double remainingSeconds, double durationSeconds) {
        // Speed effect chi ve phia sau Player.
    }

    private void renderParticle(GraphicsContext gc, Camera camera, WindParticle particle) {
        double zoom = camera.getZoom();
        double screenX = camera.worldToScreenX(particle.x);
        double screenY = camera.worldToScreenY(particle.y);

        double velocityLength = Math.hypot(particle.velocityX, particle.velocityY);
        if (velocityLength <= 0.001) {
            return;
        }

        double dirX = particle.velocityX / velocityLength;
        double dirY = particle.velocityY / velocityLength;
        double perpendicularX = -dirY;
        double perpendicularY = dirX;

        double lifeProgress = Math.max(0.0, Math.min(1.0, particle.life / particle.maxLife));
        gc.setGlobalAlpha(0.18 + lifeProgress * 0.82);
        gc.setFill(Color.rgb(175, 235, 255));

        double pixelSize = Math.max(1.0, Math.round(BASE_PIXEL_SIZE * zoom));
        int[][] pattern = WIND_PATTERNS[particle.patternIndex];

        for (int[] point : pattern) {
            double localX = point[0] * pixelSize;
            double localY = point[1] * pixelSize;

            double drawX = screenX + dirX * localX + perpendicularX * localY;
            double drawY = screenY + dirY * localX + perpendicularY * localY;

            drawX = Math.round(drawX);
            drawY = Math.round(drawY);

            gc.fillRect(drawX, drawY, pixelSize, pixelSize);
        }
    }

    @Override
    public void notifyPlayerHit() {
    }

    @Override
    public void stop() {
        active = false;
        particles.clear();
        hasPreviousPosition = false;
        moveDirX = 0.0;
        moveDirY = 0.0;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    private double randomRange(double min, double max) {
        return min + random.nextDouble() * (max - min);
    }

    private static final class WindParticle {
        private double x;
        private double y;
        private double velocityX;
        private double velocityY;
        private double life;
        private final double maxLife;
        private final int patternIndex;

        private WindParticle(double x, double y, double velocityX, double velocityY,
                             double life, int patternIndex) {
            this.x = x;
            this.y = y;
            this.velocityX = velocityX;
            this.velocityY = velocityY;
            this.life = life;
            this.maxLife = life;
            this.patternIndex = patternIndex;
        }
    }
}