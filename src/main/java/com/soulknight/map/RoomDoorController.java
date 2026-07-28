package com.soulknight.map;

import com.soulknight.engine.Camera;
import javafx.geometry.BoundingBox;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

/**
 * Quan li danh sach cua , trang thai va cham , dong mo cua
 */
public class RoomDoorController {

    private static final float DOOR_ANIM_SPEED = 6.0f; // toc do truot cua cua

    private final List<BoundingBox> doors = new ArrayList<>();
    private boolean isClosed = false;
    private float doorProgress = 0.0f;
    /**
     * Cập nhật tiến trình animation trượt cửa theo thời gian.
     */
    public void update(double deltaSeconds) {
        if (isClosed) {
            if (doorProgress < 1.0f) {
                doorProgress += (float) (DOOR_ANIM_SPEED * deltaSeconds);
                if (doorProgress > 1.0f) doorProgress = 1.0f;
            }
        } else {
            if (doorProgress > 0.0f) {
                doorProgress -= (float) (DOOR_ANIM_SPEED * deltaSeconds);
                if (doorProgress < 0.0f) doorProgress = 0.0f;
            }
        }
    }

    /**
     * Vẽ duy nhất 1 ô cửa (BoundingBox) phục vụ cho hệ thống Y-Sorting 2.5D.
     */
    public void renderSingleDoor(GraphicsContext graphicsContext, Camera camera, BoundingBox door, double tileSize) {
        if (graphicsContext == null || camera == null || tileSize <= 0.0 || door == null) {
            return;
        }

        Image closedDoorTexture = Tile.getDoorImage(true);
        Image wallShadowImage = Tile.getWallFrontShadowImage();
        double zoom = camera.getZoom();
        double renderTileSize = tileSize * zoom;

        double maxOffsetY = renderTileSize;
        double currentOffsetY = maxOffsetY * (1.0 - doorProgress);

        int tilesX = Math.max(1, (int) Math.round(door.getWidth() / tileSize));
        int tilesY = Math.max(1, (int) Math.round(door.getHeight() / tileSize));
        boolean isHorizontalDoor = door.getWidth() > door.getHeight();

        for (int tileY = 0; tileY < tilesY; tileY++) {
            for (int tileX = 0; tileX < tilesX; tileX++) {
                double worldX = door.getMinX() + tileX * tileSize;
                double worldY = door.getMinY() + tileY * tileSize;
                double screenX = camera.worldToScreenX(worldX);
                double screenY = camera.worldToScreenY(worldY);

                // 1. VẼ BÓNG TƯỜNG CHE CỬA (khi cửa trượt xuống lòng đất)
                boolean shouldDrawWallShadow = isHorizontalDoor && (tileY == 0);
                if (shouldDrawWallShadow && wallShadowImage != null && doorProgress < 0.99f) {
                    graphicsContext.setGlobalAlpha(1.0 - doorProgress);
                    graphicsContext.drawImage(wallShadowImage, screenX, screenY, renderTileSize, renderTileSize);
                    graphicsContext.setGlobalAlpha(1.0);
                }

                // 2. VẼ BÓNG ĐỔ NỀN 2.5D (Hắt xuống sàn phía dưới cọc cửa)
                if (doorProgress > 0.05f) {
                    // Tăng chiều cao bóng để dễ nhìn thấy trên nền tối (khoảng 20-25% kích thước tile)
                    double shadowHeight = renderTileSize * 0.25 * doorProgress;
                    double shadowAlpha = 0.45 * doorProgress; // Tăng độ đậm của bóng

                    graphicsContext.setGlobalAlpha(shadowAlpha);
                    graphicsContext.setFill(Color.BLACK);

                    // Đặt bóng trôi ngay bên dưới chân tile cửa để không bị sprite cửa che mất
                    graphicsContext.fillRect(
                            screenX,
                            screenY + renderTileSize - (shadowHeight * 0.5),
                            renderTileSize,
                            shadowHeight
                    );
                    graphicsContext.setGlobalAlpha(1.0);
                }

                // 3. VẼ CỌC CỬA TRƯỢT TỪ DƯỚI LÊN
                if (closedDoorTexture != null && doorProgress > 0.01f) {
                    double doorRenderY = screenY + currentOffsetY;

                    graphicsContext.save();
                    graphicsContext.beginPath();
                    graphicsContext.rect(screenX, screenY, renderTileSize, renderTileSize);
                    graphicsContext.clip();

                    // Vẽ sprite cửa
                    graphicsContext.drawImage(closedDoorTexture, screenX, doorRenderY, renderTileSize, renderTileSize);

                    // (Tùy chọn) Phủ một lớp bóng tối nhẹ lên chính mặt cửa khi cửa chìm dưới đất
                    if (doorProgress < 0.95f) {
                        graphicsContext.setFill(Color.rgb(0, 0, 0, (1.0 - doorProgress) * 0.5));
                        graphicsContext.fillRect(screenX, screenY, renderTileSize, renderTileSize);
                    }

                    graphicsContext.restore();
                }
            }
        }
    }
    /**
     * Thêm vị trí cửa (tránh trùng lặp).
     */
    public void addDoorCoordinate(double x, double y, double width, double height) {
        for (BoundingBox door : doors) {
            boolean sameX = Double.compare(door.getMinX(), x) == 0;
            boolean sameY = Double.compare(door.getMinY(), y) == 0;
            boolean sameWidth = Double.compare(door.getWidth(), width) == 0;
            boolean sameHeight = Double.compare(door.getHeight(), height) == 0;

            if (sameX && sameY && sameWidth && sameHeight) {
                return;
            }
        }
        doors.add(new BoundingBox(x, y, width, height));
    }

    /**
     * Kiểm tra vị trí cửa có thuộc khu vực vùng đệm của phòng không.
     */
    public boolean isDoorBelongsToRoom(BoundingBox roomBound, double doorX, double doorY, double doorWidth, double doorHeight) {
        BoundingBox doorBox = new BoundingBox(doorX, doorY, doorWidth, doorHeight);
        BoundingBox expandedBound = new BoundingBox(
                roomBound.getMinX() - 10.0,
                roomBound.getMinY() - 10.0,
                roomBound.getWidth() + 20.0,
                roomBound.getHeight() + 20.0
        );
        return expandedBound.intersects(doorBox);
    }

    /**
     * Kiểm tra va chạm vật lý với cửa khi đang đóng.
     */
    public boolean isHitClosedDoor(double worldX, double worldY, double radius) {
        if (!isClosed) {
            return false;
        }

        double safeRadius = Math.max(0.0, radius);
        double diameter = safeRadius * 2.0;

        for (BoundingBox door : doors) {
            if (door.intersects(worldX - safeRadius, worldY - safeRadius, diameter, diameter)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Kiểm tra xem tile cụ thể có cửa đang đóng hay không (dùng cho pathfinding A*).
     */
    public boolean isDoorClosedAtTile(int tileX, int tileY, double tileSize) {
        if (this.doorProgress <= 0.05f) {
            return false; // Cửa đang mở
        }

        double targetWorldX = tileX * tileSize;
        double targetWorldY = tileY * tileSize;

        for (BoundingBox door : doors) {
            double minX = door.getMinX();
            double maxX = door.getMaxX();
            double minY = door.getMinY();
            double maxY = door.getMaxY();

            if (targetWorldX >= minX && targetWorldX < maxX &&
                    targetWorldY >= minY && targetWorldY < maxY) {
                return true;
            }
        }
        return false;
    }

    // --- GETTERS & SETTERS ---

    public boolean isClosed() {
        return isClosed;
    }

    public void setClosed(boolean closed) {
        this.isClosed = closed;
    }

    public List<BoundingBox> getDoors() {
        return doors;
    }

    public float getDoorProgress() {
        return doorProgress;
    }
}