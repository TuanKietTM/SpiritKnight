package com.soulknight.map;

import javafx.geometry.BoundingBox;
import java.util.ArrayList;
import java.util.List;

public class Room {
    private String name;
    private BoundingBox bound;
    private boolean isActived = false; // Check xem người chơi đã vào phòng và kích hoạt trận đấu chưa
    private boolean isCleared = false; // Check xem phòng đã dọn sạch quái chưa
    private boolean isDoorClosed = false; // Trạng thái cửa hiện tại của phòng
    private List<BoundingBox> doors = new ArrayList<>();

    public Room(String name, double x, double y, double width, double height) {
        this.name = name;
        this.bound = new BoundingBox(x, y, width, height);
    }

    public void addDoorCoordinate(double x, double y, double width, double height) {
        this.doors.add(new BoundingBox(x, y, width, height));
    }

    // Thêm deltaSeconds vào hàm update của Room
    public void update(com.soulknight.engine.GameWorld gameWorld, double playerX, double playerY, List<com.soulknight.entity.Enemy> globalEnemies, double deltaSeconds) {
        if (isCleared) return;

        // Kiểm tra player vào room
        if (!isActived && bound.contains(playerX, playerY)) {
            activeRoom(gameWorld);
        }

        if (isActived) {
            checkRoomClear(globalEnemies);
        }
    }

    private void activeRoom(com.soulknight.engine.GameWorld gameWorld) {
        this.isActived = true;
        this.isDoorClosed = true;
        System.out.println("[ROOM LOG] Người chơi đã vào " + name + " -> Đóng cửa và gọi quái!");

        // Gọi GameWorld sinh quái RIÊNG cho căn phòng này dựa vào phạm vi bound của nó
        gameWorld.spawnEnemiesInRoom(this);
    }

    private void checkRoomClear(List<com.soulknight.entity.Enemy> globalEnemies) {
        // Đếm số lượng quái vật còn sống nằm trong phạm vi (bound) của căn phòng này
        long aliveEnemiesInRoom = globalEnemies.stream()
                .filter(enemy -> enemy.isAlive() && bound.contains(enemy.getPosition().getX(), enemy.getPosition().getY()))
                .count();

        // Nếu không còn con quái nào sống sót trong phòng này
        if (aliveEnemiesInRoom == 0) {
            this.isActived = false;
            this.isCleared = true;
            this.isDoorClosed = false; // Mở cửa ra cho người chơi đi tiếp
            System.out.println("[ROOM LOG] Chúc mừng! Đã dọn sạch " + name + " | Mở toàn bộ cửa!");
        }
    }

    // Hàm kiểm tra xem một vị trí bất kỳ có đang bị cửa đóng chặn lại không
    public boolean isHitClosedDoor(double worldX, double worldY, double radius) {
        if (!isDoorClosed) return false; // Cửa đang mở thì cho đi qua thoải mái

        // Kiểm tra xem vòng tròn của player có giao cắt với bất kỳ ô cửa nào của phòng không
        for (BoundingBox door : doors) {
            if (door.intersects(worldX - radius, worldY - radius, radius * 2, radius * 2)) {
                return true; // Đang đâm đầu vào cửa đóng!
            }
        }
        return false;
    }

    // Getter & Setter
    public String getName() { return name; }
    public BoundingBox getBound() { return bound; }
    public boolean isActived() { return isActived; }
    public boolean isCleared() { return isCleared; }
    public boolean isDoorClosed() { return isDoorClosed; }
    public List<BoundingBox> getDoors() { return doors; }
}