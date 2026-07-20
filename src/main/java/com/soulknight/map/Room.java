package com.soulknight.map;

import javafx.geometry.BoundingBox;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import com.soulknight.engine.Camera;
import java.util.ArrayList;
import java.util.List;

public class Room {
    private String name;
    private BoundingBox bound;
    private boolean isActived = false;
    private boolean isCleared = false;
    private boolean isDoorClosed = false;
    private List<BoundingBox> doors = new ArrayList<>();

    public Room(String name, double x, double y, double width, double height) {
        this.name = name;
        this.bound = new BoundingBox(x, y, width, height);
    }

    /**
     * Render toàn bộ cửa thuộc phòng dựa trên trạng thái (Mở / Đóng).
     */
    public void renderDoors(GraphicsContext gc, Camera camera, double tileSize) {
        // Lấy texture cửa tương ứng với trạng thái (Mở hay Đóng) từ Tile class
        Image doorTexture = Tile.getDoorImage(isDoorClosed);
        if (doorTexture == null) return;

        double zoom = camera.getZoom();

        for (BoundingBox door : doors) {
            // Chia nhỏ vùng door theo tileSize để lặp lại tile, tránh làm giãn/bóp méo hình
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

    // Kiểm tra xem một ô cửa có nằm sát hoặc giao với biên của phòng này không
    public boolean isDoorBelongsToRoom(double doorX, double doorY, double doorW, double doorH) {
        BoundingBox doorBox = new BoundingBox(doorX, doorY, doorW, doorH);
        // Mở rộng biên của phòng ra 10px để bắt dính các ô cửa nằm ngay cạnh tường phòng
        BoundingBox expandedBound = new BoundingBox(
                bound.getMinX() - 10, bound.getMinY() - 10,
                bound.getWidth() + 20, bound.getHeight() + 20
        );
        return expandedBound.intersects(doorBox);
    }

    public void addDoorCoordinate(double x, double y, double width, double height) {
        // Tránh thêm trùng lặp cửa
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
            checkRoomClear(globalEnemies);
        }
    }

    private void activeRoom(com.soulknight.engine.GameWorld gameWorld, com.soulknight.entity.Player player) {
        this.isActived = true;
        this.isDoorClosed = true; // Sập TẤT CẢ các cửa liên kết với phòng này!

        // Đẩy Player tiến nhẹ vào trong phòng để không bị kẹt khi cửa sập
        double roomCenterX = bound.getMinX() + bound.getWidth() / 2.0;
        double roomCenterY = bound.getMinY() + bound.getHeight() / 2.0;

        double diffX = roomCenterX - player.getPosition().getX();
        double diffY = roomCenterY - player.getPosition().getY();
        double length = Math.sqrt(diffX * diffX + diffY * diffY);

        if (length > 0.0) {
            double pushX = (diffX / length) * 45.0;
            double pushY = (diffY / length) * 45.0;
            player.getPosition().add(new com.soulknight.utils.Vector2D(pushX, pushY));
        }

        gameWorld.spawnEnemiesInRoom(this);
    }

    private void checkRoomClear(List<com.soulknight.entity.Enemy> globalEnemies) {
        long aliveEnemiesInRoom = globalEnemies.stream()
                .filter(enemy -> enemy.isAlive() && bound.contains(enemy.getPosition().getX(), enemy.getPosition().getY()))
                .count();

        if (aliveEnemiesInRoom == 0) {
            this.isActived = false;
            this.isCleared = true;
            this.isDoorClosed = false; // Mở lại toàn bộ cửa để đi sang các phòng khác
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