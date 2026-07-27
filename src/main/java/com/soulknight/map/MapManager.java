package com.soulknight.map;

import com.soulknight.engine.Camera;
import com.soulknight.utils.Constants;
import com.soulknight.utils.Vector2D;
import com.soulknight.map.json.*;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class MapManager {

    private Tile[][] tiles;
    private int width;
    private int height;
    private int tileSize;
    private Vector2D exitPortalPosition;
    private boolean exitPortalOpen;
    private Vector2D spawnPoint;
    private Vector2D bossSpawnPoint;
    private final List<Vector2D> enemySpawnPoints = new ArrayList<>();
    private int[][] tileMatrix;
    private final List<Room> rooms = new ArrayList<>();
    private double playerSpawnX, playerSpawnY;

    public MapManager(int width, int height, int tileSize, Random random) {
        this.width = width;
        this.height = height;
        this.tileSize = tileSize;
        this.tiles = new DungeonGenerator().generate(width, height, random);
    }
    public MapManager(String JsonPath) {
        loadMapFromJson(JsonPath);
        generateWorldTiles();
        linkDoorsToRooms();
    }

    /**
     * floor , wall , door
     */
    public void renderFloor(GraphicsContext gc, Camera camera, double renderWidth, double renderHeight) {
        gc.setFill(Color.BLACK);
        gc.fillRect(0.0, 0.0, renderWidth, renderHeight);

        double zoom = camera.getZoom();

        // Layer 1 ve san va cua mo
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Tile tile = tiles[y][x];
                if (tile != null && tile.getType() != Tile.TileType.WALL) {
                    double screenX = camera.worldToScreenX(x * tileSize);
                    double screenY = camera.worldToScreenY(y * tileSize);
                    gc.drawImage(tile.getTexture(), screenX, screenY, tileSize * zoom, tileSize * zoom);
                }
            }
        }

        // ve do bong len chan tuong
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Tile tile = tiles[y][x];
                if (tile != null && tile.getType() == Tile.TileType.WALL) {
                    boolean isWallBelow = (y + 1 < height) &&
                            tiles[y + 1][x] != null &&
                            tiles[y + 1][x].getType() == Tile.TileType.WALL;
                    if (!isWallBelow) {
                        double screenX = camera.worldToScreenX(x * tileSize);
                        double screenY = camera.worldToScreenY((y + 1) * tileSize);
                        gc.drawImage(Tile.getWallFrontShadowImage(), screenX, screenY, tileSize * zoom, tileSize * zoom);
                    }
                }
            }
        }

        // Layer 3 Ve mat dinh cua tuong
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Tile tile = tiles[y][x];
                if (tile != null && tile.getType() == Tile.TileType.WALL) {
                    double screenX = camera.worldToScreenX(x * tileSize);
                    double screenY = camera.worldToScreenY(y * tileSize);
                    gc.drawImage(Tile.getWallTopImage(), screenX, screenY, tileSize * zoom, tileSize * zoom);
                }
            }
        }

        // Layer 4 ve portal va cua phong khi dong
        if (exitPortalOpen && exitPortalPosition != null) {
            double screenX = camera.worldToScreenX(exitPortalPosition.getX());
            double screenY = camera.worldToScreenY(exitPortalPosition.getY());
            gc.setFill(Color.GOLD);
            gc.fillOval(screenX - 18.0, screenY - 18.0, 36.0, 36.0);
        }

        for (Room room : rooms) {
            room.renderDoors(gc, camera, tileSize);
        }
    }

    public List<Tile> getWallTiles() {
        List<Tile> wallList = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Tile tile = tiles[y][x];
                if (tile != null && tile.getType() == Tile.TileType.WALL) {
                    wallList.add(tile);
                }
            }
        }
        return wallList;
    }

    /**
     * Kiem tra va cham va di chuyen
     */
    public boolean isWalkable(double worldX, double worldY, double radius) {
        // Va cham cua dang dong voi cac phong dang chien dau
        for (Room room : rooms) {
            if (room.isHitClosedDoor(worldX, worldY, radius)) {
                return false;
            }
        }

        // Va cham coi tuong tren map
        double footY = worldY + 6.0;
        double footRadius = radius * 0.6;

        int minTileX = (int) Math.floor((worldX - footRadius) / tileSize);
        int maxTileX = (int) Math.floor((worldX + footRadius) / tileSize);
        int minTileY = (int) Math.floor((footY - footRadius) / tileSize);
        int maxTileY = (int) Math.floor((footY + footRadius) / tileSize);

        for (int ty = minTileY; ty <= maxTileY; ty++) {
            for (int tx = minTileX; tx <= maxTileX; tx++) {
                if (tx < 0 || ty < 0 || tx >= width || ty >= height) {
                    if (isCircleCollidingWithTile(worldX, footY, footRadius, tx, ty)) return false;
                    continue;
                }

                Tile tile = tiles[ty][tx];
                if (tile != null && !tile.isWalkable()) {
                    if (isCircleCollidingWithTile(worldX, footY, footRadius, tx, ty)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private boolean isCircleCollidingWithTile(double cx, double cy, double radius, int tx, int ty) {
        double tileLeft = tx * tileSize;
        double tileRight = (tx + 1) * tileSize;
        double tileTop = ty * tileSize;
        double tileBottom = (ty + 1) * tileSize;

        boolean isBottomWall = (ty > 0 && tiles[ty - 1][tx] != null && tiles[ty - 1][tx].getType() == Tile.TileType.FLOOR);

        if (isBottomWall) {
            tileTop += (tileSize * 0.5);
        }

        double closestX = Math.max(tileLeft, Math.min(cx, tileRight));
        double closestY = Math.max(tileTop, Math.min(cy, tileBottom));

        double distX = cx - closestX;
        double distY = cy - closestY;

        return (distX * distX + distY * distY) < (radius * radius);
    }

    /**
     * tai cau tru ctu json
     */
    private void loadMapFromJson(String path) {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) {
                System.err.println("Không thấy file json: " + path);
                return;
            }

            ObjectMapper mapper = new ObjectMapper();
            TiledMapData mapData = mapper.readValue(is, TiledMapData.class);

            this.width = mapData.width;
            this.height = mapData.height;
            this.tileSize = mapData.tilewidth;
            this.tileMatrix = new int[this.height][this.width];

            for (LayerData layer : mapData.layers) {
                if ("tilelayer".equals(layer.type) && layer.data != null) {
                    for (int i = 0; i < layer.data.size(); i++) {
                        int tileVal = layer.data.get(i);
                        if (tileVal > 0) {
                            int y = i / width;
                            int x = i % width;
                            this.tileMatrix[y][x] = tileVal;
                        }
                    }
                } else if ("objectgroup".equals(layer.type) && layer.objects != null) {
                    for (ObjectData obj : layer.objects) {
                        String type = obj.type != null ? obj.type.trim() : "";
                        String name = obj.name != null ? obj.name.trim() : "";

                        if ("playerSpawn".equalsIgnoreCase(name) || "SpawnPoint".equalsIgnoreCase(type) || "Spawn".equalsIgnoreCase(type)) {
                            this.playerSpawnX = obj.x;
                            this.playerSpawnY = obj.y;
                            this.spawnPoint = new Vector2D(obj.x, obj.y);
                        } else if ("Room".equalsIgnoreCase(type) || name.toLowerCase().contains("room")) {
                            Room room = new Room(name, obj.x, obj.y, obj.width, obj.height);
                            rooms.add(room);
                        }
                    }
                }
            }

            System.out.println("=== LOAD MAP THÀNH CÔNG ===");
            System.out.println("-> Kích thước : " + width + "x" + height + " (TileSize: " + tileSize + "px)");
            System.out.println("-> Player Spawn : " + (spawnPoint != null ? spawnPoint : "CHƯA BẮT ĐƯỢC!"));
            System.out.println("-> Số Phòng     : " + rooms.size());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Khoi toa tu grid cua tile map
     */
    private void generateWorldTiles() {
        this.tiles = new Tile[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int tileId = tileMatrix[y][x];

                double pixelX = x * tileSize;
                double pixelY = y * tileSize;

                if (tileId == 0) {
                    this.tiles[y][x] = null;
                    continue;
                }

                // Nhận diện theo firstgid trong file JSON Tiled Map của bạn:
                // wall.tsx  -> 1 đến 6
                // door.tsx  -> 7 đến 10
                // floor.tsx -> 11 trở đi
                if (tileId >= 1 && tileId <= 6) {
                    this.tiles[y][x] = new Tile(pixelX, pixelY, tileSize, Tile.TileType.WALL);
                } else if (tileId >= 7 && tileId <= 10) {
                    this.tiles[y][x] = new Tile(pixelX, pixelY, tileSize, Tile.TileType.DOOR_OPEN);
                } else {
                    this.tiles[y][x] = new Tile(pixelX, pixelY, tileSize, Tile.TileType.FLOOR);
                }
            }
        }
    }

    /**
     * Gan toa do cua cua cho tung phong
     */
    private void linkDoorsToRooms() {
        for (Room room : rooms) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    Tile tile = tiles[y][x];
                    if (tile != null && tile.getType() == Tile.TileType.DOOR_OPEN) {
                        double doorWorldX = x * tileSize;
                        double doorWorldY = y * tileSize;

                        if (room.isDoorBelongsToRoom(doorWorldX, doorWorldY, tileSize, tileSize)) {
                            room.addDoorCoordinate(doorWorldX, doorWorldY, tileSize, tileSize);
                        }
                    }
                }
            }
        }
    }

    public void updateRoomLogic(Vector2D playerPos, int aliveEnemiesInCurrentRoom) {
        for (Room room : rooms) {
            if (room.getType() == Room.RoomType.START || room.getType() == Room.RoomType.REST) {
                continue;
            }

            if (room.getState() == Room.RoomState.NOT_STARTED) {
                if (room.getBound().contains(playerPos.getX(), playerPos.getY())) {
                    room.startBattle();
                }
            } else if (room.isDoorsClosed() && aliveEnemiesInCurrentRoom <= 0) {
                room.clearRoom();
            }
        }
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

    public Vector2D findRandomWalkablePositionInRoom(Room room, Random random, double enemyRadius) {
        javafx.geometry.BoundingBox bound = room.getBound();

        int minTileX = (int) Math.floor(bound.getMinX() / tileSize);
        int maxTileX = (int) Math.floor((bound.getMinX() + bound.getWidth()) / tileSize);
        int minTileY = (int) Math.floor(bound.getMinY() / tileSize);
        int maxTileY = (int) Math.floor((bound.getMinY() + bound.getHeight()) / tileSize);

        for (int attempt = 0; attempt < 100; attempt++) {
            double randomX = bound.getMinX() + random.nextDouble() * bound.getWidth();
            double randomY = bound.getMinY() + random.nextDouble() * bound.getHeight();

            if (isWalkable(randomX, randomY, enemyRadius)) {
                int tx = (int) Math.floor(randomX / tileSize);
                int ty = (int) Math.floor(randomY / tileSize);

                if (tx >= minTileX && tx <= maxTileX && ty >= minTileY && ty <= maxTileY) {
                    Tile tile = tiles[ty][tx];
                    if (tile != null && tile.isWalkable()) {
                        return new Vector2D(randomX, randomY);
                    }
                }
            }
        }
        return new Vector2D(bound.getMinX() + bound.getWidth() / 2.0, bound.getMinY() + bound.getHeight() / 2.0);
    }

    public boolean isBulletCollidingWithWall(double worldX, double worldY) {
        int tx = (int) Math.floor(worldX / tileSize);
        int ty = (int) Math.floor(worldY / tileSize);

        if (tx < 0 || ty < 0 || tx >= width || ty >= height) {
            return true;
        }

        Tile tile = tiles[ty][tx];
        if (tile == null || !tile.isWalkable()) {
            boolean isBottomWall = (ty > 0 && tiles[ty - 1][tx] != null && tiles[ty - 1][tx].getType() == Tile.TileType.FLOOR);

            double tileTop = ty * tileSize;
            if (isBottomWall) {
                tileTop += (tileSize * 0.5);
            }

            return worldY >= tileTop;
        }

        return false;
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

    public List<Vector2D> getEnemySpawnPoints() {
        return this.enemySpawnPoints;
    }

    public Tile[][] getTiles() {
        return tiles;
    }

    public double getPlayerSpawnX() {
        return playerSpawnX;
    }

    public double getPlayerSpawnY() {
        return playerSpawnY;
    }

    public List<Room> getRooms() {
        return rooms;
    }
}