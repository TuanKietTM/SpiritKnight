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
 * su dung render tung layrer cua javafx
 * Background neon trung co gom: bau troi, sao, trang, thanh pho xa va thanh pho gan.
 */
public final class DynamicBackground {

    private static final Color SKY_TOP = Color.rgb(5, 7, 22);
    private static final Color SKY_MIDDLE = Color.rgb(31, 15, 68);
    private static final Color SKY_BOTTOM = Color.rgb(8, 30, 52);
    private static final Color FAR_CITY = Color.rgb(13, 18, 39);
    private static final Color NEAR_CITY = Color.rgb(7, 10, 25);
    private static final Color CYAN_GLOW = Color.rgb(0, 238, 255);
    private static final Color PURPLE_GLOW = Color.rgb(205, 62, 255);
    private static final Color RAIN_LIGHT = Color.rgb(115, 220, 255);
    private static final Color RAIN_HEAVY = Color.rgb(0, 238, 255);
    private static final int MAX_RAIN_DROPS = 520;

    private final List<Star> stars = new ArrayList<>();
    private final List<Tower> farTowers = new ArrayList<>();
    private final List<Tower> nearTowers = new ArrayList<>();

    // Danh sach mua duoc tao mot lan, khong tao lai moi frame
    private final List<RainDrop> rainDrops = new ArrayList<>();
    private final Random weatherRandom = new Random(20260803L);

    private double elapsedTime;
    private double cloudDrift;

    // Cuong do mua hien tai: 0.0 -> 1.0
    private double rainStrength = 0;
    private double targetRainStrength = 0.28;
    private double weatherTimer;
    private double weatherDuration = 14.0;

    // Huong gio lam hat mua nghieng
    private double windStrength = 0.18;

    // Dieu khien set
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

        createCityLayer(farTowers, random, 58, 45.0, 105.0, 55.0, 145.0);
        createCityLayer(nearTowers, random, 42, 70.0, 145.0, 100.0, 245.0);
        createRainDrops();
    }

    private void createRainDrops() {
        for (int i = 0; i < MAX_RAIN_DROPS; i++) {
            RainDrop drop = new RainDrop();

            drop.x = weatherRandom.nextDouble();
            drop.y = weatherRandom.nextDouble();

            // Hat mua xa roi cham va ngan, hat gan roi nhanh va dai
            drop.depth = 0.35 + weatherRandom.nextDouble() * 0.65;
            drop.speed = 0.55 + weatherRandom.nextDouble() * 0.9;
            drop.length = 7.0 + weatherRandom.nextDouble() * 17.0;

            // Moi hat chi xuat hien khi cuong do mua du lon
            drop.activationLevel = weatherRandom.nextDouble();
            drop.phase = weatherRandom.nextDouble() * Math.PI * 2.0;

            rainDrops.add(drop);
        }
    }

    private void createCityLayer(List<Tower> target, Random random, int count,
                                 double minWidth, double maxWidth,
                                 double minHeight, double maxHeight) {
        double x = -100.0;
        for (int i = 0; i < count; i++) {
            double width = minWidth + random.nextDouble() * (maxWidth - minWidth);
            double height = minHeight + random.nextDouble() * (maxHeight - minHeight);
            int style = random.nextInt(4);
            boolean cyan = random.nextBoolean();
            target.add(new Tower(x, width, height, style, cyan));
            x += width + 10.0 + random.nextDouble() * 28.0;
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

            /*
             * Chuyen doi giua hai trang thai:
             * 0.0: khong mua
             * 0.22 - 0.36: mua nho
             */
            if (targetRainStrength < 0.1) {
                // Tu khong mua chuyen sang mua nho
                targetRainStrength = 0.22 + weatherRandom.nextDouble() * 0.14;

                // Mua nho keo dai 12 - 24 giay
                weatherDuration = 12.0 + weatherRandom.nextDouble() * 12.0;
            } else {
                // Tu mua nho chuyen ve khong mua
                targetRainStrength = 0.0;
                // Troi kho keo dai 15 - 30 giay
                weatherDuration = 15.0 + weatherRandom.nextDouble() * 15.0;
            }
        }

        /*
         * Tang giam mua tu tu.
         * Tranh mua bien mat hoac xuat hien dot ngot.
         */
        double transitionSpeed;

        if (targetRainStrength > rainStrength) {
            transitionSpeed = 0.12;
        } else {
            transitionSpeed = 0.09;
        }

        rainStrength +=
                (targetRainStrength - rainStrength)
                        * Math.min(
                        1.0,
                        deltaSeconds * transitionSpeed
                );

        // Dua gia tri rat nho ve 0 de tat mua hoan toan
        if (targetRainStrength == 0.0 && rainStrength < 0.01) {
            rainStrength = 0.0;
        }

        // Gio nhe, tranh mua nghieng qua manh
        windStrength =
                0.10
                        + Math.sin(elapsedTime * 0.17) * 0.04
                        + Math.sin(elapsedTime * 0.043) * 0.025;
    }
    private void updateRain(double deltaSeconds) {
        double rainSpeedMultiplier = 0.58 + rainStrength * 0.38;

        for (RainDrop drop : rainDrops) {
            double speed = drop.speed * drop.depth * rainSpeedMultiplier;

            drop.y += speed * deltaSeconds;

            // Gio day hat mua sang phai
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

            // Lan set tiep theo cach 7 den 18 giay
            lightningCooldown = 7.0 + weatherRandom.nextDouble() * 11.0;
        }
    }

    private void startLightning() {
        lightningActive = true;
        lightningTime = 0.0;
        lightningAlpha = 0.0;
    }

    private double calculateLightningAlpha(double time) {
        // Chop lan dau
        if (time < 0.055) {
            return time / 0.055 * 0.72;
        }

        // Tat nhanh
        if (time < 0.13) {
            return 0.72 * (1.0 - (time - 0.055) / 0.075);
        }

        // Khoang toi ngan giua hai lan chop
        if (time < 0.20) {
            return 0.04;
        }

        // Chop lan hai manh hon
        if (time < 0.255) {
            return 0.04 + ((time - 0.20) / 0.055) * 0.86;
        }

        // Sang mo dan
        if (time < 0.52) {
            return 0.90 * (1.0 - (time - 0.255) / 0.265);
        }

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
        renderCityLayer(gc, camera, width, height, farTowers, 0.035, 0.78, FAR_CITY, 0.5);
        renderCityLayer(gc, camera, width, height, nearTowers, 0.075, 0.90, NEAR_CITY, 1.0);
        renderHorizonGlow(gc, width, height);

        // Mua nam tren thanh pho nhung van nam phia sau map
        renderRain(gc, width, height);

        // Anh chop set phu len toan bo background
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
            double visibleDropRatio = visibleStrength * 0.72;
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

    private void renderCloudBands(GraphicsContext gc, Camera camera, double width, double height) {
        double cameraOffset = camera.worldToScreenX(0.0) * 0.025;
        gc.save();
        for (int band = 0; band < 3; band++) {
            /*
             * Layer 0 va 2 troi sang phai.
             * Layer 1 troi nguoc sang trai de tao chieu sau.
             */
            double direction = band == 1 ? -1.0 : 1.0;

            double horizontalMovement =
                    cloudDrift
                            * (0.55 + band * 0.30)
                            * direction;

            /*
             * Moi layer luon len xuong voi toc do va bien do khac nhau.
             */
            double layerWave = Math.sin(elapsedTime * (0.55 + band * 0.12) + band * 1.8) * (5.0 + band * 2.0);

            double baseY = height * (0.23 + band * 0.12) + layerWave;

            double wrapWidth = width + 500.0;

            double baseX = wrap(cameraOffset + horizontalMovement + band * 260.0, wrapWidth) - 500.0;

            /*
             * May xa mo hon, may gan ro hon.
             */
            double alpha = 0.16 + band * 0.055;

            gc.setFill(Color.rgb(72 + band * 8, 55 + band * 5, 112 + band * 9, alpha));

            for (int i = -1; i < 5; i++) {
                double x = baseX + i * 390.0;

                /*
                 * Moi cum may tu luon rieng,
                 * tranh cac cum di chuyen cung mot duong thang.
                 */
                double cloudWave =
                        Math.sin(
                                elapsedTime * (0.75 + band * 0.08)
                                        + i * 1.35
                                        + band * 2.1
                        ) * (3.0 + band);

                double y = baseY + cloudWave;

                renderPixelCloud(gc, Math.floor(x), Math.floor(y), 0.82 + band * 0.12);
            }
        }

        gc.restore();
    }
    private void renderPixelCloud(GraphicsContext gc, double x, double y, double scale) {
        double pixel = 8.0 * scale;
        /*
         * Than may chinh
         */
        gc.fillRect(x, y, pixel * 22.0, pixel * 2.0);
        /*
         * Tang may thu hai
         */
        gc.fillRect(x + pixel * 3.0, y - pixel * 2.0, pixel * 16.0, pixel * 2.0);
        /*
         * Cum may ben trai
         */
        gc.fillRect(x + pixel * 5.0, y - pixel * 4.0, pixel * 5.0, pixel * 2.0);

        /*
         * Cum may chinh o giua
         */
        gc.fillRect(x + pixel * 9.0, y - pixel * 5.0, pixel * 6.0, pixel * 3.0);

        /*
         * Cum may ben phai
         */
        gc.fillRect(x + pixel * 15.0, y - pixel * 3.0, pixel * 4.0, pixel);

        /*
         * Cac duoi may nho
         */
        gc.fillRect(x + pixel * 2.0, y + pixel * 2.0, pixel * 5.0, pixel);

        gc.fillRect(x + pixel * 12.0, y + pixel * 2.0, pixel * 7.0, pixel);
    }
    private void renderCityLayer(GraphicsContext gc, Camera camera, double width, double height,
                                 List<Tower> towers, double parallax, double groundRatio,
                                 Color bodyColor, double glowStrength) {
        double baseY = height * groundRatio;
        double cameraOffset = camera.worldToScreenX(0.0) * parallax;
        double totalWidth = towers.get(towers.size() - 1).x + towers.get(towers.size() - 1).width + 130.0;

        for (int repeat = -1; repeat <= 1; repeat++) {
            for (Tower tower : towers) {
                double x = tower.x + cameraOffset + repeat * totalWidth;
                if (x + tower.width < -20.0 || x > width + 20.0) continue;
                renderTower(gc, x, baseY, tower, bodyColor, glowStrength);
            }
        }

        gc.setFill(bodyColor);
        gc.fillRect(0.0, baseY, width, height - baseY);
    }

    private void renderTower(GraphicsContext gc, double x, double baseY, Tower tower,
                             Color bodyColor, double glowStrength) {
        double topY = baseY - tower.height;
        Color glow = tower.cyan ? CYAN_GLOW : PURPLE_GLOW;

        gc.setGlobalAlpha(0.10 * glowStrength);
        gc.setFill(glow);
        gc.fillRect(x - 4.0, topY - 8.0, tower.width + 8.0, tower.height + 8.0);

        gc.setGlobalAlpha(1.0);
        gc.setFill(bodyColor);
        gc.fillRect(Math.floor(x), Math.floor(topY), Math.ceil(tower.width), Math.ceil(tower.height));

        switch (tower.style) {
            case 0 -> renderCastleRoof(gc, x, topY, tower.width, bodyColor);
            case 1 -> renderSpireRoof(gc, x, topY, tower.width, bodyColor);
            case 2 -> renderBattlement(gc, x, topY, tower.width, bodyColor);
            default -> renderTwinSpire(gc, x, topY, tower.width, bodyColor);
        }

        gc.setGlobalAlpha(0.72 * glowStrength);
        gc.setFill(glow);
        double windowY = topY + 18.0;
        for (int row = 0; windowY + row * 24.0 < baseY - 10.0; row++) {
            for (double windowX = x + 12.0; windowX < x + tower.width - 7.0; windowX += 20.0) {
                if (((int) (windowX + row * 3.0)) % 4 != 0) {
                    gc.fillRect(Math.floor(windowX), Math.floor(windowY + row * 24.0), 4.0, 8.0);
                }
            }
        }
        gc.setGlobalAlpha(1.0);
    }

    private void renderCastleRoof(GraphicsContext gc, double x, double topY, double width, Color color) {
        gc.setFill(color);
        double roofHeight = Math.min(38.0, width * 0.42);
        gc.fillPolygon(new double[]{x - 5.0, x + width / 2.0, x + width + 5.0},
                new double[]{topY, topY - roofHeight, topY}, 3);
        gc.fillRect(x + width / 2.0 - 2.0, topY - roofHeight - 19.0, 4.0, 20.0);
    }

    private void renderSpireRoof(GraphicsContext gc, double x, double topY, double width, Color color) {
        gc.setFill(color);
        gc.fillPolygon(new double[]{x + 4.0, x + width / 2.0, x + width - 4.0},
                new double[]{topY, topY - Math.min(72.0, width), topY}, 3);
    }

    private void renderBattlement(GraphicsContext gc, double x, double topY, double width, Color color) {
        gc.setFill(color);
        for (double bx = x; bx < x + width; bx += 18.0) {
            gc.fillRect(Math.floor(bx), Math.floor(topY - 12.0), 10.0, 14.0);
        }
    }

    private void renderTwinSpire(GraphicsContext gc, double x, double topY, double width, Color color) {
        gc.setFill(color);
        double half = width / 2.0;
        gc.fillPolygon(new double[]{x, x + half * 0.45, x + half * 0.9},
                new double[]{topY, topY - 45.0, topY}, 3);
        gc.fillPolygon(new double[]{x + half, x + half * 1.5, x + width},
                new double[]{topY, topY - 58.0, topY}, 3);
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
    private record Tower(double x, double width, double height, int style, boolean cyan) {}
}