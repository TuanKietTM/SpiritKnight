package com.soulknight.buff.effect;

import com.soulknight.engine.Camera;
import com.soulknight.entity.Player;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Hieu ung tang Damage theo phong cach pixel art.
 *
 * - Tao cac manh nang luong do/cam sat than Player.
 * - Particle bay len va lech ra ngoai.
 * - Chia 2 lop renderBehind/renderFront de tao cam giac 2.5D.
 * - Khong ve vong sang duoi chan, khong glow lon.
 */
public final class DamageAuraEffect implements BuffVisualEffect {

    private static final double SPAWN_INTERVAL = 0.075;
    private static final double MIN_LIFE = 0.22;
    private static final double MAX_LIFE = 0.42;
    private static final double MIN_SPEED_Y = 18.0;
    private static final double MAX_SPEED_Y = 34.0;
    private static final double MAX_SIDE_SPEED = 14.0;
    private static final double SPAWN_RADIUS_X = 10.0;
    private static final double SPAWN_RADIUS_Y = 9.0;
    private static final double BASE_PIXEL_SIZE = 2.0;

    /*
     * Mau particle pixel co dinh.
     * Moi cap {x, y} la 1 o pixel trong pattern.
     */
    private static final int[][][] ENERGY_PATTERNS = {
            {{0, 0},{0, 1}, {1, 1}, {0, 2}},
            {{0, 0}, {1, 0}, {0, 1}, {-1, 1}, {0, 2}},
            {{0, 0}, {1, 0}, {1, 1}, {0, 1}, {0, 2}},
            {{0, 0},{-1, 1}, {0, 1}, {1, 1}, {0, 2}}
    };

    private final List<EnergyParticle> particles = new ArrayList<>();
    private final Random random = new Random();

    private boolean active;
    private double spawnTimer;

    @Override
    public void start(Player player) {
        active = player != null;
        particles.clear();
        spawnTimer = 0.0;
    }

    @Override
    public void update(Player player, double deltaSeconds, double remainingSeconds, double durationSeconds) {
        if (!active || player == null || player.getPosition() == null || deltaSeconds <= 0.0) {
            return;
        }
        updateParticles(deltaSeconds);
        spawnTimer -= deltaSeconds;
        if (spawnTimer > 0.0) {
            return;
        }
        spawnTimer = SPAWN_INTERVAL;
        spawnParticle(player);
        if (random.nextDouble() < 0.30) {
            spawnParticle(player);
        }
    }

    private void spawnParticle(Player player) {
        double centerX = player.getPosition().getX();
        double centerY = player.getPosition().getY();
        double offsetX = randomRange(-SPAWN_RADIUS_X, SPAWN_RADIUS_X);
        double offsetY = randomRange(-SPAWN_RADIUS_Y, SPAWN_RADIUS_Y);
        double x = centerX + offsetX;
        double y = centerY + offsetY;

        double velocityY = -randomRange(MIN_SPEED_Y, MAX_SPEED_Y);

        double outwardDirection = offsetX == 0.0 ? randomRange(-1.0, 1.0) : Math.signum(offsetX);

        double velocityX = outwardDirection * randomRange(3.0, MAX_SIDE_SPEED);

        double life = randomRange(MIN_LIFE, MAX_LIFE);

        int patternIndex = random.nextInt(ENERGY_PATTERNS.length);

        /*
         * Dua vao offsetY de xac dinh lop 2.5D:
         * y < 0 -> phia sau Player.
         * y >= 0 -> phia truoc Player.
         */
        boolean behind = offsetY < 0.0;
        if (random.nextDouble() < 0.75) {
            behind = true;
        }


        boolean orangeVariant = random.nextDouble() < 0.40;

        particles.add(new EnergyParticle(x, y, velocityX, velocityY, life, patternIndex, behind, orangeVariant));
    }

    private void updateParticles(double deltaSeconds) {
        Iterator<EnergyParticle> iterator = particles.iterator();

        while (iterator.hasNext()) {
            EnergyParticle particle = iterator.next();

            particle.life -= deltaSeconds;

            if (particle.life <= 0.0) {
                iterator.remove();
                continue;
            }

            particle.x += particle.velocityX * deltaSeconds;
            particle.y += particle.velocityY * deltaSeconds;

            double damping = Math.max(0.0, 1.0 - 2.4 * deltaSeconds);
            particle.velocityX *= damping;
            particle.velocityY *= damping;
        }
    }

    @Override
    public void renderBehind(GraphicsContext graphicsContext, Camera camera, Player player,
                             double remainingSeconds, double durationSeconds) {
        renderLayer(graphicsContext, camera, true);
    }

    @Override
    public void renderFront(GraphicsContext graphicsContext, Camera camera, Player player,
                            double remainingSeconds, double durationSeconds) {
        renderLayer(graphicsContext, camera, false);
    }

    private void renderLayer(GraphicsContext graphicsContext, Camera camera, boolean behindLayer) {
        if (!active || graphicsContext == null || camera == null || particles.isEmpty()) {
            return;
        }

        graphicsContext.save();
        graphicsContext.setImageSmoothing(false);

        for (EnergyParticle particle : particles) {
            if (particle.behind != behindLayer) {
                continue;
            }

            renderParticle(graphicsContext, camera, particle);
        }

        graphicsContext.restore();
    }

    private void renderParticle(GraphicsContext graphicsContext, Camera camera, EnergyParticle particle) {
        double zoom = camera.getZoom();

        double screenX = camera.worldToScreenX(particle.x);

        double screenY = camera.worldToScreenY(particle.y);

        double lifeProgress = Math.max(0.0, Math.min(1.0, particle.life / particle.maxLife));

        graphicsContext.setGlobalAlpha(0.15 + lifeProgress * 0.85);

        graphicsContext.setFill(particle.orangeVariant ? Color.rgb(255, 145, 45) : Color.rgb(255, 65, 65));

        double pixelSize = Math.max(1.0, Math.round(BASE_PIXEL_SIZE * zoom));

        int[][] pattern = ENERGY_PATTERNS[particle.patternIndex];


        double scale = lifeProgress < 0.35 ? 0.75 : 1.0;
        double renderPixel = Math.max(1.0, Math.round(pixelSize * scale));

        for (int[] point : pattern) {
            double drawX = screenX + point[0] * renderPixel;
            double drawY = screenY + point[1] * renderPixel;

            drawX = Math.round(drawX);
            drawY = Math.round(drawY);

            graphicsContext.fillRect(drawX, drawY, renderPixel, renderPixel);
        }
    }

    @Override
    public void notifyPlayerHit() {
        // Damage aura khong can xu ly khi Player bi danh
    }

    @Override
    public void stop() {
        active = false;
        particles.clear();
        spawnTimer = 0.0;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    private double randomRange(double min, double max) {
        return min + random.nextDouble() * (max - min);
    }

    private static final class EnergyParticle {

        private double x;
        private double y;

        private double velocityX;
        private double velocityY;

        private double life;
        private final double maxLife;

        private final int patternIndex;
        private final boolean behind;
        private final boolean orangeVariant;

        private EnergyParticle(double x, double y, double velocityX, double velocityY, double life,
                               int patternIndex, boolean behind, boolean orangeVariant) {
            this.x = x;
            this.y = y;

            this.velocityX = velocityX;
            this.velocityY = velocityY;

            this.life = life;
            this.maxLife = life;

            this.patternIndex = patternIndex;
            this.behind = behind;
            this.orangeVariant = orangeVariant;
        }
    }
}