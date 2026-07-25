package com.soulknight.pet;

import com.soulknight.engine.Camera;
import com.soulknight.map.MapManager; // Đã import đúng MapManager
import com.soulknight.utils.Vector2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

import java.net.URL;

/**
 * Pet đi theo người chơi.
 *
 * Nâng cấp:
 * - Đi SÁT Knight hơn.
 * - Va chạm chính xác với tường và cửa đóng qua MapManager.isWalkable(...).
 * - Tự động Teleport khi Knight đi qua cửa room / cổng.
 */
public final class Pet {

    public enum State {
        IDLE,
        FOLLOWING
    }

    // =========================================================================
    // 1. Cấu hình khoảng cách: Giảm số liệu để Pet đi SÁT Knight hơn
    // =========================================================================
    private static final double FOLLOW_DISTANCE = 32.0;   // Khoảng cách bắt đầu đi theo (Cũ: 72)
    private static final double STOP_DISTANCE = 16.0;     // Khoảng cách dừng lại (Cũ: 38)
    private static final double TELEPORT_DISTANCE = 350.0; // Đi quá xa sẽ tự bay lại sát Knight

    private static final double SIDE_OFFSET = 18.0;       // Đứng sát hông Knight (Cũ: 48)
    private static final double BACK_OFFSET_Y = 8.0;      // Đứng hơi lệch phía sau 8px

    // Bán kính va chạm của Pet (truyền vào MapManager.isWalkable)
    private static final double PET_RADIUS = 8.0;

    private final PetType type;
    private final Image image;

    private double x;
    private double y;

    private double previousPlayerX;
    private boolean playerFacingRight = true;
    private boolean initializedPlayerPosition;

    private double idleTime;
    private double walkTime;

    private State state = State.IDLE;
    private boolean active = true;

    public Pet(PetType type, double spawnX, double spawnY) {
        if (type == null || !type.hasPet()) {
            throw new IllegalArgumentException(
                    "PetType phải khác null và khác PetType.NONE."
            );
        }

        this.type = type;
        this.x = spawnX;
        this.y = spawnY;
        this.previousPlayerX = spawnX;

        this.image = loadImage(type.getImagePath());
    }

    /**
     * Cập nhật vị trí Pet theo Knight VÀ kiểm tra va chạm bằng MapManager.
     *
     * @param deltaTime  thời gian giữa hai frame (giây)
     * @param playerX    tọa độ X của Knight
     * @param playerY    tọa độ Y của Knight
     * @param mapManager đối tượng MapManager quản lý bản đồ
     */
    public void update(
            double deltaTime,
            double playerX,
            double playerY,
            MapManager mapManager
    ) {
        if (!active || deltaTime <= 0) {
            return;
        }

        updatePlayerDirection(playerX);

        double targetX = calculateTargetX(playerX);
        double targetY = playerY + BACK_OFFSET_Y;

        double differenceX = targetX - x;
        double differenceY = targetY - y;

        double distance = Math.sqrt(differenceX * differenceX + differenceY * differenceY);

        /*
         * Nếu Knight đi quá xa (như đi qua Room khác),
         * Teleport ngay Pet về bên cạnh Knight.
         */
        if (distance >= TELEPORT_DISTANCE) {
            teleportNearPlayer(playerX, playerY);
            state = State.IDLE;
            return;
        }

        if (distance > FOLLOW_DISTANCE) {
            state = State.FOLLOWING;
            moveTowards(targetX, targetY, deltaTime, mapManager);
        } else if (distance <= STOP_DISTANCE) {
            state = State.IDLE;
        } else if (state == State.FOLLOWING) {
            moveTowards(targetX, targetY, deltaTime, mapManager);
        }

        idleTime += deltaTime;
        if (state == State.FOLLOWING) {
            walkTime += deltaTime;
        }
    }

    /**
     * Overload giữ lại phương thức cũ để tránh lỗi tương thích nếu chưa truyền mapManager.
     */
    public void update(double deltaTime, double playerX, double playerY) {
        update(deltaTime, playerX, playerY, null);
    }

    private void updatePlayerDirection(double playerX) {
        if (!initializedPlayerPosition) {
            previousPlayerX = playerX;
            initializedPlayerPosition = true;
            return;
        }

        double difference = playerX - previousPlayerX;

        if (difference > 0.5) {
            playerFacingRight = true;
        } else if (difference < -0.5) {
            playerFacingRight = false;
        }

        previousPlayerX = playerX;
    }

    private double calculateTargetX(double playerX) {
        if (playerFacingRight) {
            return playerX - SIDE_OFFSET;
        }

        return playerX + SIDE_OFFSET;
    }

    /**
     * Di chuyển Pet và kiểm tra va chạm tường/cửa bằng MapManager.
     */
    private void moveTowards(
            double targetX,
            double targetY,
            double deltaTime,
            MapManager mapManager
    ) {
        double differenceX = targetX - x;
        double differenceY = targetY - y;

        double distance = Math.sqrt(differenceX * differenceX + differenceY * differenceY);

        if (distance <= 0.001) {
            return;
        }

        double normalizedX = differenceX / distance;
        double normalizedY = differenceY / distance;

        double speedMultiplier = distance > 100 ? 1.35 : 1.0;
        double movement = type.getMoveSpeed() * speedMultiplier * deltaTime;

        movement = Math.min(movement, distance);

        double moveX = normalizedX * movement;
        double moveY = normalizedY * movement;

        if (mapManager == null) {
            x += moveX;
            y += moveY;
            return;
        }

        // 🔥 Tách riêng 2 trục X và Y để Pet trượt mượt mà dọc theo mép tường
        double nextX = x + moveX;
        if (mapManager.isWalkable(nextX, y, PET_RADIUS)) {
            x = nextX;
        }

        double nextY = y + moveY;
        if (mapManager.isWalkable(x, nextY, PET_RADIUS)) {
            y = nextY;
        }
    }

    /**
     * 🔥 Xử lý sự kiện Knight đi sang Room mới hoặc chuyển Portal.
     * Gọi hàm này khi Knight bước qua phòng mới để Pet đi theo ngay lập tức.
     */
    public void onRoomChanged(double newPlayerX, double newPlayerY) {
        teleportNearPlayer(newPlayerX, newPlayerY);
        state = State.IDLE;
    }

    private void teleportNearPlayer(double playerX, double playerY) {
        this.x = playerFacingRight ? playerX - SIDE_OFFSET : playerX + SIDE_OFFSET;
        this.y = playerY + BACK_OFFSET_Y;
    }

    public void teleport(double newX, double newY) {
        this.x = newX;
        this.y = newY;
    }

    // =========================================================================
    // RENDER METHODS
    // =========================================================================

    public void render(GraphicsContext graphicsContext, double cameraX, double cameraY) {
        if (!active || graphicsContext == null || image == null) {
            return;
        }

        double renderX = x - cameraX;
        double renderY = y - cameraY;

        if (state == State.IDLE) {
            renderY += Math.sin(idleTime * 4.2) * 2.2;
        }

        if (state == State.FOLLOWING) {
            renderY += Math.abs(Math.sin(walkTime * 10)) * -2.5;
        }

        double width = type.getRenderWidth();
        double height = type.getRenderHeight();

        graphicsContext.drawImage(
                image,
                renderX - width / 2.0,
                renderY - height,
                width,
                height
        );
    }

    public void render(GraphicsContext graphicsContext) {
        render(graphicsContext, 0, 0);
    }

    public void render(GraphicsContext graphicsContext, Camera camera) {
        if (!active || graphicsContext == null || camera == null || image == null) {
            return;
        }

        Vector2D screenPosition = camera.worldToScreen(new Vector2D(x, y));

        double renderX = screenPosition.getX();
        double renderY = screenPosition.getY();

        if (state == State.IDLE) {
            renderY += Math.sin(idleTime * 4.2) * 2.2;
        }

        if (state == State.FOLLOWING) {
            renderY -= Math.abs(Math.sin(walkTime * 10.0)) * 2.5;
        }

        double width = type.getRenderWidth();
        double height = type.getRenderHeight();

        graphicsContext.drawImage(
                image,
                renderX - width / 2.0,
                renderY - height,
                width,
                height
        );
    }

    // =========================================================================
    // GETTERS & SETTERS
    // =========================================================================

    private Image loadImage(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }

        URL resource = Pet.class.getResource(path);

        if (resource == null) {
            System.err.println("Không tìm thấy ảnh pet: " + path);
            return null;
        }

        return new Image(resource.toExternalForm(), false);
    }

    public PetType getType() {
        return type;
    }

    public State getState() {
        return state;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isFollowing() {
        return state == State.FOLLOWING;
    }

    public boolean isPlayerFacingRight() {
        return playerFacingRight;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}