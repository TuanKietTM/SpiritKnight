package com.soulknight.map;

import com.soulknight.engine.Camera;
import com.soulknight.utils.Constants;
import com.soulknight.utils.Vector2D;
import com.soulknight.map.json.*;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
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
    //(vitdung) thêm biến để viết hàm
    private Vector2D spawnPoint;
    private Vector2D bossSpawnPoint;
    private final List<Vector2D> enemySpawnPoints = new ArrayList<>();
    private int[][] tileMatrix; // biến lưu ma trận đọc từ Json
    private List<Room> rooms = new ArrayList<>();
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
//        this.tileSize = Constants.TILE_SIZE;
        generateWorldTiles();
    }

    public void render(GraphicsContext graphicsContext, Camera camera, double renderWidth, double renderHeight) {
        graphicsContext.setFill(Color.BLACK);
        graphicsContext.fillRect(0.0, 0.0, renderWidth, renderHeight);

        double zoom = camera.getZoom();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Tile tile = tiles[y][x];
                if (tile == null) continue;
                double worldX = x * tileSize;
                double worldY = y * tileSize;
                double screenX = camera.worldToScreenX(worldX);
                double screenY = camera.worldToScreenY(worldY);

                graphicsContext.setFill(tile.getFillColor());
                graphicsContext.fillRect(screenX, screenY, tileSize * zoom, tileSize * zoom);

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

    // 🎯 THAY THẾ TOÀN BỘ HÀM isWalkable CŨ BẰNG THUẬT TOÁN HÌNH TRÒN NÀY
    public boolean isWalkable(double worldX, double worldY, double radius) {
        // 1. Xác định phạm vi các ô gạch xung quanh Player dựa trên khung bao của hình tròn
        int minTileX = (int) Math.floor((worldX - radius) / tileSize);
        int maxTileX = (int) Math.floor((worldX + radius) / tileSize);
        int minTileY = (int) Math.floor((worldY - radius) / tileSize);
        int maxTileY = (int) Math.floor((worldY + radius) / tileSize);

        // 2. Duyệt qua toàn bộ các ô gạch trong vùng lân cận Player
        for (int ty = minTileY; ty <= maxTileY; ty++) {
            for (int tx = minTileX; tx <= maxTileX; tx++) {

                // Nếu ô gạch nằm ngoài biên bản đồ -> Coi như đó là tường cứng
                if (tx < 0 || ty < 0 || tx >= width || ty >= height) {
                    if (isCircleCollidingWithTile(worldX, worldY, radius, tx, ty)) {
                        return false;
                    }
                    continue;
                }

                Tile tile = tiles[ty][tx];
                // Nếu ô gạch là Tường (hoặc ô rỗng null) -> Kiểm tra va chạm tròn với ô vuông này
                if (tile == null || !tile.isWalkable()) {
                    if (isCircleCollidingWithTile(worldX, worldY, radius, tx, ty)) {
                        return false; // Phát hiện va chạm thực sự với ô tường!
                    }
                }
            }
        }
        return true; // Không va chạm với bất kỳ ô tường nào, di chuyển an toàn!
    }

    // 🎯 HÀM TRỢ GIÚP: Tính khoảng cách chính xác từ tâm hình tròn tới cạnh của ô gạch
    private boolean isCircleCollidingWithTile(double cx, double cy, double radius, int tx, int ty) {
        // Xác định biên giới hạn (trái, phải, trên, dưới) của ô gạch mục tiêu
        double tileLeft = tx * tileSize;
        double tileRight = (tx + 1) * tileSize;
        double tileTop = ty * tileSize;
        double tileBottom = (ty + 1) * tileSize;

        // Tìm điểm gần nhất nằm trên biên của ô gạch đối với tâm hình tròn (cx, cy)
        double closestX = Math.max(tileLeft, Math.min(cx, tileRight));
        double closestY = Math.max(tileTop, Math.min(cy, tileBottom));

        // Tính khoảng cách từ tâm hình tròn tới điểm gần nhất đó
        double distX = cx - closestX;
        double distY = cy - closestY;
        double distanceSquared = (distX * distX) + (distY * distY);

        // Nếu khoảng cách nhỏ hơn bình phương bán kính -> Có va chạm sạt mép
        return distanceSquared < (radius * radius);
    }

    //(vitdung) hàm đọc dữ liệu từ file Json
    private void loadMapFromJson(String path) {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) {
                System.out.println("khong co file json");
                return;
            }

            ObjectMapper mapper = new ObjectMapper();
            TiledMapData mapData = mapper.readValue(is, TiledMapData.class);
            this.width = mapData.width;
            this.height = mapData.height;
            this.tileSize = mapData.tilewidth;
            this.tileMatrix = new int[this.height][this.width];
            for (LayerData layer : mapData.layers) {
                if ("tilelayer".equals(layer.type)) {
                    for (int i = 0; i < layer.data.size(); i++) {
                        int y = i / width;
                        int x = i % width;
                        this.tileMatrix[y][x] = layer.data.get(i);
                    }
                }
                else if ("objectgroup".equals(layer.type) && layer.objects != null) {
                    for (ObjectData obj : layer.objects) {
                        String type = obj.type != null ? obj.type.trim() : "";
                        String name = obj.name != null ? obj.name.trim() : "";

                        if (type.equals("Spawn")) {
                            this.playerSpawnX = obj.x;
                            this.playerSpawnY = obj.y;
                            this.spawnPoint = new Vector2D(obj.x, obj.y);
                        }
                        else if (type.equals("Room")) {
                            // Tạo vùng phòng ảo dựa trên tọa độ pixel của Tiled
                            Room room = new Room(name, obj.x, obj.y, obj.width, obj.height);
                            rooms.add(room);
                        }
                        // Bạn có thể xử lý thêm Door và EnemySpawn tại đây...
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
                        // ID = 1 tương ứng với Floor (Sàn)
                        // 🎯 SỬA 3: Truyền thêm tọa độ pixel và kích thước vào để Tile tự định vị
                        this.tiles[y][x] = new Tile(pixelX, pixelY, tileSize, Tile.TileType.FLOOR);
                        break;
                    case 2:
                        // ID = 2 tương ứng với Wall (Tường)
                        this.tiles[y][x] = new Tile(pixelX, pixelY, tileSize, Tile.TileType.WALL);
                        break;
                    default:
                        // Mặc định là nền đen hoặc ô trống (không vẽ gì)
                        this.tiles[y][x] = null;
                        break;
                }
            }
        }
        System.out.println("Đã dịch và khởi tạo toàn bộ thực thể gạch trên Map!");
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



    public List<Vector2D> getEnemySpawnPoints() {
        return this.enemySpawnPoints;
    }

    public Tile[][] getTiles() { return tiles; }
    public double getPlayerSpawnX() { return playerSpawnX; }
    public double getPlayerSpawnY() { return playerSpawnY; }


}
