package com.soulknight.map;

import javafx.scene.paint.Color;

public final class Tile {

    private final TileType type;

    public Tile(TileType type) {
        this.type = type;
    }

    public TileType getType() {
        return type;
    }

    public boolean isWalkable() {
        return type != TileType.WALL;
    }

    public Color getFillColor() {
        return switch (type) {
            case FLOOR -> Color.web("#1f2630");  // Màu nền tối
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
