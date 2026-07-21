package com.soulknight.map;

import com.soulknight.utils.Vector2D;
import javafx.geometry.BoundingBox;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import com.soulknight.engine.Camera;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Room {
    private String name;
    private BoundingBox bound;
    private boolean isActived = false;
    private boolean isCleared = false;
    private boolean isDoorClosed = false;
    private List<BoundingBox> doors = new ArrayList<>();

    // Quản lý đợt quái (wave)
    private int currentWave = 0;
    private int maxWaves = 2;
    private double waveDelayTimer = 0.0;
    private boolean isWaitingForNextWave = false;

    // Danh sách vật cản trong phòng
    private List<Obstacle> obstacles = new ArrayList<>();

    public Room(String name, double x, double y, double width, double height) {
        this.name = name;
        this.bound = new BoundingBox(x, y, width, height);

        // 🎯 THIẾT LẬP CHO PHÒNG ĐẦU TIÊN TRỐNG HOÀN TOÀN
        if (this.name.equalsIgnoreCase("StartRoom") || this.name.contains("Spawn")) {
            this.isActived = false;
            this.isCleared = true;
            this.isDoorClosed = false;
            this.maxWaves = 0;
        } else if (this.name.equalsIgnoreCase("BossRoom")) {
            this.maxWaves = 1;         // Phòng boss chỉ có 1 đợt quái
        } else {
            this.maxWaves = 2;         // Các phòng quái bình thường có 2 đợt quái
        }
    }

    public List<Obstacle> getObstacles() {
        return obstacles;
    }

    public void generateObstacles(Random random) {
        obstacles.clear();

        if (this.name.equalsIgnoreCase("StartRoom") || this.name.contains("Spawn")) {
            return;
        }

        int maxObstacles = 3; // Giới hạn số lượng tối đa trong 1 phòng
        int attempts = 0;
        double obsSize = 40.0;
        double minPaddingFromWall = 40.0; // Khoảng cách an toàn tránh dính vào tường
        double minDistanceBetweenObs = 60.0; // Khoảng cách tối thiểu giữa các vật cản

        while (obstacles.size() < maxObstacles && attempts < 50) {
            attempts++;

            double minX = bound.getMinX() + minPaddingFromWall;
            double minY = bound.getMinY() + minPaddingFromWall;
            double maxX = bound.getMinX() + bound.getWidth() - minPaddingFromWall - obsSize;
            double maxY = bound.getMinY() + bound.getHeight() - minPaddingFromWall - obsSize;

            if (maxX <= minX || maxY <= minY) break;

            double x = minX + random.nextDouble() * (maxX - minX);
            double y = minY + random.nextDouble() * (maxY - minY);
            Vector2D newPos = new Vector2D(x, y);

            // Kiểm tra xem có bị đè lên vật cản đã sinh trước đó không
            boolean overlapped = false;
            for (Obstacle existing : obstacles) {
                if (newPos.distance(existing.getPosition()) < minDistanceBetweenObs) {
                    overlapped = true;
                    break;
                }
            }

            if (!overlapped) {
                boolean destructible = random.nextDouble() > 0.2; // 80% phá được, 20% cột đá
                obstacles.add(new Obstacle(newPos, obsSize, obsSize, 30, destructible));
            }
        }
    }

    /**
     * Render toàn bộ cửa thuộc phòng dựa trên trạng thái (Mở / Đóng).
     */
    public void renderDoors(GraphicsContext gc, Camera camera, double tileSize) {
        Image doorTexture = Tile.getDoorImage(isDoorClosed);
        if (doorTexture == null) return;

        double zoom = camera.getZoom();

        for (BoundingBox door : doors) {
            int tilesX = (int) Math.round(door.getWidth() / tileSize);
            int tilesY = (int) Math.round(door.getHeight() / tileSize);

            if (tilesX <= 0) tilesX = 1;
            if (tilesY <= 0) tilesY = 1;

            for (int ty = 0; ty < tilesY; ty++) {
                for (int tx = 0; tx < tilesX; tx++) {
                    double worldX = door.getMinX() + (tx * tileSize);
                    double worldY = door.getMinY() + (ty * tileSize);

                    double screenX = camera.worldToScreenX(worldX);
                    double screenY = camera.worldToScreenY(worldY);

                    gc.drawImage(doorTexture, screenX, screenY, tileSize * zoom, tileSize * zoom);
                }
            }
        }
    }

    public boolean isDoorBelongsToRoom(double doorX, double doorY, double doorW, double doorH) {
        BoundingBox doorBox = new BoundingBox(doorX, doorY, doorW, doorH);
        BoundingBox expandedBound = new BoundingBox(
                bound.getMinX() - 10, bound.getMinY() - 10,
                bound.getWidth() + 20, bound.getHeight() + 20
        );
        return expandedBound.intersects(doorBox);
    }

    public void addDoorCoordinate(double x, double y, double width, double height) {
        BoundingBox newDoor = new BoundingBox(x, y, width, height);
        for (BoundingBox d : doors) {
            if (d.getMinX() == x && d.getMinY() == y) return;
        }
        this.doors.add(newDoor);
    }

    public void update(com.soulknight.engine.GameWorld gameWorld, com.soulknight.entity.Player player, List<com.soulknight.entity.Enemy> globalEnemies, double deltaSeconds) {
        if (isCleared) return;

        double playerX = player.getPosition().getX();
        double playerY = player.getPosition().getY();

        if (!isActived && bound.contains(playerX, playerY)) {
            activeRoom(gameWorld, player);
        }

        if (isActived) {
            if (isWaitingForNextWave) {
                waveDelayTimer -= deltaSeconds;
                if (waveDelayTimer <= 0) {
                    isWaitingForNextWave = false;
                    currentWave++;
                    gameWorld.spawnEnemiesInRoom(this, currentWave);
                }
            }
            checkRoomClear(gameWorld, globalEnemies);
        }
    }

    private void activeRoom(com.soulknight.engine.GameWorld gameWorld, com.soulknight.entity.Player player) {
        this.isActived = true;
        this.isDoorClosed = true;

        // Đẩy Player tiến nhẹ vào trong phòng để không bị kẹt khi cửa sập
        double roomCenterX = bound.getMinX() + bound.getWidth() / 2.0;
        double roomCenterY = bound.getMinY() + bound.getHeight() / 2.0;

        double diffX = roomCenterX - player.getPosition().getX();
        double diffY = roomCenterY - player.getPosition().getY();
        double length = Math.sqrt(diffX * diffX + diffY * diffY);
//xac dinh khoang day
        if (length > 0.0) {
            double pushX = (diffX / length) * 5.0;
            double pushY = (diffY / length) * 5.0;
            player.getPosition().add(new com.soulknight.utils.Vector2D(pushX, pushY));
        }

        // Khởi động wave đầu tiên & Sinh vật cản cho phòng
        this.currentWave = 1;
        generateObstacles(new Random());
        gameWorld.spawnEnemiesInRoom(this, currentWave);
    }

    private void checkRoomClear(com.soulknight.engine.GameWorld gameWorld, List<com.soulknight.entity.Enemy> globalEnemies) {
        long aliveEnemiesInRoom = globalEnemies.stream()
                .filter(enemy -> enemy.isAlive() && bound.contains(enemy.getPosition().getX(), enemy.getPosition().getY()))
                .count();

        if (aliveEnemiesInRoom == 0 && !isWaitingForNextWave) {
            if (currentWave < maxWaves) {
                this.isWaitingForNextWave = true;
                this.waveDelayTimer = 1.0;
            } else {
                this.isActived = false;
                this.isCleared = true;
                this.isDoorClosed = false;
            }
        }
    }

    public boolean isHitClosedDoor(double worldX, double worldY, double radius) {
        if (!isDoorClosed) return false;

        for (BoundingBox door : doors) {
            if (door.intersects(worldX - radius, worldY - radius, radius * 2, radius * 2)) {
                return true;
            }
        }
        return false;
    }

    public String getName() { return name; }
    public BoundingBox getBound() { return bound; }
    public boolean isActived() { return isActived; }
    public boolean isCleared() { return isCleared; }
    public boolean isDoorClosed() { return isDoorClosed; }
    public List<BoundingBox> getDoors() { return doors; }
}