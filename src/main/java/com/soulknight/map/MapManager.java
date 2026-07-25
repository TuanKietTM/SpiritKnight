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

    //(vitdung) viết constructor này để cho GameWorld load map lên
    public MapManager(String JsonPath) {
        loadMapFromJson(JsonPath);
        generateWorldTiles();
    }
    // 1. Vẽ Nền sàn và Tường phía trên
    public void renderBackground(GraphicsContext graphicsContext, Camera camera, double renderWidth, double renderHeight) {
        graphicsContext.setFill(Color.BLACK);
        graphicsContext.fillRect(0.0, 0.0, renderWidth, renderHeight);

        double zoom = camera.getZoom();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Tile tile = tiles[y][x];
                if (tile == null) continue;

                // Bỏ qua tường phía dưới để vẽ ở Foreground
                if (isSouthWallBoundary(x, y)) {
                    continue;
                }

                double worldX = x * tileSize;
                double worldY = y * tileSize;
                double screenX = camera.worldToScreenX(worldX);
                double screenY = camera.worldToScreenY(worldY);

                graphicsContext.drawImage(tile.getTexture(), screenX, screenY, tileSize * zoom, tileSize * zoom);
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

        for (Room room : rooms) {
            room.renderDoors(graphicsContext, camera, tileSize);
        }
    }

    // 2. Vẽ Lớp Tường phía dưới đè lên chân Player
    public void renderForeground(GraphicsContext graphicsContext, Camera camera) {
        double zoom = camera.getZoom();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Tile tile = tiles[y][x];
                if (tile == null) continue;

                if (isSouthWallBoundary(x, y)) {
                    double worldX = x * tileSize;
                    double worldY = y * tileSize;
                    double screenX = camera.worldToScreenX(worldX);
                    double screenY = camera.worldToScreenY(worldY);

                    graphicsContext.drawImage(tile.getTexture(), screenX, screenY, tileSize * zoom, tileSize * zoom);
                }
            }
        }
    }

    // 3. Hàm kiểm tra Tường phía dưới
    private boolean isSouthWallBoundary(int x, int y) {
        Tile current = tiles[y][x];
        if (current == null || current.getType() != Tile.TileType.WALL) {
            return false;
        }
        // Tường có Sàn ngay phía trên nó -> Là bức tường tiếp giáp cạnh dưới của phòng
        return (y > 0 && tiles[y - 1][x] != null && tiles[y - 1][x].getType() == Tile.TileType.FLOOR);
    }

    // (cuongpluss) Kiem tra va cham tren ban do
    // SỬA LẠI TRONG MapManager.java
    public boolean isWalkable(double worldX, double worldY, double radius) {
        // Kiem tra va cham voi cua dang dong cua cac phong
        for (Room room : rooms) {
            if (room.isHitClosedDoor(worldX, worldY, radius)) {
                return false;
            }
        }

        // 🎯 CHỈNH HITBOX BÀN CHÂN:
        // Thu nhỏ bán kính va chạm lại 50% (radius * 0.5) và dịch tâm xuống chân một chút (+6px)
        // Giúp phần thân nhân vật lấn thoải mái vào sprite tường
        double footRadius = radius * 0.5;
        double footY = worldY + 8.0;

        // Xac dinh pham vi các ô gạch xung quanh theo Hitbox bàn chân mới
        int minTileX = (int) Math.floor((worldX - footRadius) / tileSize);
        int maxTileX = (int) Math.floor((worldX + footRadius) / tileSize);
        int minTileY = (int) Math.floor((footY - footRadius) / tileSize);
        int maxTileY = (int) Math.floor((footY + footRadius) / tileSize);

        for (int ty = minTileY; ty <= maxTileY; ty++) {
            for (int tx = minTileX; tx <= maxTileX; tx++) {

                if (tx < 0 || ty < 0 || tx >= width || ty >= height) {
                    if (isCircleCollidingWithTile(worldX, footY, footRadius, tx, ty)) {
                        return false;
                    }
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

    // kiem tra va cham cua player : coi player la circle
    private boolean isCircleCollidingWithTile(double cx, double cy, double radius, int tx, int ty) {
        double tileLeft = tx * tileSize;
        double tileRight = (tx + 1) * tileSize;
        double tileTop = ty * tileSize;
        double tileBottom = (ty + 1) * tileSize;

        double closestX = Math.max(tileLeft, Math.min(cx, tileRight));
        double closestY = Math.max(tileTop, Math.min(cy, tileBottom));

        double distX = cx - closestX;
        double distY = cy - closestY;
        double distanceSquared = (distX * distX) + (distY * distY);

        return distanceSquared < (radius * radius);
    }

    //(vitdung) hàm đọc dữ liệu từ file Json
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

            // Doc layer ROOM de khoi tao danh sach phong
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

            // doc layer Tile va gan cac Object dang Door vao Room
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

                            // gan o cua voi tat ca cac phong tiep giap voi no
                            boolean assignedByBound = false;
                            for (Room room : rooms) {
                                if (room.isDoorBelongsToRoom(doorX, doorY, doorW, doorH)) {
                                    room.addDoorCoordinate(doorX, doorY, doorW, doorH);
                                    assignedByBound = true;
                                }
                            }

                            // Du phong khong bat duoc toa do giao cat
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

    //(vitdung) hàm chuyển ma trânj thành map.
    private void generateWorldTiles() {
        this.tiles = new Tile[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int tileId = tileMatrix[y][x];

                double pixelX = x * tileSize;
                double pixelY = y * tileSize;

                switch (tileId) {
                    case 1:
                        this.tiles[y][x] = new Tile(pixelX, pixelY, tileSize, Tile.TileType.FLOOR);
                        break;
                    case 2:
                        this.tiles[y][x] = new Tile(pixelX, pixelY, tileSize, Tile.TileType.WALL);
                        break;
                    default:
                        this.tiles[y][x] = null;
                        break;
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

    /**
     * Tìm một vị trí ngẫu nhiên hợp lệ nằm trọn trong một Room cụ thể.
     * Đảm bảo vị trí đó là WALKABLE (Sàn) và không đè lên tường.
     */
    public Vector2D findRandomWalkablePositionInRoom(Room room, Random random, double enemyRadius) {
        javafx.geometry.BoundingBox bound = room.getBound();

        // Tính toán giới hạn ô gạch (Tile) bao quanh phòng để tối ưu vòng lặp
        int minTileX = (int) Math.floor(bound.getMinX() / tileSize);
        int maxTileX = (int) Math.floor((bound.getMinX() + bound.getWidth()) / tileSize);
        int minTileY = (int) Math.floor(bound.getMinY() / tileSize);
        int maxTileY = (int) Math.floor((bound.getMinY() + bound.getHeight()) / tileSize);

        // Thử tối đa 100 lần để tìm vị trí trống sạch sẽ
        for (int attempt = 0; attempt < 100; attempt++) {
            // Lấy ngẫu nhiên một tọa độ pixel nằm trong BoundingBox của phòng
            double randomX = bound.getMinX() + random.nextDouble() * bound.getWidth();
            double randomY = bound.getMinY() + random.nextDouble() * bound.getHeight();

            // 🎯 ĐIỀU KIỆN 1: Tọa độ đó phải di chuyển được (Walkable) theo cơ chế va chạm hiện tại
            if (isWalkable(randomX, randomY, enemyRadius)) {

                // 🎯 ĐIỀU KIỆN 2: Ép kỹ hơn - ô gạch tại tâm đó bắt buộc phải tồn tại và là FLOOR
                int tx = (int) Math.floor(randomX / tileSize);
                int ty = (int) Math.floor(randomY / tileSize);

                if (tx >= minTileX && tx <= maxTileX && ty >= minTileY && ty <= maxTileY) {
                    Tile tile = tiles[ty][tx];
                    // Chỉ cho phép sinh trên gạch sàn thông thường (ID = 1)
                    if (tile != null && tile.isWalkable()) {
                        return new Vector2D(randomX, randomY);
                    }
                }
            }
        }
        // Nếu phòng quá chật hoặc không tìm thấy sau 100 lần, trả về tâm phòng làm điểm dự phòng
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