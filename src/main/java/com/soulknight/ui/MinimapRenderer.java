package com.soulknight.ui;

import com.soulknight.engine.GameWorld;
import com.soulknight.entity.Enemy;
import com.soulknight.entity.Player;
import com.soulknight.map.MapManager;
import com.soulknight.map.Room;
import com.soulknight.utils.Vector2D;
import javafx.geometry.BoundingBox;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.List;

public class MinimapRenderer {

    private final Canvas canvas;
    // Tỉ lệ thu nhỏ tọa độ thế giới về Minimap
    private double scale = 0.08;

    public MinimapRenderer(Canvas canvas) {
        this.canvas = canvas;
    }

    public void setScale(double scale) {
        this.scale = scale;
    }

    public void render(GameWorld world, Player player) {
        if (canvas == null || player == null || world == null) return;

        GraphicsContext gc = canvas.getGraphicsContext2D();
        double mapWidth = canvas.getWidth();
        double mapHeight = canvas.getHeight();

        if (mapWidth <= 0) mapWidth = 140;
        if (mapHeight <= 0) mapHeight = 140;

        double centerX = mapWidth / 2.0;
        double centerY = mapHeight / 2.0;

        MapManager mapManager = world.getMapManager();
        if (mapManager == null) return;

        Vector2D playerPos = player.getPosition();
        if (playerPos == null) return;

        double playerX = playerPos.getX();
        double playerY = playerPos.getY();

        gc.clearRect(0, 0, mapWidth, mapHeight);
        gc.save();

        gc.setFill(Color.rgb(18, 22, 28, 0.85));
        gc.fillRoundRect(0, 0, mapWidth, mapHeight, 12, 12);

        // 2. VẼ CÁC ĐOẠN HÀNH LÀNG / ĐƯỜNG ĐI (Lớp dưới cùng)
        drawCorridors(gc, mapManager, playerX, playerY, centerX, centerY);

        // 3. LẤY VÀ VẼ CÁC PHÒNG (ROOMS)
        List<Room> rooms = mapManager.getRooms();
        Room currentRoom = null;

        if (rooms != null) {
            for (Room room : rooms) {
                if (room == null || room.getBound() == null) continue;

                BoundingBox bound = room.getBound();
                double roomWorldX = bound.getMinX();
                double roomWorldY = bound.getMinY();
                double roomWorldW = bound.getWidth();
                double roomWorldH = bound.getHeight();

                double drawX = centerX + (roomWorldX - playerX) * scale;
                double drawY = centerY + (roomWorldY - playerY) * scale;
                double drawW = roomWorldW * scale;
                double drawH = roomWorldH * scale;

                if (room.containsPosition(playerPos, 0)) {
                    currentRoom = room;
                }

                if (room.getState() == Room.RoomState.IN_PROGRESS) {
                    gc.setFill(Color.rgb(140, 50, 50, 0.85));
                } else if (room.getType() == Room.RoomType.BOSS) {
                    gc.setFill(Color.rgb(110, 60, 130, 0.85));
                } else {
                    gc.setFill(Color.rgb(80, 85, 95, 0.9));
                }

                gc.fillRect(drawX, drawY, drawW, drawH);
                drawDoors(gc, room, playerX, playerY, centerX, centerY);
            }
        }
        if (currentRoom != null) {
            BoundingBox curBound = currentRoom.getBound();
            double rX = centerX + (curBound.getMinX() - playerX) * scale;
            double rY = centerY + (curBound.getMinY() - playerY) * scale;
            double rW = curBound.getWidth() * scale;
            double rH = curBound.getHeight() * scale;
            drawFocusCorner(gc, rX, rY, rW, rH);
        }

        if (world.getEnemies() != null) {
            gc.setFill(Color.rgb(240, 40, 40, 0.95));
            for (Enemy enemy : world.getEnemies()) {
                if (enemy == null || !enemy.isAlive()) continue;

                Vector2D ePos = enemy.getPosition();
                if (ePos == null) continue;

                double drawX = centerX + (ePos.getX() - playerX) * scale;
                double drawY = centerY + (ePos.getY() - playerY) * scale;

                if (drawX >= 0 && drawX <= mapWidth && drawY >= 0 && drawY <= mapHeight) {
                    gc.fillOval(drawX - 2.5, drawY - 2.5, 5, 5);
                }
            }
        }

        drawPlayerIcon(gc, centerX, centerY);
        gc.setStroke(Color.rgb(60, 65, 75, 0.8));
        gc.setLineWidth(2.0);
        gc.strokeRoundRect(1, 1, mapWidth - 2, mapHeight - 2, 12, 12);

        gc.restore();
    }

    /**
     * Ve hang lang
     */
    private void drawCorridors(GraphicsContext gc, MapManager mapManager, double playerX, double playerY, double cx, double cy) {
        List<BoundingBox> corridors = mapManager.getCorridors();
        if (corridors == null || corridors.isEmpty()) return;
        gc.setFill(Color.rgb(60, 65, 75, 0.85));

        for (BoundingBox corridor : corridors) {
            if (corridor == null) continue;

            double drawX = cx + (corridor.getMinX() - playerX) * scale;
            double drawY = cy + (corridor.getMinY() - playerY) * scale;
            double drawW = corridor.getWidth() * scale;
            double drawH = corridor.getHeight() * scale;
            gc.fillRect(drawX, drawY, drawW + 0.5, drawH + 0.5);
        }
    }
    private void drawFocusCorner(GraphicsContext gc, double x, double y, double w, double h) {
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2.0);

        double len = Math.min(Math.min(w, h) * 0.25, 5.0);

        gc.strokeLine(x, y, x + len, y);
        gc.strokeLine(x, y, x, y + len);

        gc.strokeLine(x + w, y, x + w - len, y);
        gc.strokeLine(x + w, y, x + w, y + len);

        gc.strokeLine(x, y + h, x + len, y + h);
        gc.strokeLine(x, y + h, x, y + h - len);

        gc.strokeLine(x + w, y + h, x + w - len, y + h);
        gc.strokeLine(x + w, y + h, x + w, y + h - len);
    }

    /**
     * Ve bieu tuong player mau xanh la
     */
    private void drawPlayerIcon(GraphicsContext gc, double cx, double cy) {
        gc.setFill(Color.rgb(40, 230, 90));

        double[] xPoints = {cx - 4, cx, cx + 4, cx + 4, cx - 4};
        double[] yPoints = {cy, cy - 5, cy, cy + 4, cy + 4};
        gc.fillPolygon(xPoints, yPoints, 5);
    }

    /**
     * ve cua
     */
    private void drawDoors(GraphicsContext gc, Room room, double playerX, double playerY, double cx, double cy) {
        List<BoundingBox> doors = room.getDoors();
        if (doors == null || doors.isEmpty()) return;

        for (BoundingBox door : doors) {
            if (door == null) continue;

            double drawX = cx + (door.getMinX() - playerX) * scale;
            double drawY = cy + (door.getMinY() - playerY) * scale;
            double drawW = door.getWidth() * scale;
            double drawH = door.getHeight() * scale;

            // Đảm bảo kích thước tối thiểu là 3.0px để cửa không bị biến mất khi thu nhỏ
            double renderW = Math.max(drawW, 3.0);
            double renderH = Math.max(drawH, 3.0);

            if (room.isDoorsClosed()) {
                gc.setFill(Color.rgb(230, 90, 30, 0.95));
                gc.fillRect(drawX, drawY, renderW, renderH);
            }
        }
    }
}