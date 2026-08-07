package com.soulknight.buff.effect.combat;

import com.soulknight.engine.Camera;
import com.soulknight.utils.Vector2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.BlendMode;
import javafx.scene.paint.Color;

import java.util.Random;

/**
 * Vu no tu than , bung no manh theo phong cach pixel art.
 *
 * Bao gom:
 * - Flash vang trang cuc ngan + lop flash do cam toan man hinh.
 * - 3 shockwave lan rong voi toc do khac nhau.
 * - Loi lua 4 lop do -> cam -> vang -> trang nong.
 * - Cum lua pixel ban tung ra ngoai.
 * - Ember do/cam bay xa va tan dan.
 * - Manh vo toi mau vang ra sau vu no.
 *
 * Day chi la visual effect. Damage radius van do GameWorld quyet dinh.
 */
public final class DeathFireExplosionEffect implements CombatEffect {

    private static final double DURATION = 0.62;
    private static final double WHITE_FLASH_DURATION = 0.035;
    private static final double RED_FLASH_DURATION = 0.13;
    private static final double MAX_SHOCKWAVE_RADIUS = 300.0;
    private static final int FIRE_COUNT = 64;
    private static final int EMBER_COUNT = 34;
    private static final int DEBRIS_COUNT = 18;
    private final Vector2D center;
    private final FireParticle[] fireParticles = new FireParticle[FIRE_COUNT];
    private final Ember[] embers = new Ember[EMBER_COUNT];
    private final Debris[] debris = new Debris[DEBRIS_COUNT];
    private double elapsed;
    private double remaining = DURATION;
    public DeathFireExplosionEffect(Vector2D center) {
        this.center = center.copy();
        Random random = new Random();
        createFireParticles(random);
        createEmbers(random);
        createDebris(random);
    }

    private void createFireParticles(Random random) {
        for (int i = 0; i < fireParticles.length; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            // Hai lop toc do de vu no co tam day va tia lua bay xa.
            double speed;
            if (i < FIRE_COUNT * 0.65) {
                speed = 85.0 + random.nextDouble() * 145.0;
            } else {
                speed = 190.0 + random.nextDouble() * 180.0;
            }
            double sideSpread = randomRange(random, -20.0, 20.0);
            double velocityX = Math.cos(angle) * speed
                    + Math.cos(angle + Math.PI / 2.0) * sideSpread;
            double velocityY = Math.sin(angle) * speed
                    + Math.sin(angle + Math.PI / 2.0) * sideSpread;
            double size = 2.0 + random.nextInt(4);
            int colorVariant = random.nextInt(4);

            fireParticles[i] = new FireParticle(velocityX, velocityY, size, colorVariant);
        }
    }

    private void createEmbers(Random random) {
        for (int i = 0; i < embers.length; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double speed = 160.0 + random.nextDouble() * 260.0;

            embers[i] = new Ember(
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed,
                    1.0 + random.nextInt(2),
                    random.nextBoolean()
            );
        }
    }

    private void createDebris(Random random) {
        for (int i = 0; i < debris.length; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double speed = 70.0 + random.nextDouble() * 150.0;

            debris[i] = new Debris(
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed,
                    3.0 + random.nextInt(4)
            );
        }
    }

    @Override
    public void update(double deltaSeconds) {
        if (deltaSeconds <= 0.0 || isFinished()) {
            return;
        }

        elapsed += deltaSeconds;
        remaining = Math.max(0.0, remaining - deltaSeconds);
        updateFireParticles(deltaSeconds);
        updateEmbers(deltaSeconds);
        updateDebris(deltaSeconds);
    }

    private void updateFireParticles(double deltaSeconds) {
        for (FireParticle particle : fireParticles) {
            particle.x += particle.velocityX * deltaSeconds;
            particle.y += particle.velocityY * deltaSeconds;
            // Lua bi hut cham dan sau cu bung dau tien.
            double damping = Math.max(0.0, 1.0 - 3.3 * deltaSeconds);
            particle.velocityX *= damping;
            particle.velocityY *= damping;
            // Lua hoi boc len khi da bung ra.
            particle.velocityY -= 18.0 * deltaSeconds;
        }
    }
    private void updateEmbers(double deltaSeconds) {
        for (Ember ember : embers) {
            ember.x += ember.velocityX * deltaSeconds;
            ember.y += ember.velocityY * deltaSeconds;

            double damping = Math.max(0.0, 1.0 - 1.8 * deltaSeconds);

            ember.velocityX *= damping;
            ember.velocityY *= damping;

            // Tan lua sau do bat dau bay len nhe.
            ember.velocityY -= 28.0 * deltaSeconds;
        }
    }

    private void updateDebris(double deltaSeconds) {
        for (Debris piece : debris) {
            piece.x += piece.velocityX * deltaSeconds;
            piece.y += piece.velocityY * deltaSeconds;
            piece.velocityY += 90.0 * deltaSeconds;
            double damping = Math.max(0.0, 1.0 - 2.0 * deltaSeconds);
            piece.velocityX *= damping;
        }
    }

    @Override
    public void render(GraphicsContext gc, Camera camera) {
        if (gc == null || camera == null || isFinished()) {
            return;
        }
        double zoom = camera.getZoom();
        double screenX = camera.worldToScreenX(center.getX());
        double screenY = camera.worldToScreenY(center.getY());
        double progress = Math.min(1.0, elapsed / DURATION);
        double life = 1.0 - progress;
        gc.save();
        gc.setImageSmoothing(false);
        renderScreenFlash(gc);
        renderShockwaves(gc, screenX, screenY, zoom, progress);
        renderFireBloom(gc, screenX, screenY, zoom, progress);
        renderFireParticles(gc, camera, zoom, life);
        renderEmbers(gc, camera, zoom, life);
        renderDebris(gc, camera, zoom, life);
        gc.restore();
    }

    /**
     * Hai lop flash: trang nong rat ngan, sau do do/cam tan nhanh.
     */
    private void renderScreenFlash(GraphicsContext gc) {
        double canvasWidth = gc.getCanvas().getWidth();
        double canvasHeight = gc.getCanvas().getHeight();

        if (elapsed <= RED_FLASH_DURATION) {
            double progress = elapsed / RED_FLASH_DURATION;
            double alpha = (1.0 - progress) * 0.42;
            gc.save();
            gc.setGlobalBlendMode(BlendMode.ADD);
            gc.setGlobalAlpha(alpha);
            gc.setFill(Color.rgb(255, 45, 10));
            gc.fillRect(0, 0, canvasWidth, canvasHeight);
            gc.restore();
        }

        if (elapsed <= WHITE_FLASH_DURATION) {
            double progress = elapsed / WHITE_FLASH_DURATION;
            double alpha = (1.0 - progress) * 0.72;
            gc.save();
            gc.setGlobalBlendMode(BlendMode.ADD);
            gc.setGlobalAlpha(alpha);
            gc.setFill(Color.rgb(255, 245, 190));
            gc.fillRect(0, 0, canvasWidth, canvasHeight);
            gc.restore();
        }
    }

    /**
     * 3 shockwave bung lan, trong do lop dau dam va nong nhat.
     */
    private void renderShockwaves(GraphicsContext gc, double screenX, double screenY, double zoom, double progress) {
        double eased = 1.0 - Math.pow(1.0 - progress, 3.2);
        renderShockwaveLayer(gc, screenX, screenY,
                MAX_SHOCKWAVE_RADIUS * eased * zoom,
                Math.max(0.0, (1.0 - progress) * 0.95),
                Math.max(2.0, 8.0 * zoom * (1.0 - progress)),
                Color.rgb(255, 30, 10)
        );

        double middleProgress = Math.max(0.0, progress - 0.035);
        double middleEased = 1.0 - Math.pow(1.0 - middleProgress, 3.0);

        renderShockwaveLayer(gc, screenX, screenY,
                MAX_SHOCKWAVE_RADIUS * 0.78 * middleEased * zoom,
                Math.max(0.0, (1.0 - progress) * 0.75),
                Math.max(2.0, 5.0 * zoom),
                Color.rgb(255, 105, 15)
        );

        double innerProgress = Math.max(0.0, progress - 0.07);
        double innerEased = 1.0 - Math.pow(1.0 - innerProgress, 2.8);

        renderShockwaveLayer(gc, screenX, screenY,
                MAX_SHOCKWAVE_RADIUS * 0.52 * innerEased * zoom,
                Math.max(0.0, (1.0 - progress) * 0.65),
                Math.max(1.0, 3.0 * zoom),
                Color.rgb(255, 215, 70)
        );
    }

    private void renderShockwaveLayer(GraphicsContext gc, double centerX, double centerY, double radius,
                                      double alpha, double lineWidth, Color color) {
        if (radius <= 0.0 || alpha <= 0.0) {
            return;
        }
        gc.save();
        gc.setGlobalBlendMode(BlendMode.ADD);
        gc.setGlobalAlpha(alpha);
        gc.setStroke(color);
        gc.setLineWidth(lineWidth);
        gc.strokeOval(centerX - radius, centerY - radius, radius * 2.0, radius * 2.0);
        gc.restore();
    }

    /**
     * Loi no 4 lop + cac canh lua pixel bung quanh tam.
     */
    private void renderFireBloom(GraphicsContext gc, double screenX, double screenY, double zoom, double progress) {
        if (progress > 0.58) {
            return;
        }
        double coreProgress = progress / 0.58;
        double burst = 1.0 - Math.pow(1.0 - coreProgress, 2.6);
        double alpha = 1.0 - coreProgress;
        // Loi bung nhanh len hon 100 world pixel visual.
        double size = (18.0 + burst * 102.0) * zoom;
        gc.save();
        gc.setGlobalBlendMode(BlendMode.ADD);
        gc.setGlobalAlpha(alpha * 0.92);
        gc.setFill(Color.rgb(150, 5, 5));
        drawPixelSquare(gc, screenX, screenY, size);
        gc.setGlobalAlpha(alpha);
        gc.setFill(Color.rgb(255, 20, 5));
        drawPixelSquare(gc, screenX, screenY, size * 0.82);
        gc.setFill(Color.rgb(255, 100, 10));
        drawPixelSquare(gc, screenX, screenY, size * 0.61);
        gc.setFill(Color.rgb(255, 215, 60));
        drawPixelSquare(gc, screenX, screenY, size * 0.39);
        gc.setFill(Color.rgb(255, 250, 205));
        drawPixelSquare(gc, screenX, screenY, size * 0.20);
        renderFlamePetals(gc, screenX, screenY, zoom, burst, alpha);
        gc.restore();
    }

    /**
     * Cac mui lua pixel toa ra nhu bong lua bi xe tung.
     */
    private void renderFlamePetals(GraphicsContext gc, double centerX, double centerY,
                                   double zoom, double burst, double alpha) {
        final int petals = 16;
        for (int i = 0; i < petals; i++) {
            double angle = i * (Math.PI * 2.0 / petals);
            // Xen ke do dai de no khong thanh mot vong tron qua deu.
            double lengthFactor = (i % 3 == 0) ? 1.25 : (i % 2 == 0 ? 1.0 : 0.82);
            double distance = (22.0 + 72.0 * burst * lengthFactor) * zoom;
            double x = centerX + Math.cos(angle) * distance;
            double y = centerY + Math.sin(angle) * distance;
            double pixel = Math.max(2.0, Math.round((4.0 + 5.0 * (1.0 - burst)) * zoom));
            gc.setGlobalAlpha(alpha * 0.9);
            gc.setFill(i % 3 == 0 ? Color.rgb(255, 225, 70)
                    : Color.rgb(255, 55, 10));

            drawPixelCross(gc, x, y, pixel);
        }
    }

    private void renderFireParticles(GraphicsContext gc, Camera camera,
                                     double zoom, double life) {
        gc.save();
        gc.setGlobalBlendMode(BlendMode.ADD);
        gc.setGlobalAlpha(Math.max(0.0, life));

        for (FireParticle particle : fireParticles) {
            double x = camera.worldToScreenX(center.getX() + particle.x);
            double y = camera.worldToScreenY(center.getY() + particle.y);

            switch (particle.colorVariant) {
                case 0 -> gc.setFill(Color.rgb(180, 8, 8));
                case 1 -> gc.setFill(Color.rgb(255, 25, 5));
                case 2 -> gc.setFill(Color.rgb(255, 95, 10));
                default -> gc.setFill(Color.rgb(255, 215, 55));
            }

            double size = Math.max(2.0, Math.round(particle.size * zoom));

            // Mot cum 2-3 pixel tao cam giac manh lua chu khong phai mot diem don.
            gc.fillRect(Math.round(x), Math.round(y), size, size);

            if (particle.colorVariant >= 2) {
                gc.fillRect(Math.round(x - size), Math.round(y + size * 0.5), size, size);
            }
        }

        gc.restore();
    }

    private void renderEmbers(GraphicsContext gc, Camera camera, double zoom, double life) {
        gc.save();
        gc.setGlobalBlendMode(BlendMode.ADD);
        gc.setGlobalAlpha(Math.max(0.0, life * 0.95));

        for (Ember ember : embers) {
            double x = camera.worldToScreenX(center.getX() + ember.x);
            double y = camera.worldToScreenY(center.getY() + ember.y);

            gc.setFill(ember.hot ? Color.rgb(255, 225, 90) : Color.rgb(255, 35, 10));
            double size = Math.max(1.0, Math.round(ember.size * zoom));

            gc.fillRect(Math.round(x), Math.round(y), size, size);
        }

        gc.restore();
    }

    private void renderDebris(GraphicsContext gc, Camera camera, double zoom, double life) {
        gc.save();
        gc.setGlobalAlpha(Math.max(0.0, life * 0.78));
        gc.setFill(Color.rgb(70, 15, 12));

        for (Debris piece : debris) {
            double x = camera.worldToScreenX(center.getX() + piece.x);
            double y = camera.worldToScreenY(center.getY() + piece.y);

            double size = Math.max(2.0, Math.round(piece.size * zoom));

            gc.fillRect(Math.round(x), Math.round(y), size, size);
        }

        gc.restore();
    }

    private void drawPixelCross(GraphicsContext gc, double centerX, double centerY, double pixel) {
        double x = Math.round(centerX);
        double y = Math.round(centerY);
        double p = Math.max(1.0, Math.round(pixel));

        gc.fillRect(x - p / 2.0, y - p / 2.0, p, p);
        gc.fillRect(x - p * 1.5, y - p / 2.0, p, p);
        gc.fillRect(x + p * 0.5, y - p / 2.0, p, p);
        gc.fillRect(x - p / 2.0, y - p * 1.5, p, p);
        gc.fillRect(x - p / 2.0, y + p * 0.5, p, p);
    }

    private void drawPixelSquare(GraphicsContext gc, double centerX, double centerY, double size) {
        double roundedSize = Math.max(1.0, Math.round(size));

        gc.fillRect(Math.round(centerX - roundedSize / 2.0),
                Math.round(centerY - roundedSize / 2.0),
                roundedSize, roundedSize);
    }

    private double randomRange(Random random, double min, double max) {
        return min + random.nextDouble() * (max - min);
    }

    @Override
    public boolean isFinished() {
        return remaining <= 0.0;
    }

    private static final class FireParticle {
        private double x;
        private double y;

        private double velocityX;
        private double velocityY;

        private final double size;
        private final int colorVariant;

        private FireParticle(double velocityX, double velocityY, double size, int colorVariant) {
            this.velocityX = velocityX;
            this.velocityY = velocityY;
            this.size = size;
            this.colorVariant = colorVariant;
        }
    }

    private static final class Ember {
        private double x;
        private double y;

        private double velocityX;
        private double velocityY;

        private final double size;
        private final boolean hot;

        private Ember(double velocityX, double velocityY, double size, boolean hot) {
            this.velocityX = velocityX;
            this.velocityY = velocityY;
            this.size = size;
            this.hot = hot;
        }
    }

    private static final class Debris {
        private double x;
        private double y;

        private double velocityX;
        private double velocityY;

        private final double size;

        private Debris(double velocityX, double velocityY, double size) {
            this.velocityX = velocityX;
            this.velocityY = velocityY;
            this.size = size;
        }
    }
}