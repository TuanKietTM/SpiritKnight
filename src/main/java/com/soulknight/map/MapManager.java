package com.soulknight.map;

import com.soulknight.engine.Camera;
import com.soulknight.utils.Constants;
import com.soulknight.utils.Vector2D;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public final class MapManager {

    private final Tile[][] tiles;
    private int width;
    private int height;
    private final int tileSize;
    private Vector2D exitPortalPosition;
    private boolean exitPortalOpen;
    //(vitdung) thêm biến để viết hàm
    private Vector2D spawnPoint;
    private Vector2D bossSpawnPoint;
    private final List<Vector2D> enemySpawnPoints = new ArrayList<>();


    public MapManager(int width, int height, int tileSize, Random random) {
        this.width = width;
        this.height = height;
        this.tileSize = tileSize;
        this.tiles = new DungeonGenerator().generate(width, height, random);
    }
    //(vitdung) viết constructor này để cho GameWorld load map lên
    public MapManager(String mapResourcePath, int tileSize) {
        this.tileSize = tileSize;
        this.exitPortalOpen = false;
        this.tiles = loadMapFromResource(mapResourcePath);
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
        if (this.spawnPoint != null) {
            return this.spawnPoint;
        } else {
            return new Vector2D((width / 2.0 + 0.5) * tileSize, (height / 2.0 + 0.5) * tileSize);
        }
    }

    public Vector2D getBossSpawnPoint() {
        if (this.bossSpawnPoint != null) {
            return this.bossSpawnPoint;
        } else {
            return new Vector2D((width - 3.5) * tileSize, (height - 3.5) * tileSize);
        }
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

    //(vitdung) code tạo hàm để load map từ file txt
    private Tile[][] loadMapFromResource(String resourcePath) {
        List<String> lines = new ArrayList<>();

        try (InputStream is = getClass().getResourceAsStream(resourcePath);
             InputStreamReader isr = new InputStreamReader(is);
             BufferedReader br = new BufferedReader(isr)) {

            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    lines.add(line);
                }
            }
        } catch (Exception e) {
            System.err.println("Không thể đọc file map: " + resourcePath);
            e.printStackTrace();
        }

        int mapHeight = lines.size();
        int mapWidth = lines.get(0).trim().split("\\s+").length;
        this.height = mapHeight;
        this.width = mapWidth;

        Tile[][] loadedTiles = new Tile[height][width];

        for (int y = 0; y < mapHeight; y ++) {
            String[] tokens = lines.get(y).trim().split("\\s+"); // mảng tokens chứa các dòng map
            for(int x = 0; x < mapWidth; x++ ) { // vòng for xử lí từng dòng map
                String token = tokens[x];
                // tính vị trí tâm của ô gạch
                double pixelX = (x + 0.5) * tileSize;
                double pixelY = (y + 0.5) * tileSize;

                switch(token) {
                    case "W":
                        loadedTiles[y][x] = new Tile(Tile.TileType.WALL);
                        break;
                    case ".":
                        loadedTiles[y][x] = new Tile(Tile.TileType.FLOOR);
                        break;
                    case "D":
                        loadedTiles[y][x] = new Tile(Tile.TileType.DOOR);
                        break;
                    case "P": // vị trí người chơi
                        loadedTiles[y][x] = new Tile(Tile.TileType.FLOOR);
                        this.spawnPoint = new Vector2D(pixelX, pixelY);
                        break;
                    case "B": // vị trí boss
                        loadedTiles[y][x] = new Tile(Tile.TileType.FLOOR);
                        this.bossSpawnPoint = new Vector2D(pixelX, pixelY);
                        break;
                    case "M": // vị trí của portal
                        loadedTiles[y][x] = new Tile(Tile.TileType.PORTAL);
                        this.exitPortalPosition = new Vector2D(pixelX, pixelY);
                        break;
                    case "E": // load enemy trong txt
                        loadedTiles[y][x] = new Tile(Tile.TileType.FLOOR);
                        this.enemySpawnPoints.add(new Vector2D(pixelX, pixelY));
                    default:
                        loadedTiles[y][x] = new Tile(Tile.TileType.FLOOR);
                        break;
                }
            }
        }
        return loadedTiles;
    }

    public List<Vector2D> getEnemySpawnPoints() {
        return this.enemySpawnPoints;
    }
}
