package com.soulknight.animation;

import com.soulknight.engine.Camera;
import com.soulknight.entity.Player;
import com.soulknight.utils.SoundManager;
import com.soulknight.utils.Vector2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.Glow;
import javafx.scene.paint.Color;

import java.util.Random;

/**
 * Hieu ung khi Player chet.
 * Co the Player tan dan, linh hon la ban sao mo bay len va day nang luong dut ra.
 */
public final class PlayerDeathEffect {

    private static final int PARTICLE_COUNT = 32;
    private static final int SOUL_TRAIL_COUNT = 4;

    private final Vector2D position;
    private final double duration;

    private final double[] particleAngle = new double[PARTICLE_COUNT];
    private final double[] particleDistance = new double[PARTICLE_COUNT];
    private final double[] particleSize = new double[PARTICLE_COUNT];
    private final double[] particleDelay = new double[PARTICLE_COUNT];
    private final double[] particleSpin = new double[PARTICLE_COUNT];

    private final ColorAdjust soulColorAdjust = new ColorAdjust();
    private final Glow soulGlow = new Glow(0.42);

    private double timer;
    private boolean finished;

    public PlayerDeathEffect(Vector2D position, double duration) {
        this.position = position.copy();
        this.duration = Math.max(0.90, duration);

        Random random = new Random();

        for (int i = 0; i < PARTICLE_COUNT; i++) {
            particleAngle[i] = random.nextDouble() * Math.PI * 2.0;
            particleDistance[i] = 18.0 + random.nextDouble() * 42.0;
            particleSize[i] = 1.0 + random.nextInt(3);
            particleDelay[i] = random.nextDouble() * 0.22;
            particleSpin[i] = random.nextBoolean() ? 1.0 : -1.0;
        }

        // Lam linh hon gan trang hon, giu chat pixel art va khong bi nhoe qua manh
        soulColorAdjust.setSaturation(-0.88);
        soulColorAdjust.setBrightness(0.48);
        soulColorAdjust.setContrast(-0.18);
        soulGlow.setInput(soulColorAdjust);

        SoundManager.getInstance().playSFX("sound_die");
    }

    public void update(double deltaSeconds) {
        if (finished) return;

        timer += Math.max(0.0, deltaSeconds);

        if (timer >= duration) {
            timer = duration;
            finished = true;
        }
    }

    /**
     * Alpha cua co the Player that.
     */
    public double getEntityAlpha() {
        double progress = getProgress();

        if (progress < 0.14) {
            return 1.0;
        }

        return 1.0 - smoothStep(clamp01((progress - 0.14) / 0.68));
    }

    /**
     * Co the chi xep nhe, khong co ve thanh mot cham.
     */
    public double getEntityScaleX() {
        double progress = getProgress();

        if (progress < 0.18) {
            return 1.0 + Math.sin(progress / 0.18 * Math.PI) * 0.05;
        }

        return Math.max(
                0.82,
                1.0 - smoothStep(clamp01((progress - 0.18) / 0.68)) * 0.18
        );
    }

    public double getEntityScaleY() {
        double progress = getProgress();

        if (progress < 0.18) {
            return 1.0 - Math.sin(progress / 0.18 * Math.PI) * 0.08;
        }

        return Math.max(
                0.68,
                1.0 - smoothStep(clamp01((progress - 0.18) / 0.68)) * 0.32
        );
    }

    public double getShakeX() {
        double progress = getProgress();

        if (progress > 0.36) return 0.0;

        return Math.sin(timer * 105.0) * 2.0 * (1.0 - progress / 0.36);
    }

    public double getShakeY() {
        double progress = getProgress();

        if (progress > 0.36) return 0.0;

        return Math.cos(timer * 87.0) * 1.2 * (1.0 - progress / 0.36);
    }

    public boolean isFinished() {
        return finished;
    }

    /**
     * Ve linh hon la ban sao mo cua Player.
     * Goi sau khi ve co the Player that.
     */
    public void renderSoulCopy(GraphicsContext gc, Camera camera, Player player) {
        if (gc == null || camera == null || player == null || player.getPosition() == null) {
            return;
        }

        double progress = getProgress();

        // Giữ linh hon trong co the mot nhip ngan roi moi tach ra
        if (progress < 0.16 || progress >= 0.99) {
            return;
        }

        double local = clamp01((progress - 0.16) / 0.83);
        double zoom = camera.getZoom();

        double pivotX = camera.worldToScreenX(player.getPosition().getX());
        double pivotY = camera.worldToScreenY(player.getPosition().getY());

        // Bay cham luc dau, tang toc khi roi xa co the
        double rise = (4.0 + 105.0 * Math.pow(local, 1.85)) * zoom;
        double sway = Math.sin(timer * 5.2) * 8.0 * zoom * (1.0 - local * 0.25);
        double bob = Math.sin(timer * 10.0) * 1.8 * zoom;

        double soulX = pivotX + sway;
        double soulY = pivotY - rise + bob;

        // Nho dan va mo dan khi bay xa
        double soulScale = 1.0 - smoothStep(local) * 0.22;
        double appearAlpha = smoothStep(clamp01(local / 0.14));
        double disappearAlpha = 1.0 - smoothStep(clamp01((local - 0.72) / 0.28));
        double soulAlpha = appearAlpha * disappearAlpha * 0.52;

        // Day nang luong noi than xac va linh hon, dut dan khi bay cao
        double connectionAlpha =
                (1.0 - smoothStep(clamp01((local - 0.05) / 0.48))) * 0.45;

        renderSoulConnection(gc, pivotX, pivotY, soulX, soulY, connectionAlpha, zoom
        );

        renderSoulReleasePulse(gc, pivotX, pivotY, local, zoom);

        // Ve du anh tu xa den gan de linh hon khong trong nhu clone
        for (int i = SOUL_TRAIL_COUNT - 1; i >= 1; i--) {
            double delayedTimer = Math.max(0.0, timer - i * 0.055);

            double trailSway = Math.sin(delayedTimer * 5.2) * 8.0 * zoom * (1.0 - local * 0.25);

            trailSway += Math.sin(i * 2.7 + timer * 3.0) * i * 0.8 * zoom;

            double trailRise = Math.max(0.0, rise - i * 10.0 * zoom);
            double trailScale = soulScale * (1.0 - i * 0.025);
            double trailAlpha = soulAlpha * Math.max(0.035, 0.22 - i * 0.045);

            drawSoulSprite(gc, camera, player, pivotX, pivotY, trailSway, -trailRise + bob, trailScale, trailAlpha);
        }

        drawSoulSprite(gc, camera, player, pivotX, pivotY, sway, -rise + bob, soulScale, soulAlpha);
    }

    /**
     * Ve particle, vong nang luong va xung cuoi.
     */
    public void render(GraphicsContext gc, Camera camera) {
        if (gc == null || camera == null) {
            return;
        }

        double progress = getProgress();
        double zoom = camera.getZoom();
        double pixel = Math.max(2.0, Math.floor(2.0 * zoom));
        double screenX = snap(camera.worldToScreenX(position.getX()), pixel);
        double screenY = snap(camera.worldToScreenY(position.getY()) + 10.0 * zoom, pixel);
        gc.save();
        renderParticles(gc, screenX, screenY, pixel, zoom, progress);
        renderGroundRing(gc, screenX, screenY, pixel, zoom, progress);
        renderFinalPulse(gc, screenX, screenY, pixel, zoom, progress);
        gc.restore();
    }
    private void drawSoulSprite(GraphicsContext gc, Camera camera, Player player,
                                double pivotX, double pivotY,
                                double offsetX, double offsetY,
                                double scale, double alpha) {
        if (alpha <= 0.01) return;
        gc.save();
        gc.setGlobalAlpha(clamp01(alpha));
        gc.setEffect(soulGlow);
        gc.translate(offsetX, offsetY);
        gc.translate(pivotX, pivotY);
        gc.rotate(Math.sin(timer * 4.0) * 2.4);

        // Linh hon keo dai nhe theo chieu doc de tao cam giac bi hut len
        double stretchY = 1.0 + Math.sin(timer * 7.0) * 0.04;
        gc.scale(scale * 0.96, scale * stretchY);
        gc.translate(-pivotX, -pivotY);
        player.render(gc, camera);
        gc.restore();
    }

    private void renderSoulConnection(GraphicsContext gc, double bodyX, double bodyY,
                                      double soulX, double soulY, double alpha, double zoom) {
        if (alpha <= 0.01) return;

        gc.save();
        gc.setStroke(Color.rgb(150, 235, 255, alpha));
        gc.setLineWidth(Math.max(1.0, 1.5 * zoom));

        double middleX =
                (bodyX + soulX) / 2.0
                        + Math.sin(timer * 18.0)
                        * 3.0
                        * zoom;

        double middleY = (bodyY + soulY) / 2.0;

        gc.beginPath();
        gc.moveTo(bodyX, bodyY - 8.0 * zoom);
        gc.quadraticCurveTo(middleX, middleY, soulX, soulY);
        gc.stroke();

        gc.restore();
    }

    private void renderSoulReleasePulse(GraphicsContext gc,
                                        double screenX, double screenY,
                                        double local, double zoom) {
        if (local < 0.08 || local > 0.30) return;

        double pulse = clamp01((local - 0.08) / 0.22);
        double width = 24.0 * zoom * pulse;
        double height = width * 0.35;
        double alpha = (1.0 - pulse) * 0.65;

        gc.save();
        gc.setStroke(Color.rgb(180, 245, 255, alpha));
        gc.setLineWidth(Math.max(1.0, zoom));
        gc.strokeOval(
                screenX - width / 2.0,
                screenY - height / 2.0,
                width,
                height
        );
        gc.restore();
    }

    private void renderParticles(GraphicsContext gc, double screenX, double screenY,
                                 double pixel, double zoom, double progress) {
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            double local = clamp01(
                    (progress - particleDelay[i]) / (0.90 - particleDelay[i])
            );

            if (local <= 0.0 || local >= 1.0) continue;

            double eased = smoothStep(local);
            double angle = particleAngle[i] + particleSpin[i] * local * 1.45;
            double distance = particleDistance[i] * zoom * eased * 0.72;

            double x = screenX + Math.cos(angle) * distance;
            double y =
                    screenY
                            - 18.0 * zoom
                            + Math.sin(angle) * distance * 0.42
                            - local * 52.0 * zoom;

            double size = Math.max(
                    pixel,
                    particleSize[i] * pixel * (1.0 - local * 0.48)
            );

            double alpha = Math.sin(local * Math.PI) * 0.82;

            if (i % 5 == 0) {
                gc.setFill(Color.rgb(245, 255, 255, alpha));
            } else if (i % 2 == 0) {
                gc.setFill(Color.rgb(110, 225, 255, alpha));
            } else {
                gc.setFill(Color.rgb(45, 125, 200, alpha * 0.82));
            }

            gc.fillRect(snap(x, pixel), snap(y, pixel), size, size);
        }
    }

    private void renderGroundRing(GraphicsContext gc, double screenX, double screenY,
                                  double pixel, double zoom, double progress) {
        double local;

        if (progress < 0.12) {
            local = smoothStep(progress / 0.12);
        } else {
            local = 1.0 - smoothStep(clamp01((progress - 0.48) / 0.42));
        }

        if (local <= 0.01) return;

        double width = 58.0 * zoom * local;
        double height = Math.max(pixel * 2.0, 14.0 * zoom * local);

        gc.setStroke(Color.rgb(90, 220, 255, 0.72 * local));
        gc.setLineWidth(pixel);
        gc.strokeOval(screenX - width / 2.0, screenY - height / 2.0, width, height);

        gc.setStroke(Color.rgb(220, 250, 255, 0.45 * local));
        gc.strokeOval(screenX - width * 0.32, screenY - height * 0.23,
                width * 0.64, height * 0.46
        );
    }

    private void renderFinalPulse(GraphicsContext gc, double screenX, double screenY,
                                  double pixel, double zoom, double progress) {
        if (progress < 0.72) return;

        double local = clamp01((progress - 0.72) / 0.28);
        double width = 78.0 * zoom * local;
        double height = width * 0.34;
        double alpha = (1.0 - local) * 0.62;

        gc.setStroke(Color.rgb(130, 230, 255, alpha));
        gc.setLineWidth(pixel);
        gc.strokeOval(
                screenX - width / 2.0,
                screenY - height / 2.0,
                width,
                height
        );
    }

    private double getProgress() {
        return clamp01(timer / duration);
    }

    private static double smoothStep(double value) {
        value = clamp01(value);
        return value * value * (3.0 - 2.0 * value);
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static double snap(double value, double pixel) {
        return Math.floor(value / pixel) * pixel;
    }
}