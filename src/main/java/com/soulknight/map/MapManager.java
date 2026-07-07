package com.soulknight.map;

import com.soulknight.engine.Camera;
import com.soulknight.utils.Constants;
import com.soulknight.utils.Vector2D;
import java.util.Random;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public final class MapManager {

    private final Tile[][] tiles;
    private final int width;
    private final int height;
    private final int tileSize;
    private Vector2D exitPortalPosition;
    private boolean exitPortalOpen;

    public MapManager(int width, int height, int tileSize, Random random) {
        this.width = width;
        this.height = height;
        this.tileSize = tileSize;
        this.tiles = new DungeonGenerator().generate(width, height, random);
    }

    public void render(GraphicsContext graphicsContext, Camera camera, double renderWidth, double renderHeight) {
        graphicsContext.setFill(Color.BLACK);
        graphicsContext.fillRect(0.0, 0.0, renderWidth, renderHeight);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Tile tile = tiles[y][x];
                double worldX = x * tileSize;
                double worldY = y * tileSize;
                double screenX = camera.worldToScreenX(worldX);
                double screenY = camera.worldToScreenY(worldY);

                graphicsContext.setFill(tile.getFillColor());
                graphicsContext.fillRect(screenX, screenY, tileSize, tileSize);
            }
        }

        if (exitPortalOpen && exitPortalPosition != null) {
            double screenX = camera.worldToScreenX(exitPortalPosition.getX());
            double screenY = camera.worldToScreenY(exitPortalPosition.getY());
            graphicsContext.setFill(Color.GOLD);
            graphicsContext.fillOval(screenX - 18.0, screenY - 18.0, 36.0, 36.0);
            graphicsContext.setStroke(Color.WHITE);
            graphicsContext.setLineWidth(2.0);
            graphicsContext.strokeOval(screenX - 18.0, screenY - 18.0, 36.0, 36.0);
        }
    }

    public boolean isWalkable(double worldX, double worldY, double radius) {
        double left = worldX - radius;
        double right = worldX + radius;
        double top = worldY - radius;
        double bottom = worldY + radius;
        return isWalkablePoint(left, top) && isWalkablePoint(right, top) && isWalkablePoint(left, bottom)
                && isWalkablePoint(right, bottom) && isWalkablePoint(worldX, worldY);
    }

    public Vector2D getSpawnPoint() {
        return new Vector2D((width / 2.0 + 0.5) * tileSize, (height / 2.0 + 0.5) * tileSize);
    }

    public Vector2D getBossSpawnPoint() {
        return new Vector2D((width - 3.5) * tileSize, (height - 3.5) * tileSize);
    }

    public void openExitPortal(Vector2D position) {
        this.exitPortalPosition = position;
        this.exitPortalOpen = true;
    }

    public void closeExitPortal() {
        this.exitPortalOpen = false;
        this.exitPortalPosition = null;
    }

    public boolean isExitPortalOpen() {
        return exitPortalOpen;
    }

    public boolean isPlayerInsideExitPortal(Vector2D position) {
        return exitPortalOpen && exitPortalPosition != null && position.distance(exitPortalPosition) <= Constants.PORTAL_RADIUS;
    }

    public Vector2D getExitPortalPosition() {
        return exitPortalPosition;
    }

    public Vector2D findRandomWalkablePosition(Random random, double margin) {
        for (int attempt = 0; attempt < 200; attempt++) {
            int x = 2 + random.nextInt(Math.max(1, width - 4));
            int y = 2 + random.nextInt(Math.max(1, height - 4));
            double worldX = (x + 0.5) * tileSize;
            double worldY = (y + 0.5) * tileSize;
            if (isWalkable(worldX, worldY, margin)) {
                return new Vector2D(worldX, worldY);
            }
        }
        return null;
    }

    public double getWorldWidth() {
        return width * (double) tileSize;
    }

    public double getWorldHeight() {
        return height * (double) tileSize;
    }

    public int getTileSize() {
        return tileSize;
    }

    private boolean isWalkablePoint(double worldX, double worldY) {
        int tileX = (int) Math.floor(worldX / tileSize);
        int tileY = (int) Math.floor(worldY / tileSize);
        if (tileX < 0 || tileY < 0 || tileX >= width || tileY >= height) {
            return false;
        }
        return tiles[tileY][tileX].isWalkable();
    }
}
