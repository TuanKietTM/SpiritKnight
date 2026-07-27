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
    }

    // =========================================================================
    // HÀM RENDER TỰ ĐỘNG ĐỔ BÓNG 3 LỚP (RENDER OFFSET Y + 1)
    // =========================================================================
    public void renderFloor(GraphicsContext gc, Camera camera, double renderWidth, double renderHeight) {
        gc.setFill(Color.BLACK);
        gc.fillRect(0.0, 0.0, renderWidth, renderHeight);

        double zoom = camera.getZoom();

        // 1. LỚP SÀN (FLOOR)
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Tile tile = tiles[y][x];
                if (tile != null && tile.getType() == Tile.TileType.FLOOR) {
                    double screenX = camera.worldToScreenX(x * tileSize);
                    double screenY = camera.worldToScreenY(y * tileSize);
                    gc.drawImage(tile.getTexture(), screenX, screenY, tileSize * zoom, tileSize * zoom);
                }
            }
        }

        // 2. LỚP BÓNG ĐỔ & THÀNH TƯỜNG (Vẽ lệch xuống ô y + 1)
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Tile tile = tiles[y][x];
                if (tile != null && tile.getType() == Tile.TileType.WALL) {
                    // Kiểm tra ô phía dưới xem có phải tường không
                    boolean isWallBelow = (y + 1 < height) &&
                            tiles[y + 1][x] != null &&
                            tiles[y + 1][x].getType() == Tile.TileType.WALL;

                    // Nếu bên dưới KHÔNG là tường -> Đặt hiệu ứng bóng đổ & vạch cyan tràn xuống ô y + 1
                    if (!isWallBelow) {
                        double screenX = camera.worldToScreenX(x * tileSize);
                        double screenY = camera.worldToScreenY((y + 1) * tileSize);
                        gc.drawImage(Tile.getWallFrontShadowImage(), screenX, screenY, tileSize * zoom, tileSize * zoom);
                    }
                }
            }
        }

        // 3. LỚP BỀ MẶT TƯỜNG (WALL_TOP)
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

        // 4. LỚP CỬA PHÒNG
        for (Room room : rooms) {
            room.renderDoors(gc, camera, tileSize);
        }
    }

    // Lấy danh sách ô tường phục vụ cho Y-Sorting
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

    // Kiểm tra tường có Sàn phía trên nó hay không
    private boolean isSouthWallBoundary(int x, int y) {
        Tile current = tiles[y][x];
        if (current == null || current.getType() != Tile.TileType.WALL) {
            return false;
        }
        return (y > 0 && tiles[y - 1][x] != null && tiles[y - 1][x].getType() == Tile.TileType.FLOOR);
    }

    // Kiểm tra va chạm bản đồ
    public boolean isWalkable(double worldX, double worldY, double radius) {
        for (Room room : rooms) {
            if (room.isHitClosedDoor(worldX, worldY, radius)) {
                return false;
            }
        }

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
                if (tile == null || !tile.isWalkable()) {
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

        boolean isBottomWall = isSouthWallBoundary(tx, ty);

        if (isBottomWall) {
            // Tường phía Nam: Cho phép lấn chân nhẹ 50% vào thành tường để tạo chiều sâu 3D
            tileTop += (tileSize * 0.5);
        }

        double closestX = Math.max(tileLeft, Math.min(cx, tileRight));
        double closestY = Math.max(tileTop, Math.min(cy, tileBottom));

        double distX = cx - closestX;
        double distY = cy - closestY;
        double distanceSquared = (distX * distX) + (distY * distY);

        return distanceSquared < (radius * radius);
    }

    private void loadMapFromJson(String path) {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) {
                System.out.println("khong co file json: " + path);
                return;
            }

            ObjectMapper mapper = new ObjectMapper();
            TiledMapData mapData = mapper.readValue(is, TiledMapData.class);
            this.width = mapData.width;
            this.height = mapData.height;
            this.tileSize = mapData.tilewidth;
            this.tileMatrix = new int[this.height][this.width];

            for (LayerData layer : mapData.layers) {
                if ("objectgroup".equals(layer.type) && layer.objects != null) {
                    for (ObjectData obj : layer.objects) {
                        String type = obj.type != null ? obj.type.trim() : "";
                        String name = obj.name != null ? obj.name.trim() : "";

                        if (type.equals("Spawn")) {
                            this.playerSpawnX = obj.x;
                            this.playerSpawnY = obj.y;
                            this.spawnPoint = new Vector2D(obj.x, obj.y);
                        } else if (type.equals("Room")) {
                            Room room = new Room(name, obj.x, obj.y, obj.width, obj.height);
                            rooms.add(room);
                        }
                    }
                }
            }

            for (LayerData layer : mapData.layers) {
                if ("tilelayer".equals(layer.type)) {
                    for (int i = 0; i < layer.data.size(); i++) {
                        int y = i / width;
                        int x = i % width;
                        this.tileMatrix[y][x] = layer.data.get(i);
                    }
                } else if ("objectgroup".equals(layer.type) && layer.objects != null) {
                    for (ObjectData obj : layer.objects) {
                        String type = obj.type != null ? obj.type.trim() : "";
                        String name = obj.name != null ? obj.name.trim() : "";

                        if (type.equals("Door") || name.startsWith("Door")) {
                            double doorX = obj.x;
                            double doorY = obj.y;
                            double doorW = obj.width;
                            double doorH = obj.height;

                            boolean assignedByBound = false;
                            for (Room room : rooms) {
                                if (room.isDoorBelongsToRoom(doorX, doorY, doorW, doorH)) {
                                    room.addDoorCoordinate(doorX, doorY, doorW, doorH);
                                    assignedByBound = true;
                                }
                            }

                            if (!assignedByBound && obj.properties != null) {
                                String belongToRoom = "";
                                for (PropertyData prop : obj.properties) {
                                    if ("belongToRoom".equals(prop.name)) {
                                        belongToRoom = prop.value.trim();
                                        break;
                                    }
                                }
                                final String targetRoomName = belongToRoom;
                                rooms.stream()
                                        .filter(r -> r.getName().equals(targetRoomName))
                                        .findFirst()
                                        .ifPresent(r -> r.addDoorCoordinate(doorX, doorY, doorW, doorH));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void generateWorldTiles() {
        this.tiles = new Tile[height][width];

        // Khởi tạo mặc định theo đúng ID trong JSON (ID 1: FLOOR, ID 2: WALL)
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int tileId = tileMatrix[y][x];
                double pixelX = x * tileSize;
                double pixelY = y * tileSize;

                if (tileId == 1) {
                    this.tiles[y][x] = new Tile(pixelX, pixelY, tileSize, Tile.TileType.FLOOR);
                } else if (tileId == 2) {
                    this.tiles[y][x] = new Tile(pixelX, pixelY, tileSize, Tile.TileType.WALL);
                } else {
                    this.tiles[y][x] = null;
                }
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