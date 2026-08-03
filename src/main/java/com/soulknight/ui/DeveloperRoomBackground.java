package com.soulknight.ui;

import javafx.animation.AnimationTimer;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.Stop;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Background phong lam viec cua dev  luc dem muon.
 */
public final class DeveloperRoomBackground {

    private static final DateTimeFormatter CLOCK_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final String[] CODE_LINES = {
            "public void update(double dt) {",
            "    input.poll();",
            "    renderer.render();",
            "}",
            "",
            "build: SUCCESS",
            "autosave: complete",
            "tests: 18 passed",
            "branch: feature/ui",
            "fps: stable"
    };

    private final Canvas canvas;
    private final GraphicsContext gc;
    private final Random random = new Random(24062026L);
    private final List<RainDrop> rainDrops = new ArrayList<>();
    private final List<SteamParticle> steamParticles = new ArrayList<>();

    private AnimationTimer timer;
    private long lastFrameNanos;
    private double elapsed;
    private double codeOffset;
    private double cursorTimer;
    private double routerTimer;
    private double roomPulse;

    private final DoubleProperty roomReveal = new SimpleDoubleProperty(1.0);
    private final DoubleProperty monitorPower = new SimpleDoubleProperty(1.0);
    private final DoubleProperty rainReveal = new SimpleDoubleProperty(1.0);
    private final DoubleProperty ambientReveal = new SimpleDoubleProperty(1.0);

    public DeveloperRoomBackground(Canvas canvas) {
        this.canvas = canvas;
        this.gc = canvas.getGraphicsContext2D();
        initializeParticles();
    }

    public void start() {
        if (timer != null) return;

        lastFrameNanos = 0L;
        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (lastFrameNanos == 0L) {
                    lastFrameNanos = now;
                    render();
                    return;
                }

                double deltaSeconds = Math.min((now - lastFrameNanos) / 1_000_000_000.0, 0.05);
                lastFrameNanos = now;
                update(deltaSeconds);
                render();
            }
        };
        timer.start();
    }

    public void stop() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
        lastFrameNanos = 0L;
    }

    public boolean isRunning() {
        return timer != null;
    }

    public void prepareCinematicReveal() {
        roomReveal.set(0.0);
        monitorPower.set(0.0);
        rainReveal.set(0.0);
        ambientReveal.set(0.0);
        render();
    }

    public void showImmediately() {
        roomReveal.set(1.0);
        monitorPower.set(1.0);
        rainReveal.set(1.0);
        ambientReveal.set(1.0);
        render();
    }

    public DoubleProperty roomRevealProperty() { return roomReveal; }
    public DoubleProperty monitorPowerProperty() { return monitorPower; }
    public DoubleProperty rainRevealProperty() { return rainReveal; }
    public DoubleProperty ambientRevealProperty() { return ambientReveal; }

    private void initializeParticles() {
        rainDrops.clear();
        steamParticles.clear();

        for (int i = 0; i < 80; i++) {
            rainDrops.add(new RainDrop(random.nextDouble(), random.nextDouble(),
                    0.22 + random.nextDouble() * 0.35,
                    7.0 + random.nextDouble() * 12.0,
                    0.10 + random.nextDouble() * 0.22));
        }

        for (int i = 0; i < 7; i++) {
            steamParticles.add(new SteamParticle(
                    random.nextDouble(), random.nextDouble(),
                    8.0 + random.nextDouble() * 10.0,
                    0.07 + random.nextDouble() * 0.08));
        }
    }

    private void update(double deltaSeconds) {
        elapsed += deltaSeconds;
        codeOffset += deltaSeconds * 9.0;
        cursorTimer += deltaSeconds;
        routerTimer += deltaSeconds;
        roomPulse = (Math.sin(elapsed * 0.65) + 1.0) * 0.5;

        for (RainDrop drop : rainDrops) {
            drop.y += drop.speed * deltaSeconds;
            drop.x -= drop.speed * 0.18 * deltaSeconds;
            if (drop.y > 1.08 || drop.x < -0.08) {
                drop.x = 0.10 + random.nextDouble() * 0.95;
                drop.y = -random.nextDouble() * 0.15;
            }
        }

        for (SteamParticle steam : steamParticles) {
            steam.life += deltaSeconds * steam.speed;
            if (steam.life > 1.0) {
                steam.life = 0.0;
                steam.offsetX = random.nextDouble();
            }
        }
    }

    private void render() {
        double width = canvas.getWidth();
        double height = canvas.getHeight();
        if (width <= 1.0 || height <= 1.0) return;

        gc.clearRect(0.0, 0.0, width, height);

        // Ve toan bo can phong, sau do phu bong toi de tao silhouette.( hieu ung hien dan dan _)
//        ban dau can phong toi sau do cac doi tuong hien ra
        renderRoom(width, height);
        renderWindow(width, height);
        renderDesk(width, height);
        renderCoffee(width, height);
        renderMonitorSilhouette(width, height);

        double visibleRoom = clamp(roomReveal.get());
        gc.setFill(Color.rgb(0, 0, 0, 0.92 - visibleRoom * 0.72));
        gc.fillRect(0.0, 0.0, width, height);

        double monitor = clamp(monitorPower.get());
        if (monitor > 0.001) {
            gc.save();
            gc.setGlobalAlpha(monitor);
            renderSideMonitor(width, height);
            renderMonitorGlow(width, height, monitor);
            gc.restore();
        }

        double rain = clamp(rainReveal.get());
        if (rain > 0.001) {
            gc.save();
            gc.setGlobalAlpha(rain);
            renderRainInWindow(width, height);
            gc.restore();
        }

        double ambient = clamp(ambientReveal.get());
        if (ambient > 0.001) {
            gc.save();
            gc.setGlobalAlpha(ambient);
            renderAmbientLight(width, height);
            gc.restore();
        }

        renderVignette(width, height);
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private void renderMonitorSilhouette(double width, double height) {
//        ban đầu phủ tôi
        double x = width * 0.69;
        double y = height * 0.16;
        double w = width * 0.255;
        double h = height * 0.43;
        gc.setFill(Color.rgb(3, 5, 8));
        gc.fillRect(x - 9.0, y - 9.0, w + 18.0, h + 18.0);
        gc.setFill(Color.rgb(10, 13, 17));
        gc.fillRect(x, y, w, h);
    }

    private void renderMonitorGlow(double width, double height, double power) {
        gc.setFill(Color.rgb(91, 130, 148, 0.11 * power));
        gc.fillOval(width * 0.47, height * 0.03, width * 0.55, height * 0.78);
    }

    private void renderRainInWindow(double width, double height) {
        double x = width * 0.055;
        double y = height * 0.12;
        double w = width * 0.285;
        double h = height * 0.50;
        renderRain(x, y, w, h);
    }

    private void renderRoom(double width, double height) {
        LinearGradient wall = new LinearGradient(
                0.0, 0.0, 0.0, 1.0, true, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.rgb(14, 21, 31)),
                new Stop(0.62, Color.rgb(10, 16, 24)),
                new Stop(1.0, Color.rgb(6, 10, 16))
        );
        gc.setFill(wall);
        gc.fillRect(0.0, 0.0, width, height);

        gc.setFill(Color.rgb(21, 30, 40, 0.55));
        gc.fillRect(0.0, height * 0.12, width, 2.0);
        gc.fillRect(0.0, height * 0.78, width, 3.0);

        gc.setFill(Color.rgb(8, 12, 18, 0.7));
        gc.fillRect(width * 0.40, height * 0.10, width * 0.20, height * 0.68);
    }

    private void renderWindow(double width, double height) {
        double x = width * 0.055;
        double y = height * 0.12;
        double w = width * 0.285;
        double h = height * 0.50;

        gc.setFill(Color.rgb(3, 8, 15));
        gc.fillRect(x - 10.0, y - 10.0, w + 20.0, h + 20.0);
        gc.setFill(Color.rgb(33, 42, 52));
        gc.fillRect(x - 5.0, y - 5.0, w + 10.0, h + 10.0);

        LinearGradient night = new LinearGradient(
                0.0, 0.0, 0.0, 1.0, true, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.rgb(8, 18, 33)),
                new Stop(1.0, Color.rgb(18, 31, 43))
        );
        gc.setFill(night);
        gc.fillRect(x, y, w, h);

        renderCity(x, y, w, h);

        gc.setFill(Color.rgb(39, 48, 58));
        gc.fillRect(x + w * 0.49, y, 6.0, h);
        gc.fillRect(x, y + h * 0.53, w, 6.0);

        gc.setFill(Color.rgb(159, 179, 194, 0.08));
        gc.fillPolygon(new double[]{x + 8, x + w * 0.35, x + w * 0.18},
                new double[]{y + 8, y + 8, y + h * 0.46}, 3);
    }

    private void renderCity(double x, double y, double w, double h) {
        gc.setFill(Color.rgb(6, 12, 20, 0.95));
        double base = y + h;
        double buildingWidth = w / 11.0;

        for (int i = 0; i < 12; i++) {
            double bh = h * (0.16 + ((i * 37) % 100) / 260.0);
            double bx = x + i * buildingWidth - 2.0;
            gc.fillRect(bx, base - bh, buildingWidth + 3.0, bh);

            for (int row = 0; row < 4; row++) {
                for (int col = 0; col < 2; col++) {
                    if ((i + row + col) % 3 == 0) {
                        gc.setFill(Color.rgb(205, 174, 94, 0.25));
                        gc.fillRect(bx + 5 + col * 9, base - bh + 10 + row * 15, 4.0, 5.0);
                    }
                }
            }
            gc.setFill(Color.rgb(6, 12, 20, 0.95));
        }
    }

    private void renderRain(double x, double y, double w, double h) {
        gc.save();
        gc.beginPath();
        gc.rect(x, y, w, h);
        gc.closePath();
        gc.clip();
        gc.setLineWidth(1.0);

        for (RainDrop drop : rainDrops) {
            double px = x + drop.x * w;
            double py = y + drop.y * h;
            gc.setStroke(Color.rgb(154, 184, 205, drop.opacity));
            gc.strokeLine(px, py, px - drop.length * 0.22, py + drop.length);
        }
        gc.restore();
    }

    private void renderDesk(double width, double height) {
        double deskY = height * 0.76;
        gc.setFill(Color.rgb(18, 15, 14));
        gc.fillRect(0.0, deskY, width, height - deskY);
        gc.setFill(Color.rgb(58, 43, 32));
        gc.fillRect(0.0, deskY, width, 15.0);
        gc.setFill(Color.rgb(91, 65, 44, 0.35));
        gc.fillRect(0.0, deskY + 3.0, width, 2.0);

        double keyboardX = width * 0.39;
        double keyboardY = height * 0.85;
        double keyboardW = width * 0.22;
        double keyboardH = height * 0.055;
        gc.setFill(Color.rgb(18, 23, 29));
        gc.fillRoundRect(keyboardX, keyboardY, keyboardW, keyboardH, 5.0, 5.0);
        gc.setStroke(Color.rgb(67, 78, 89, 0.7));
        gc.strokeRoundRect(keyboardX, keyboardY, keyboardW, keyboardH, 5.0, 5.0);

        gc.setFill(Color.rgb(118, 130, 139, 0.25));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 12; col++) {
                gc.fillRect(keyboardX + 10 + col * (keyboardW - 20) / 12.0,
                        keyboardY + 8 + row * 9.0, 7.0, 4.0);
            }
        }
    }

    private void renderSideMonitor(double width, double height) {
        double x = width * 0.69;
        double y = height * 0.16;
        double w = width * 0.255;
        double h = height * 0.43;

        gc.setFill(Color.rgb(5, 8, 12));
        gc.fillRect(x - 9.0, y - 9.0, w + 18.0, h + 18.0);
        gc.setFill(Color.rgb(28, 36, 44));
        gc.fillRect(x - 5.0, y - 5.0, w + 10.0, h + 10.0);
        gc.setFill(Color.rgb(7, 14, 18));
        gc.fillRect(x, y, w, h);

        gc.setFill(Color.rgb(114, 154, 137, 0.76));
        gc.setFont(javafx.scene.text.Font.font("Consolas", Math.max(10.0, width * 0.0105)));

        double lineHeight = Math.max(14.0, height * 0.025);
        double totalHeight = CODE_LINES.length * lineHeight;
        double offset = codeOffset % totalHeight;

        for (int repeat = -1; repeat <= 1; repeat++) {
            for (int i = 0; i < CODE_LINES.length; i++) {
                double lineY = y + 23.0 + i * lineHeight - offset + repeat * totalHeight;
                if (lineY > y + 12.0 && lineY < y + h - 18.0) {
                    gc.fillText(CODE_LINES[i], x + 14.0, lineY);
                }
            }
        }

        if (((int) (cursorTimer * 2.0)) % 2 == 0) {
            gc.setFill(Color.rgb(166, 192, 174, 0.8));
            gc.fillRect(x + 14.0, y + h - 24.0, 8.0, 2.0);
        }

        gc.setFill(Color.rgb(12, 17, 21));
        gc.fillRect(x + w * 0.44, y + h + 8.0, w * 0.12, height * 0.10);
        gc.fillRect(x + w * 0.28, y + h + height * 0.10, w * 0.44, 8.0);

        gc.setFill(Color.rgb(169, 178, 184, 0.75));
        gc.setFont(javafx.scene.text.Font.font("Consolas", Math.max(11.0, width * 0.012)));
        gc.fillText(LocalTime.now().format(CLOCK_FORMAT), x + w - 52.0, y + h - 12.0);

        boolean routerOn = ((int) (routerTimer * 1.4)) % 2 == 0;
        gc.setFill(routerOn ? Color.rgb(106, 168, 114, 0.85) : Color.rgb(45, 74, 50, 0.7));
        gc.fillOval(x + w - 16.0, y + 9.0, 5.0, 5.0);
    }

    private void renderCoffee(double width, double height) {
        double cupX = width * 0.82;
        double cupY = height * 0.78;
        double cupW = width * 0.055;
        double cupH = height * 0.075;

        gc.setFill(Color.rgb(112, 82, 58));
        gc.fillRoundRect(cupX, cupY, cupW, cupH, 5.0, 5.0);
        gc.setStroke(Color.rgb(140, 104, 74));
        gc.setLineWidth(5.0);
        gc.strokeOval(cupX + cupW - 4.0, cupY + cupH * 0.25, cupW * 0.48, cupH * 0.52);
        gc.setFill(Color.rgb(40, 25, 18));
        gc.fillOval(cupX + 5.0, cupY + 4.0, cupW - 10.0, 7.0);

        for (SteamParticle steam : steamParticles) {
            double life = steam.life;
            double sx = cupX + cupW * (0.28 + steam.offsetX * 0.42) + Math.sin(elapsed * 1.4 + life * 7.0) * 5.0;
            double sy = cupY - life * height * 0.10;
            gc.setStroke(Color.rgb(186, 194, 196, Math.max(0.0, (1.0 - life) * steam.opacity)));
            gc.setLineWidth(2.0);
            gc.strokeLine(sx, sy, sx + 4.0, sy - steam.length);
        }
    }

    private void renderAmbientLight(double width, double height) {
        double alpha = 0.035 + roomPulse * 0.025;
        gc.setFill(Color.rgb(91, 130, 148, alpha));
        gc.fillOval(width * 0.28, height * 0.08, width * 0.45, height * 0.70);

        gc.setFill(Color.rgb(205, 142, 70, 0.035));
        gc.fillOval(width * 0.72, height * 0.48, width * 0.26, height * 0.34);
    }

    private void renderVignette(double width, double height) {
        gc.setFill(Color.rgb(0, 0, 0, 0.34));
        gc.fillRect(0.0, 0.0, width, height * 0.07);
        gc.fillRect(0.0, height * 0.93, width, height * 0.07);
        gc.fillRect(0.0, 0.0, width * 0.035, height);
        gc.fillRect(width * 0.965, 0.0, width * 0.035, height);
    }

    private static final class RainDrop {
        private double x;
        private double y;
        private final double speed;
        private final double length;
        private final double opacity;

        private RainDrop(double x, double y, double speed, double length, double opacity) {
            this.x = x;
            this.y = y;
            this.speed = speed;
            this.length = length;
            this.opacity = opacity;
        }
    }

    private static final class SteamParticle {
        private double offsetX;
        private double life;
        private final double length;
        private final double opacity;
        private final double speed = 0.34;

        private SteamParticle(double offsetX, double life, double length, double opacity) {
            this.offsetX = offsetX;
            this.life = life;
            this.length = length;
            this.opacity = opacity;
        }
    }
}
