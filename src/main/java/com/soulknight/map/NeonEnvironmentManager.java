package com.soulknight.map;

import com.soulknight.engine.Camera;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Quan ly rieng fog, neon particle, hologram va tru dien.
 * Toan bo cong thuc va hieu ung duoc giu nguyen tu MapManager cu.
 */
public final class NeonEnvironmentManager {

    private final List<FogWisp> fogWisps = new ArrayList<>();
    private final List<NeonParticle> neonParticles = new ArrayList<>();
    private final List<Hologram> holograms = new ArrayList<>();
    private final List<ElectricPillar> electricPillars = new ArrayList<>();
    private final List<GroundGlow> groundGlows = new ArrayList<>();

    public void initializeDynamicEnvironment(
            Tile[][] tiles,
            int[][] tileMatrix,
            int width,
            int height,
            int tileSize
    ) {
        fogWisps.clear();
        neonParticles.clear();
        holograms.clear();
        electricPillars.clear();
        groundGlows.clear();

        Random random = new Random(20260802L);
        double worldWidth = width * (double) tileSize;
        double worldHeight = height * (double) tileSize;

        // Lop suong mong lon, chuyen dong rat cham.
        for (int i = 0; i < 18; i++) {
            fogWisps.add(new FogWisp(
                    random.nextDouble() * worldWidth,
                    random.nextDouble() * worldHeight,
                    120.0 + random.nextDouble() * 220.0,
                    24.0 + random.nextDouble() * 45.0,
                    4.0 + random.nextDouble() * 8.0,
                    random.nextDouble() * Math.PI * 2.0
            ));
        }

        // Cac hat sang neon rat nho, chi xuat hien tren vung FLOOR.
        for (int i = 0; i < 90; i++) {
            int gridX = random.nextInt(Math.max(1, width));
            int gridY = random.nextInt(Math.max(1, height));
            Tile tile = tiles[gridY][gridX];

            if (tile == null || tile.getType() != Tile.TileType.FLOOR) {
                continue;
            }

            neonParticles.add(new NeonParticle(
                    (gridX + random.nextDouble()) * tileSize,
                    (gridY + random.nextDouble()) * tileSize,
                    1.0 + random.nextDouble() * 2.0,
                    5.0 + random.nextDouble() * 10.0,
                    random.nextDouble() * Math.PI * 2.0,
                    random.nextBoolean()
            ));
        }

        // Hologram hiem, khong dat tren moi tile de tranh roi mat.
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                Tile tile = tiles[y][x];
                if (tile == null || tile.getType() != Tile.TileType.FLOOR) {
                    continue;
                }

                long hash = x * 73856093L ^ y * 19349663L;
                int value = (int) Math.floorMod(hash, 1000);

                if (value < 12) {
                    holograms.add(new Hologram(
                            (x + 0.5) * tileSize,
                            (y + 0.62) * tileSize,
                            tileSize * 0.42,
                            value % 2 == 0,
                            (x + y) * 0.7
                    ));
                }

                /*
                 * Khoang 5% tile floor co ground glow.
                 * Vi tri duoc tao theo toa do nen luon co dinh, khong nhap nhay.
                 */
                int glowValue = (int) Math.floorMod(
                        x * 83492791L ^ y * 2971215073L,
                        1000
                );

                if (glowValue < 50) {
                    groundGlows.add(new GroundGlow(
                            x * tileSize,
                            y * tileSize,
                            tileSize,
                            glowValue % 2 == 0,
                            (x * 0.83 + y * 1.17)
                    ));
                }

                if (tileMatrix != null && tileMatrix[y][x] >= 45) {
                    electricPillars.add(new ElectricPillar(
                            (x + 0.5) * tileSize,
                            (y + 0.5) * tileSize,
                            tileSize * 0.9,
                            x * 17.0 + y * 31.0
                    ));
                }
            }
        }
    }

    public void renderGroundGlows(GraphicsContext gc, Camera camera, double renderWidth, double renderHeight, double time) {
        double zoom = camera.getZoom();

        gc.save();

        for (GroundGlow glow : groundGlows) {
            double screenX = camera.worldToScreenX(glow.worldX);
            double screenY = camera.worldToScreenY(glow.worldY);
            double drawSize = glow.size * zoom;

            if (!isInsideScreen(screenX, screenY, drawSize, drawSize, renderWidth, renderHeight)) {
                continue;
            }

            /*
             * Moi tile co phase rieng de khong phat sang cung luc.
             * Alpha duoc giu nhe de khong lam roi gameplay.
             */
            double pulse = 0.5 + Math.sin(time * 1.35 + glow.phase) * 0.5;

            double coreAlpha = 0.035 + pulse * 0.055;
            double borderAlpha = 0.08 + pulse * 0.12;

            Color color = glow.cyan ? Color.rgb(0, 238, 255) : Color.rgb(205, 62, 255);

            // Vung sang mem o giua tile.
            gc.setGlobalAlpha(coreAlpha);
            gc.setFill(color);
            gc.fillOval(
                    screenX + drawSize * 0.16,
                    screenY + drawSize * 0.16,
                    drawSize * 0.68,
                    drawSize * 0.68
            );

            // Vien pixel neon nhe, bam theo kich thuoc tile.
            gc.setGlobalAlpha(borderAlpha);
            gc.setStroke(color);
            gc.setLineWidth(Math.max(1.0, zoom));
            gc.strokeRect(
                    Math.floor(screenX + drawSize * 0.08),
                    Math.floor(screenY + drawSize * 0.08),
                    Math.ceil(drawSize * 0.84),
                    Math.ceil(drawSize * 0.84)
            );

            // Loi sang nho de tao cam giac nang luong dang chay.
            gc.setGlobalAlpha(0.10 + pulse * 0.16);
            gc.setFill(color);
            double coreSize = Math.max(1.0, 2.0 * zoom);
            gc.fillRect(
                    Math.floor(screenX + drawSize * 0.5 - coreSize * 0.5),
                    Math.floor(screenY + drawSize * 0.5 - coreSize * 0.5),
                    coreSize,
                    coreSize
            );
        }

        gc.restore();
    }

    public void renderFog(GraphicsContext gc, Camera camera, double renderWidth, double renderHeight, double time) {
        double zoom = camera.getZoom();

        gc.save();
        for (FogWisp fog : fogWisps) {
            double driftX = Math.sin(time * 0.10 + fog.phase) * fog.driftRadius;
            double driftY = Math.cos(time * 0.07 + fog.phase) * 7.0;
            double screenX = camera.worldToScreenX(fog.worldX + driftX);
            double screenY = camera.worldToScreenY(fog.worldY + driftY);
            double drawWidth = fog.width * zoom;
            double drawHeight = fog.height * zoom;

            if (!isInsideScreen(screenX, screenY, drawWidth, drawHeight, renderWidth, renderHeight)) {
                continue;
            }

            double pulse = 0.035 + (Math.sin(time * 0.35 + fog.phase) + 1.0) * 0.012;
            gc.setGlobalAlpha(pulse);
            gc.setFill(Color.rgb(110, 150, 190));
            gc.fillOval(screenX, screenY, drawWidth, drawHeight);
        }
        gc.restore();
    }

    public void renderNeonParticles(GraphicsContext gc, Camera camera, double renderWidth, double renderHeight, double time) {
        double zoom = camera.getZoom();

        gc.save();
        for (NeonParticle particle : neonParticles) {
            double floatY = Math.sin(time * 0.9 + particle.phase) * particle.floatRange;
            double floatX = Math.cos(time * 0.45 + particle.phase) * 3.0;
            double screenX = camera.worldToScreenX(particle.worldX + floatX);
            double screenY = camera.worldToScreenY(particle.worldY + floatY);
            double size = particle.size * zoom;

            if (!isInsideScreen(screenX, screenY, size * 2.0, size * 2.0, renderWidth, renderHeight)) {
                continue;
            }

            double alpha = 0.25 + (Math.sin(time * 2.0 + particle.phase) + 1.0) * 0.18;
            Color color = particle.cyan ? Color.rgb(0, 238, 255) : Color.rgb(205, 62, 255);

            gc.setGlobalAlpha(alpha * 0.35);
            gc.setFill(color);
            gc.fillOval(screenX - size, screenY - size, size * 3.0, size * 3.0);

            gc.setGlobalAlpha(alpha);
            gc.fillRect(Math.floor(screenX), Math.floor(screenY), Math.max(1.0, size), Math.max(1.0, size));
        }
        gc.restore();
    }

    public void renderHolograms(GraphicsContext gc, Camera camera, double renderWidth, double renderHeight, double time) {
        double zoom = camera.getZoom();

        gc.save();
        for (Hologram hologram : holograms) {
            double flicker = 0.42 + Math.sin(time * 3.4 + hologram.phase) * 0.14;
            double bob = Math.sin(time * 1.1 + hologram.phase) * 4.0;
            double size = hologram.size * zoom;
            double screenX = camera.worldToScreenX(hologram.worldX);
            double screenY = camera.worldToScreenY(hologram.worldY + bob) - size;

            if (!isInsideScreen(screenX - size, screenY - size, size * 2.0, size * 2.0, renderWidth, renderHeight)) {
                continue;
            }

            Color color = hologram.cyan ? Color.rgb(0, 238, 255) : Color.rgb(205, 62, 255);
            gc.setStroke(color);
            gc.setLineWidth(Math.max(1.0, zoom));
            gc.setGlobalAlpha(Math.max(0.18, flicker));

            double half = size * 0.5;
            gc.strokePolygon(
                    new double[]{screenX, screenX + half, screenX, screenX - half},
                    new double[]{screenY - half, screenY, screenY + half, screenY},
                    4
            );

            gc.setGlobalAlpha(flicker * 0.45);
            for (int line = -2; line <= 2; line++) {
                double lineY = screenY + line * size * 0.16;
                gc.strokeLine(screenX - half * 0.65, lineY, screenX + half * 0.65, lineY);
            }
        }
        gc.restore();
    }

    public void renderElectricPillars(GraphicsContext gc, Camera camera, double renderWidth, double renderHeight, double time) {
        double zoom = camera.getZoom();

        for (ElectricPillar pillar : electricPillars) {
            double size = pillar.size * zoom;
            double screenX = camera.worldToScreenX(pillar.worldX);
            double screenY = camera.worldToScreenY(pillar.worldY);

            if (!isInsideScreen(screenX - size, screenY - size, size * 2.0, size * 2.0, renderWidth, renderHeight)) {
                continue;
            }

            gc.save();

            // Bong va chan tru.
            gc.setGlobalAlpha(0.22);
            gc.setFill(Color.BLACK);
            gc.fillOval(screenX - size * 0.34, screenY + size * 0.22, size * 0.68, size * 0.22);

            gc.setGlobalAlpha(1.0);
            gc.setFill(Color.rgb(18, 24, 46));
            gc.fillRect(screenX - size * 0.22, screenY - size * 0.24, size * 0.44, size * 0.62);
            gc.setFill(Color.rgb(0, 238, 255, 0.85));
            gc.fillRect(screenX - size * 0.08, screenY - size * 0.32, size * 0.16, size * 0.42);

            // Vung sang quanh loi dien.
            double pulse = 0.28 + (Math.sin(time * 4.0 + pillar.phase) + 1.0) * 0.10;
            gc.setGlobalAlpha(pulse);
            gc.setFill(Color.rgb(0, 238, 255));
            gc.fillOval(screenX - size * 0.33, screenY - size * 0.58, size * 0.66, size * 0.66);

            // Tia set thay doi theo tung khoang thoi gian ngan.
            gc.setGlobalAlpha(0.85);
            gc.setStroke(Color.rgb(190, 245, 255));
            gc.setLineWidth(Math.max(1.0, 1.4 * zoom));
            renderLightningBolt(gc, screenX, screenY - size * 0.52, screenX, screenY + size * 0.02,
                    pillar.phase + Math.floor(time * 12.0), size * 0.16);

            gc.restore();
        }
    }

    private void renderLightningBolt(GraphicsContext gc, double startX, double startY,
                                     double endX, double endY, double seed, double spread) {
        int segments = 6;
        double previousX = startX;
        double previousY = startY;

        for (int i = 1; i <= segments; i++) {
            double progress = i / (double) segments;
            double nextX = startX + (endX - startX) * progress;
            double nextY = startY + (endY - startY) * progress;

            if (i < segments) {
                double noise = Math.sin(seed * 12.9898 + i * 78.233) * 43758.5453;
                noise = noise - Math.floor(noise);
                nextX += (noise - 0.5) * spread;
            }

            gc.strokeLine(previousX, previousY, nextX, nextY);
            previousX = nextX;
            previousY = nextY;
        }
    }
//    mot so vien sang nho o giua tile, khong nhap nhay, de tao cam giac nang luong chay quanh tile.

    private static final class GroundGlow {
        private final double worldX;
        private final double worldY;
        private final double size;
        private final boolean cyan;
        private final double phase;

        private GroundGlow(
                double worldX,
                double worldY,
                double size,
                boolean cyan,
                double phase
        ) {
            this.worldX = worldX;
            this.worldY = worldY;
            this.size = size;
            this.cyan = cyan;
            this.phase = phase;
        }
    }

    private static final class FogWisp {
        private final double worldX;
        private final double worldY;
        private final double width;
        private final double height;
        private final double driftRadius;
        private final double phase;

        private FogWisp(double worldX, double worldY, double width, double height,
                        double driftRadius, double phase) {
            this.worldX = worldX;
            this.worldY = worldY;
            this.width = width;
            this.height = height;
            this.driftRadius = driftRadius;
            this.phase = phase;
        }
    }

    private static final class NeonParticle {
        private final double worldX;
        private final double worldY;
        private final double size;
        private final double floatRange;
        private final double phase;
        private final boolean cyan;

        private NeonParticle(double worldX, double worldY, double size,
                             double floatRange, double phase, boolean cyan) {
            this.worldX = worldX;
            this.worldY = worldY;
            this.size = size;
            this.floatRange = floatRange;
            this.phase = phase;
            this.cyan = cyan;
        }
    }

    private static final class Hologram {
        private final double worldX;
        private final double worldY;
        private final double size;
        private final boolean cyan;
        private final double phase;

        private Hologram(double worldX, double worldY, double size, boolean cyan, double phase) {
            this.worldX = worldX;
            this.worldY = worldY;
            this.size = size;
            this.cyan = cyan;
            this.phase = phase;
        }
    }

    private static final class ElectricPillar {
        private final double worldX;
        private final double worldY;
        private final double size;
        private final double phase;

        private ElectricPillar(double worldX, double worldY, double size, double phase) {
            this.worldX = worldX;
            this.worldY = worldY;
            this.size = size;
            this.phase = phase;
        }
    }


    private boolean isInsideScreen(double screenX, double screenY, double width, double height, double renderWidth, double renderHeight) {
        return screenX + width >= 0.0
                && screenY + height >= 0.0
                && screenX <= renderWidth
                && screenY <= renderHeight;
    }

}