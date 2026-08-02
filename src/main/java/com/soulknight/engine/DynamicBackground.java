package com.soulknight.engine;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.Stop;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Background neon trung co gom: bau troi, sao, trang, may toan man hinh, va mua.
 */
public final class DynamicBackground {

    private static final Color SKY_TOP = Color.rgb(5, 7, 22);
    private static final Color SKY_MIDDLE = Color.rgb(31, 15, 68);
    private static final Color SKY_BOTTOM = Color.rgb(8, 30, 52);

    private static final Color CYAN_GLOW = Color.rgb(0, 238, 255);
    private static final Color PURPLE_GLOW = Color.rgb(205, 62, 255);
    private static final Color RAIN_LIGHT = Color.rgb(115, 220, 255);
    private static final int MAX_RAIN_DROPS = 520;

    private final List<Star> stars = new ArrayList<>();
    private final List<RainDrop> rainDrops = new ArrayList<>();
    private final Random weatherRandom = new Random(20260803L);

    private double elapsedTime;
    private double cloudDrift;

    private double rainStrength = 0;
    private double targetRainStrength = 0.28;
    private double weatherTimer;
    private double weatherDuration = 14.0;

    private double windStrength = 0.18;

    private double lightningCooldown = 8.0;
    private boolean lightningActive;
    private double lightningTime;
    private double lightningAlpha;

    public DynamicBackground() {
        Random random = new Random(20260802L);

        for (int i = 0; i < 85; i++) {
            stars.add(new Star(random.nextDouble() * 2200.0,
                    18.0 + random.nextDouble() * 360.0,
                    1.0 + random.nextInt(2),
                    random.nextDouble() * Math.PI * 2.0));
        }

        createRainDrops();
    }

    private void createRainDrops() {
        for (int i = 0; i < MAX_RAIN_DROPS; i++) {
            RainDrop drop = new RainDrop();
            drop.x = weatherRandom.nextDouble();
            drop.y = weatherRandom.nextDouble();
            drop.depth = 0.35 + weatherRandom.nextDouble() * 0.65;
            drop.speed = 0.55 + weatherRandom.nextDouble() * 0.9;
            drop.length = 7.0 + weatherRandom.nextDouble() * 17.0;
            drop.activationLevel = weatherRandom.nextDouble();
            drop.phase = weatherRandom.nextDouble() * Math.PI * 2.0;
            rainDrops.add(drop);
        }
    }

    public void update(double deltaSeconds) {
        double delta = Math.max(0.0, Math.min(deltaSeconds, 0.05));

        elapsedTime += delta;
        cloudDrift += delta * 24.0;

        updateWeather(delta);
        updateRain(delta);
        updateLightning(delta);
    }

    private void updateWeather(double deltaSeconds) {
        weatherTimer += deltaSeconds;

        if (weatherTimer >= weatherDuration) {
            weatherTimer = 0.0;

            if (targetRainStrength < 0.1) {
                targetRainStrength = 0.22 + weatherRandom.nextDouble() * 0.14;
                weatherDuration = 12.0 + weatherRandom.nextDouble() * 12.0;
            } else {
                targetRainStrength = 0.0;
                weatherDuration = 15.0 + weatherRandom.nextDouble() * 15.0;
            }
        }

        double transitionSpeed = (targetRainStrength > rainStrength) ? 0.12 : 0.09;

        rainStrength += (targetRainStrength - rainStrength) * Math.min(1.0, deltaSeconds * transitionSpeed);

        if (targetRainStrength == 0.0 && rainStrength < 0.01) {
            rainStrength = 0.0;
        }

        windStrength = 0.10 + Math.sin(elapsedTime * 0.17) * 0.04 + Math.sin(elapsedTime * 0.043) * 0.025;
    }

    private void updateRain(double deltaSeconds) {
        double rainSpeedMultiplier = 0.58 + rainStrength * 0.38;

        for (RainDrop drop : rainDrops) {
            double speed = drop.speed * drop.depth * rainSpeedMultiplier;
            drop.y += speed * deltaSeconds;
            drop.x += windStrength * speed * deltaSeconds * 0.22;

            if (drop.y > 1.08 || drop.x > 1.08) {
                resetRainDrop(drop);
            }

            if (drop.x < -0.08) {
                drop.x = 1.05;
            }
        }
    }

    private void resetRainDrop(RainDrop drop) {
        drop.y = -0.05 - weatherRandom.nextDouble() * 0.25;
        drop.x = -0.05 + weatherRandom.nextDouble() * 1.05;
        drop.speed = 0.55 + weatherRandom.nextDouble() * 0.9;
        drop.length = 7.0 + weatherRandom.nextDouble() * 17.0;
    }

    private void updateLightning(double deltaSeconds) {
        if (!lightningActive) {
            lightningCooldown -= deltaSeconds;
            if (lightningCooldown <= 0.0) {
                startLightning();
            }
            return;
        }

        lightningTime += deltaSeconds;
        lightningAlpha = calculateLightningAlpha(lightningTime);

        if (lightningTime >= 0.52) {
            lightningActive = false;
            lightningTime = 0.0;
            lightningAlpha = 0.0;
            lightningCooldown = 7.0 + weatherRandom.nextDouble() * 11.0;
        }
    }

    private void startLightning() {
        lightningActive = true;
        lightningTime = 0.0;
        lightningAlpha = 0.0;
    }

    private double calculateLightningAlpha(double time) {
        if (time < 0.055) return time / 0.055 * 0.72;
        if (time < 0.13) return 0.72 * (1.0 - (time - 0.055) / 0.075);
        if (time < 0.20) return 0.04;
        if (time < 0.255) return 0.04 + ((time - 0.20) / 0.055) * 0.86;
        if (time < 0.52) return 0.90 * (1.0 - (time - 0.255) / 0.265);
        return 0.0;
    }

    public void render(GraphicsContext gc, Camera camera, double width, double height) {
        if (gc == null || camera == null || width <= 0.0 || height <= 0.0) {
            return;
        }

        gc.save();
        gc.setImageSmoothing(false);

        renderSky(gc, width, height);
        renderStars(gc, camera, width, height);
        renderMoon(gc, width, height);
        renderCloudBands(gc, camera, width, height);

        renderHorizonGlow(gc, width, height);
        renderRain(gc, width, height);
        renderLightningFlash(gc, width, height);

        gc.restore();
    }

    private void renderRain(GraphicsContext gc, double width, double height) {
        double visibleStrength = rainStrength;
        if (visibleStrength <= 0.01) {
            return;
        }
        double windPixelOffset = windStrength * 18.0;

        gc.save();

        for (RainDrop drop : rainDrops) {
            if (drop.activationLevel > visibleStrength) {
                continue;
            }

            double x = drop.x * width;
            double y = drop.y * height;
            double depth = drop.depth;
            double length = drop.length * (0.45 + visibleStrength * 0.45) * depth;
            double endX = x + windPixelOffset * depth;
            double endY = y + length;
            double alpha = (0.10 + visibleStrength * 0.30) * (0.40 + depth * 0.50);

            gc.setGlobalAlpha(Math.min(0.78, alpha));
            gc.setStroke(RAIN_LIGHT);
            gc.setLineWidth(depth > 0.82 ? 1.2 : 1.0);
            gc.strokeLine(Math.floor(x), Math.floor(y), Math.floor(endX), Math.floor(endY));
        }

        gc.restore();
    }

    private void renderLightningFlash(GraphicsContext gc, double width, double height) {
        if (lightningAlpha <= 0.001) {
            return;
        }

        gc.save();
        gc.setGlobalAlpha(lightningAlpha * 0.42);
        gc.setFill(Color.rgb(210, 235, 255));
        gc.fillRect(0.0, 0.0, width, height);

        gc.setGlobalAlpha(lightningAlpha * 0.22);
        gc.setFill(PURPLE_GLOW);
        gc.fillRect(0.0, height * 0.45, width, height * 0.55);
        gc.restore();
    }

    private void renderSky(GraphicsContext gc, double width, double height) {
        LinearGradient gradient = new LinearGradient(0, 0, 0, 1, true,
                CycleMethod.NO_CYCLE,
                new Stop(0.0, SKY_TOP),
                new Stop(0.58, SKY_MIDDLE),
                new Stop(1.0, SKY_BOTTOM));
        gc.setFill(gradient);
        gc.fillRect(0.0, 0.0, width, height);
    }

    private void renderStars(GraphicsContext gc, Camera camera, double width, double height) {
        double cameraOffset = camera.worldToScreenX(0.0) * 0.015;
        for (Star star : stars) {
            double x = wrap(star.x + cameraOffset, width + 80.0) - 40.0;
            if (star.y > height * 0.64) continue;

            double alpha = 0.45 + Math.sin(elapsedTime * 1.8 + star.phase) * 0.28;
            gc.setGlobalAlpha(Math.max(0.12, alpha));
            gc.setFill(star.size > 1.0 ? CYAN_GLOW : Color.WHITE);
            gc.fillRect(Math.floor(x), Math.floor(star.y), star.size, star.size);
        }
        gc.setGlobalAlpha(1.0);
    }

    private void renderMoon(GraphicsContext gc, double width, double height) {
        double radius = Math.min(width, height) * 0.075;
        double x = width * 0.76;
        double y = height * 0.19;

        gc.setGlobalAlpha(0.12);
        gc.setFill(PURPLE_GLOW);
        gc.fillOval(x - radius * 1.35, y - radius * 1.35, radius * 2.7, radius * 2.7);

        gc.setGlobalAlpha(0.86);
        gc.setFill(Color.rgb(216, 225, 255));
        gc.fillOval(x - radius, y - radius, radius * 2.0, radius * 2.0);

        gc.setGlobalAlpha(0.78);
        gc.setFill(SKY_MIDDLE);
        gc.fillOval(x - radius * 0.45, y - radius * 1.02, radius * 1.8, radius * 1.8);
        gc.setGlobalAlpha(1.0);
    }

    /**
     * Render mây phủ rộng toàn bộ chiều cao màn hình từ trên xuống dưới.
     */
    private void renderCloudBands(GraphicsContext gc, Camera camera, double width, double height) {
        double cameraOffset = camera.worldToScreenX(0.0) * 0.025;
        gc.save();

        // Tăng lên 7 tầng mây để phủ toàn bộ màn hình
        int totalBands = 7;

        for (int band = 0; band < totalBands; band++) {
            double direction = (band % 2 == 1) ? -1.0 : 1.0;
            double horizontalMovement = cloudDrift * (0.35 + band * 0.15) * direction;
            double layerWave = Math.sin(elapsedTime * (0.45 + band * 0.10) + band * 1.5) * (6.0 + band * 2.0);

            // Phân bổ độ cao từ 10% đến 85% chiều cao màn hình
            double heightRatio = 0.10 + (band / (double)(totalBands - 1)) * 0.75;
            double baseY = height * heightRatio + layerWave;

            double wrapWidth = width + 600.0;
            double baseX = wrap(cameraOffset + horizontalMovement + band * 190.0, wrapWidth) - 600.0;

            // Độ mờ nhạt dần về phía dưới để hòa trộn tự nhiên
            double alpha = 0.12 + (band % 3) * 0.04;

            gc.setFill(Color.rgb(45 + band * 6, 35 + band * 4, 85 + band * 7, alpha));

            // Vẽ dải đám mây từ trái sang phải
            for (int i = -1; i < 6; i++) {
                double x = baseX + i * 350.0;
                double cloudWave = Math.sin(elapsedTime * (0.65 + band * 0.06) + i * 1.25 + band * 1.8) * (4.0 + band);
                double y = baseY + cloudWave;

                // Scale mây lớn dần khi ở phía dưới tạo cảm giác chiều sâu 2.5D
                double scale = 0.75 + band * 0.14;
                renderPixelCloud(gc, Math.floor(x), Math.floor(y), scale);
            }
        }
        gc.restore();
    }

    private void renderPixelCloud(GraphicsContext gc, double x, double y, double scale) {
        double pixel = 8.0 * scale;
        gc.fillRect(x, y, pixel * 22.0, pixel * 2.0);
        gc.fillRect(x + pixel * 3.0, y - pixel * 2.0, pixel * 16.0, pixel * 2.0);
        gc.fillRect(x + pixel * 5.0, y - pixel * 4.0, pixel * 5.0, pixel * 2.0);
        gc.fillRect(x + pixel * 9.0, y - pixel * 5.0, pixel * 6.0, pixel * 3.0);
        gc.fillRect(x + pixel * 15.0, y - pixel * 3.0, pixel * 4.0, pixel);
        gc.fillRect(x + pixel * 2.0, y + pixel * 2.0, pixel * 5.0, pixel);
        gc.fillRect(x + pixel * 12.0, y + pixel * 2.0, pixel * 7.0, pixel);
    }

    private void renderHorizonGlow(GraphicsContext gc, double width, double height) {
        double y = height * 0.84;
        gc.setGlobalAlpha(0.16);
        gc.setFill(CYAN_GLOW);
        gc.fillRect(0.0, y, width, 2.0);
        gc.setGlobalAlpha(0.07);
        gc.fillRect(0.0, y - 8.0, width, 18.0);
        gc.setGlobalAlpha(1.0);
    }

    private double wrap(double value, double range) {
        double result = value % range;
        return result < 0.0 ? result + range : result;
    }

    private static final class RainDrop {
        private double x;
        private double y;
        private double speed;
        private double length;
        private double depth;
        private double activationLevel;
        private double phase;
    }

    private record Star(double x, double y, double size, double phase) {}
}