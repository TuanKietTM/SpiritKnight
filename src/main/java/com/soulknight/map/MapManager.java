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
    // 1. Chỉ vẽ ô SÀN (Nền nhà bẹt dưới cùng)
    public void renderFloor(GraphicsContext gc, Camera camera, double renderWidth, double renderHeight) {
        gc.setFill(Color.BLACK);
        gc.fillRect(0.0, 0.0, renderWidth, renderHeight);

        double zoom = camera.getZoom();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Tile tile = tiles[y][x];
                if (tile == null) continue;

                // Chỉ vẽ các ô là FLOOR hoặc DOOR_OPEN (các ô nằm bẹt dưới đất)
                if (tile.getType() != Tile.TileType.WALL) {
                    double worldX = x * tileSize;
                    double worldY = y * tileSize;
                    double screenX = camera.worldToScreenX(worldX);
                    double screenY = camera.worldToScreenY(worldY);
                    gc.drawImage(tile.getTexture(), screenX, screenY, tileSize * zoom, tileSize * zoom);
                }
            }
        }

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

    // 2. Lấy danh sách tất cả các Ô TƯỜNG để đưa vào hệ thống Y-Sorting
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
    public boolean isWalkable(double worldX, double worldY, double radius) {
        for (Room room : rooms) {
            if (room.isHitClosedDoor(worldX, worldY, radius)) {
                return false;
            }
        }

        // Tâm tính va chạm dịch nhẹ xuống chân Player (+6px)
        double footY = worldY + 6.0;
        double footRadius = radius * 0.6; // Hitbox thu gọn ở chân

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

    // kiem tra va cham cua player : coi player la circle
    private boolean isCircleCollidingWithTile(double cx, double cy, double radius, int tx, int ty) {
        double tileLeft = tx * tileSize;
        double tileRight = (tx + 1) * tileSize;
        double tileTop = ty * tileSize;
        double tileBottom = (ty + 1) * tileSize;


        boolean isBottomWall = (ty > 0 && tiles[ty - 1][tx] != null && tiles[ty - 1][tx].getType() == Tile.TileType.FLOOR);

        if (isBottomWall) {
            // Nếu là Tường Ngang ở đáy phòng: Cho phép lấn sâu xuống 50% để đè tường thò đầu
            tileTop += (tileSize * 0.5);
        } else {
            // Nếu là Tường Dọc, Tường Góc L, hay Tường Trên: Chặn full 100% không cho kẹt góc!
            // (Giữ nguyên tileTop = ty * tileSize)
        }

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
                System.err.println("Không thấy file json: " + path);
                return;
            }

            ObjectMapper mapper = new ObjectMapper();
            TiledMapData mapData = mapper.readValue(is, TiledMapData.class);

            // 1. Lấy thông số kích thước Map
            this.width = mapData.width;
            this.height = mapData.height;
            this.tileSize = mapData.tilewidth;
            this.tileMatrix = new int[this.height][this.width];

            // 2. ĐỌC TẤT CẢ TILE LAYERS (floor, wall, door, v.v...)
            // Dùng cơ chế đè: Chỉ lấy ID > 0 để các layer ở trên (wall, door)
            // không làm xóa mất dữ liệu của layer bên dưới (floor)
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
                }

                // 3. ĐỌC OBJECT GROUP (Chỉ dùng cho SpawnPoint và Rooms)
                else if ("objectgroup".equals(layer.type) && layer.objects != null) {
                    for (ObjectData obj : layer.objects) {
                        String type = obj.type != null ? obj.type.trim() : "";
                        String name = obj.name != null ? obj.name.trim() : "";

                        // A. Bắt vị trí Player Spawn
                        if ("playerSpawn".equalsIgnoreCase(name) || "SpawnPoint".equalsIgnoreCase(type) || "Spawn".equalsIgnoreCase(type)) {
                            this.playerSpawnX = obj.x;
                            this.playerSpawnY = obj.y;
                            this.spawnPoint = new Vector2D(obj.x, obj.y);
                        }
                        // B. Bắt danh sách các Phòng (startRoom, fightRoom_1, restRoom, bossRoom...)
                        else if ("Room".equalsIgnoreCase(type) || name.toLowerCase().contains("room")) {
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

    //(vitdung) hàm chuyển ma trânj thành map.
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

                switch (tileId) {
                    case 11:
                        this.tiles[y][x] = new Tile(pixelX, pixelY, tileSize, Tile.TileType.FLOOR);
                        break;
                    case 1:
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

    public boolean isBulletCollidingWithWall(double worldX, double worldY) {
        int tx = (int) Math.floor(worldX / tileSize);
        int ty = (int) Math.floor(worldY / tileSize);

        // Bắn ra ngoài biên map -> Nổ đạn
        if (tx < 0 || ty < 0 || tx >= width || ty >= height) {
            return true;
        }

        Tile tile = tiles[ty][tx];
        if (tile == null || !tile.isWalkable()) {
            // Kiểm tra xem ô này có phải là Tường Ngang dưới đáy phòng không
            boolean isBottomWall = (ty > 0 && tiles[ty - 1][tx] != null && tiles[ty - 1][tx].getType() == Tile.TileType.FLOOR);

            double tileTop = ty * tileSize;
            if (isBottomWall) {
                // Với tường đáy: Đạn được bay lọt qua nửa trên (phần thò đầu),
                // chỉ nổ khi bay đâm vào NỬA DƯỚI (chân tường cứng)
                tileTop += (tileSize * 0.5);
            }

            // Nếu tọa độ Y của đạn chạm vào vùng cứng -> Trả về true (Nổ đạn)
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