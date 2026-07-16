package com.soulknight.map;

import javafx.scene.paint.Color;

public final class Tile {

    private final TileType type;
    private double x;
    private double y;
    private double size;

    public Tile(double x, double y, double size, TileType type) {
        this.x = x;
        this.y = y;
        this.size = size;
        this.type = type;
    }

    public Tile(TileType type) {
        this.type = type;
    }

    public TileType getType() {
        return type;
    }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getSize() { return size; }

    public boolean isWalkable() {
        return type != TileType.WALL;
    }

    public Color getFillColor() {
        return switch (type) {
            case FLOOR -> Color.ORANGE;  // Màu nền tối
            case WALL -> Color.web("#0d1117");   // Màu tường đen kịt
            case SPAWN -> Color.web("#223d2f");  // Màu khu vực xuất phát
            case DOOR -> Color.web("#5c4033");   // Màu nâu của cửa gỗ
            case PORTAL -> Color.web("#ffd700"); // Màu vàng của cổng dịch chuyển
            case BOSS -> Color.BLUE;
        };
    }

    public enum TileType {
        FLOOR,
        WALL,
        SPAWN,
        DOOR,
        PORTAL,
        BOSS
    }
}
