package com.soulknight.ui;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
public final class MainMenuRealityBreachBackground {
    private static final double VW = 512.0;
    private static final double VH = 288.0;
    private static final int RAIN_COUNT = 100;
    private static final int GLASS_DROP_COUNT = 34;
    private static final int CODE_GLYPH_COUNT = 24;
    private static final int STAR_COUNT = 42;
    private final Canvas canvas;
    private final GraphicsContext gc;
    private final Random random = new Random(26082026L);
    private final List<RainDrop> rain = new ArrayList<>();
    private final List<GlassDrop> glassDrops = new ArrayList<>();
    private final List<CodeGlyph> codeGlyphs = new ArrayList<>();
    private final List<Star> stars = new ArrayList<>();
    private AnimationTimer timer;
    private long lastFrameNanos;
    private double elapsed;
    private double portalPulse;
    private double codeScroll;
    private double cityDrift;
    private double fantasyDrift;
    private double nextLightning = 8.0;
    private double lightningLife;
    private double nextGlitch = 2.8;
    private double glitchLife;
    public MainMenuRealityBreachBackground(Canvas canvas) {
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
                double dt = Math.min((now - lastFrameNanos) / 1_000_000_000.0, 0.05);
                lastFrameNanos = now;
                update(dt);
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

    public void renderImmediately() {
        render();
    }

    private void initializeParticles() {
        rain.clear();
        glassDrops.clear();
        codeGlyphs.clear();
        stars.clear();

        for (int i = 0; i < RAIN_COUNT; i++) {
            rain.add(new RainDrop(random.nextDouble(),
                    random.nextDouble(), 0.25 + random.nextDouble() * 0.55,
                    3 + random.nextInt(8), 0.07 + random.nextDouble() * 0.17));
        }

        for (int i = 0; i < GLASS_DROP_COUNT; i++) {
            glassDrops.add(new GlassDrop(0.02 + random.nextDouble() * 0.96,
                    random.nextDouble(), 1 + random.nextInt(2),
                    0.015 + random.nextDouble() * 0.035, 0.07 + random.nextDouble() * 0.14));
        }
        String[] glyphs = {
                "{", "}", "01", "10", ">", "_", "[]", "::",
                "GameWorld", "build", "SYNC", "ERROR", "Knight",
                "0xAF", "void", "class", "run()", "save()", "PORTAL"
        };
        for (int i = 0; i < CODE_GLYPH_COUNT; i++) {
            codeGlyphs.add(new CodeGlyph(0.03 + random.nextDouble() * 0.92,
                    random.nextDouble(), glyphs[random.nextInt(glyphs.length)],
                    0.04 + random.nextDouble() * 0.10, 0.03 + random.nextDouble() * 0.05));
        }
        for (int i = 0; i < STAR_COUNT; i++) {
            stars.add(new Star(random.nextDouble(), random.nextDouble(),
                    random.nextBoolean() ? 1 : 2, 0.12 + random.nextDouble() * 0.38));
        }
    }

    private void update(double dt) {
        elapsed += dt;
        portalPulse = (Math.sin(elapsed * 1.5) + 1.0) * 0.5;
        codeScroll += dt * 4.0;
        cityDrift += dt * 0.40;
        fantasyDrift += dt * 0.16;
        for (RainDrop drop : rain) {
            drop.y += drop.speed * dt;
            drop.x -= drop.speed * 0.12 * dt;
            if (drop.y > 1.08 || drop.x < -0.08) {
                drop.x = 0.05 + random.nextDouble();
                drop.y = -0.15 - random.nextDouble() * 0.25;
            }
        }

        for (GlassDrop drop : glassDrops) {
            drop.y += drop.speed * dt;
            if (drop.y > 1.05) {
                drop.y = -0.08 - random.nextDouble() * 0.18;
                drop.x = 0.02 + random.nextDouble() * 0.96;
            }
        }
        for (CodeGlyph glyph : codeGlyphs) {
            glyph.y -= glyph.speed * dt;
            if (glyph.y < -0.08) {
                glyph.y = 1.08;
                glyph.x = 0.02 + random.nextDouble() * 0.96;
            }
        }
        if (lightningLife > 0) {
            lightningLife -= dt;
        } else {
            nextLightning -= dt;
            if (nextLightning <= 0) {
                lightningLife = 0.14;
                nextLightning = 9.0 + random.nextDouble() * 12.0;
            }
        }
        if (glitchLife > 0) {
            glitchLife -= dt;
        } else {
            nextGlitch -= dt;
            if (nextGlitch <= 0) {
                glitchLife = 0.12 + random.nextDouble() * 0.12;
                nextGlitch = 2.5 + random.nextDouble() * 4.0;
            }
        }
    }

    private void render() {
        double width = canvas.getWidth();
        double height = canvas.getHeight();
        if (width <= 1 || height <= 1) return;
        gc.clearRect(0, 0, width, height);
        gc.save();
        gc.scale(width / VW, height / VH);
      // cac lop render
        renderRoomBase();
        renderLeftMenuSafeZone();
        renderWindowBase();
        renderCoderWorld();
        renderWindowFrame();
        renderRain();
        renderGlassDrops();
        renderCodeReflection();
        renderDeskForeground();
        renderWarmRoomLight();
        renderLightningReveal();
        renderVignette();
        gc.restore();
    }
    private void renderRoomBase() {
        LinearGradient room = new LinearGradient(
                0, 0, 0, 1,
                true,
                CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.rgb(8, 13, 22)),
                new Stop(0.62, Color.rgb(6, 10, 17)),
                new Stop(1.0, Color.rgb(3, 6, 10))
        );
        gc.setFill(room);
        gc.fillRect(0, 0, VW, VH);

        gc.setFill(Color.rgb(21, 28, 38, 0.50));
        gc.fillRect(0, 48, VW, 1);
        gc.fillRect(0, 237, VW, 2);
    }

    /**
     * Dark visual-safe region behind the menu buttons.
     */
    private void renderLeftMenuSafeZone() {
        LinearGradient shade = new LinearGradient(
                0, 0, 1, 0,
                true,
                CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.rgb(0, 3, 9, 0.65)),
                new Stop(0.78, Color.rgb(0, 3, 9, 0.37)),
                new Stop(1.0, Color.rgb(0, 3, 9, 0.05))
        );
        gc.setFill(shade);
        gc.fillRect(0, 0, 220, VH);
        // Dev-room details.
        gc.setFill(Color.rgb(14, 21, 31));
        gc.fillRect(19, 73, 41, 28);
        gc.fillRect(67, 65, 28, 37);
        gc.setStroke(Color.rgb(74, 88, 101, 0.22));
        gc.setLineWidth(1);
        gc.strokeRect(19, 73, 41, 28);
        gc.strokeRect(67, 65, 28, 37);
        // Cable.
        gc.setStroke(Color.rgb(11, 16, 23, 0.80));
        gc.strokeLine(112, 42, 112, 145);
        gc.strokeLine(112, 145, 121, 153);
    }
    private void renderWindowBase() {
        gc.setFill(Color.rgb(2, 6, 12));
        gc.fillRect(216, 18, 291, 221);
        gc.setFill(Color.rgb(24, 35, 45));
        gc.fillRect(219, 21, 285, 215);
        gc.setFill(Color.rgb(6, 17, 30));
        gc.fillRect(223, 25, 277, 207);
    }
    private void renderWindowFrame() {
        final int x = 219;
        final int y = 21;
        final int w = 285;
        final int h = 215;

        gc.setFill(Color.rgb(29, 40, 49));
        gc.fillRect(x - 4, y - 4, w + 8, 4);
        gc.fillRect(x - 4, y + h, w + 8, 5);
        gc.fillRect(x - 4, y, 4, h);
        gc.fillRect(x + w, y, 5, h);

        gc.setFill(Color.rgb(67, 82, 94, 0.34));
        gc.fillRect(x, y, w, 1);
        gc.fillRect(x, y, 1, h);
    }
    private void renderCoderWorld() {
        final int x = 223;
        final int y = 25;
        final int w = 277;
        final int h = 207;
        LinearGradient sky = new LinearGradient(
                0, 0, 0, 1,
                true,
                CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.rgb(5, 17, 32)),
                new Stop(0.45, Color.rgb(10, 28, 45)),
                new Stop(1.0, Color.rgb(12, 24, 34))
        );
        gc.setFill(sky);
        gc.fillRect(x, y, w, h);
        // Clouds.
        gc.setFill(Color.rgb(77, 105, 127, 0.10));
        for (int i = 0; i < 7; i++) {
            int cx = x + Math.floorMod((int)(i * 53 + cityDrift * 3), w + 40) - 20;
            int cy = y + 15 + (i % 4) * 15;
            int cw = 31 + (i % 3) * 12;
            gc.fillRect(cx, cy, cw, 4);
            gc.fillRect(cx + 6, cy - 4, cw - 11, 4);
        }

        renderCoderCityBack(x, y, w, h);
        renderCoderCityFront(x, y, w, h);
    }

    private void renderCoderCityBack(int x, int y, int w, int h) {
        int base = y + h - 36;
        gc.setFill(Color.rgb(9, 20, 31, 0.96));
        for (int i = 0; i < 20; i++) {
            int bx = x + i * 15 - 5;
            int bh = 17 + Math.floorMod(i * 29, 42);
            int bw = 12 + Math.floorMod(i * 7, 7);
            gc.fillRect(bx, base - bh, bw, bh);
        }
    }

    private void renderCoderCityFront(int x, int y, int w, int h) {
        int base = y + h;
        gc.setFill(Color.rgb(4, 11, 18));
        for (int i = 0; i < 11; i++) {
            int bx = x + i * 28 - 4;
            int bh = 25 + Math.floorMod(i * 31, 58);
            int bw = 25 + (i % 2) * 4;
            gc.fillRect(bx, base - bh, bw, bh);
            // rooftop antenna
            if (i % 2 == 0) {
                gc.fillRect(bx + bw - 7, base - bh - 10, 1, 10);
            }

            for (int r = 0; r < 4; r++) {
                for (int c = 0; c < 2; c++) {
                    if ((i + r + c) % 3 == 0) {
                        double flicker = 0.55 + 0.45 * ((Math.sin(elapsed * 0.8 + i + r) + 1.0) * 0.5);
                        gc.setFill(Color.rgb(209, 160, 83, 0.22 * flicker));
                        gc.fillRect(bx + 5 + c * 9, base - bh + 8 + r * 11, 2, 3);
                        gc.setFill(Color.rgb(4, 11, 18));
                    }
                }
            }
        }

        // cyan neon sign
        double neon = 0.6 + 0.4 * ((Math.sin(elapsed * 1.6) + 1.0) * 0.5);
        gc.setFill(Color.rgb(48, 143, 172, 0.12 * neon));
        gc.fillRect(316, 160, 10, 39);
        gc.setFill(Color.rgb(89, 210, 224, 0.65 * neon));
        gc.fillRect(320, 164, 2, 31);
    }
    private void renderRain() {
        final int x = 223;
        final int y = 25;
        final int w = 277;
        final int h = 207;
        gc.save();
        gc.beginPath();
        gc.rect(x, y, w, h);
        gc.clip();
        for (RainDrop drop : rain) {
            int px = snap(x + drop.x * w);
            int py = snap(y + drop.y * h);
            gc.setStroke(Color.rgb(146, 188, 211, drop.opacity));
            gc.setLineWidth(1);
            gc.strokeLine(px, py, px - Math.max(1, drop.length / 5), py + drop.length);
        }
        gc.restore();
    }
    private void renderGlassDrops() {
        final int x = 223;
        final int y = 25;
        final int w = 277;
        final int h = 207;
        gc.save();
        gc.beginPath();
        gc.rect(x, y, w, h);
        gc.clip();
        for (GlassDrop drop : glassDrops) {
            int px = snap(x + drop.x * w);
            int py = snap(y + drop.y * h);

            gc.setFill(Color.rgb(188, 211, 224, drop.opacity));
            gc.fillRect(px, py, drop.size, drop.size + 2);

            gc.setFill(Color.rgb(60, 94, 110, drop.opacity * 0.42));
            gc.fillRect(px, py + drop.size + 2, 1, 3);
        }

        gc.restore();
    }

    private void renderCodeReflection() {
        gc.save();
        gc.beginPath();
        gc.rect(223, 25, 277, 207);
        gc.clip();
        gc.setFont(Font.font("Consolas", 5.5));
        for (int i = 0; i < codeGlyphs.size(); i++) {
            CodeGlyph glyph = codeGlyphs.get(i);

            int gx = snap(228 + glyph.x * 190);
            int gy = snap(30 + glyph.y * 197);
            double fadeRight = 1.0 - Math.max(0.0, (gx - 385) / 120.0);
            double alpha = glyph.opacity * Math.max(0.0, fadeRight);
            gc.setFill(Color.rgb(105, 189, 190, alpha));
            gc.fillText(glyph.text, gx, gy);
        }

        // Big faint code line reflection.
        gc.setFont(Font.font("Consolas", 7));
        gc.setFill(Color.rgb(112, 176, 179, 0.065));
        gc.fillText("world.changeState(GameState.MAIN_MENU);", 240, 73);
        gc.fillText("portal.sync(knightRealm);", 252, 98);

        gc.restore();
    }
    private void renderDeskForeground() {
        int deskY = 239;

        gc.setFill(Color.rgb(7, 7, 9));
        gc.fillRect(0, deskY, VW, VH - deskY);

        gc.setFill(Color.rgb(46, 31, 23));
        gc.fillRect(0, deskY, VW, 7);

        gc.setFill(Color.rgb(92, 59, 36, 0.30));
        gc.fillRect(0, deskY, VW, 1);

        // keyboard / notebook
        gc.setFill(Color.rgb(13, 17, 22));
        gc.fillRect(360, 252, 88, 15);

        gc.setFill(Color.rgb(56, 66, 74, 0.30));
        gc.fillRect(363, 254, 82, 1);

        // coffee cup
        gc.setFill(Color.rgb(24, 18, 16));
        gc.fillRect(462, 246, 20, 24);
        gc.fillRect(481, 251, 6, 11);

        gc.setFill(Color.rgb(108, 67, 39, 0.38));
        gc.fillRect(464, 248, 16, 2);

        renderSteam();
    }

    private void renderSteam() {
        double phase = elapsed * 0.60;

        for (int i = 0; i < 3; i++) {
            double t = (phase + i * 0.33) % 1.0;
            int sx = 468 + i * 4 + snap(Math.sin(elapsed * 1.3 + i) * 2);
            int sy = 244 - snap(t * 20);

            gc.setFill(Color.rgb(169, 183, 188, (1.0 - t) * 0.09));
            gc.fillRect(sx, sy, 2, 3);
        }
    }

    private void renderWarmRoomLight() {
        double pulse = 0.78 + portalPulse * 0.22;

        gc.setFill(Color.rgb(220, 139, 67, 0.027 * pulse));
        gc.fillOval(419, 170, 126, 124);

        gc.setFill(Color.rgb(237, 173, 92, 0.075 * pulse));
        gc.fillRect(504, 185, 4, 28);

        gc.setFill(Color.rgb(255, 204, 116, 0.40 * pulse));
        gc.fillRect(507, 191, 2, 16);
    }

    private void renderLightningReveal() {
        if (lightningLife <= 0) return;
        double alpha = Math.min(1.0, lightningLife / 0.14);
        gc.setFill(Color.rgb(145, 175, 215, 0.08 * alpha));
        gc.fillRect(223, 25, 277, 207);
        gc.setStroke(Color.rgb(205, 226, 244, 0.62 * alpha));
        gc.setLineWidth(1);
        gc.strokeLine(468, 36, 460, 50);
        gc.strokeLine(460, 50, 466, 50);
        gc.strokeLine(466, 50, 455, 70);
        gc.setFill(Color.rgb(158, 184, 211, 0.030 * alpha));
        gc.fillRect(0, 0, VW, VH);
    }

    private void renderVignette() {
        LinearGradient left = new LinearGradient(
                0, 0, 1, 0,
                true,
                CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.rgb(0, 2, 7, 0.60)),
                new Stop(0.34, Color.rgb(0, 2, 7, 0.36)),
                new Stop(0.63, Color.rgb(0, 2, 7, 0.06)),
                new Stop(1.0, Color.TRANSPARENT)
        );
        gc.setFill(left);
        gc.fillRect(0, 0, VW, VH);
        gc.setFill(Color.rgb(0, 0, 0, 0.22));
        gc.fillRect(0, 0, VW, 10);
        gc.fillRect(0, VH - 15, VW, 15);
        gc.fillRect(0, 0, 8, VH);
        gc.fillRect(VW - 8, 0, 8, VH);
    }

    private int snap(double value) {
        return (int) Math.round(value);
    }

    private static final class RainDrop {
        private double x;
        private double y;
        private final double speed;
        private final int length;
        private final double opacity;

        private RainDrop(
                double x,
                double y,
                double speed,
                int length,
                double opacity
        ) {
            this.x = x;
            this.y = y;
            this.speed = speed;
            this.length = length;
            this.opacity = opacity;
        }
    }

    private static final class GlassDrop {
        private double x;
        private double y;
        private final int size;
        private final double speed;
        private final double opacity;

        private GlassDrop(
                double x,
                double y,
                int size,
                double speed,
                double opacity
        ) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.speed = speed;
            this.opacity = opacity;
        }
    }

    private static final class CodeGlyph {
        private double x;
        private double y;
        private final String text;
        private final double opacity;
        private final double speed;

        private CodeGlyph(
                double x,
                double y,
                String text,
                double opacity,
                double speed
        ) {
            this.x = x;
            this.y = y;
            this.text = text;
            this.opacity = opacity;
            this.speed = speed;
        }
    }

    private static final class Star {
        private final double x;
        private final double y;
        private final int size;
        private final double opacity;

        private Star(
                double x,
                double y,
                int size,
                double opacity
        ) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.opacity = opacity;
        }
    }
}