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
            case FLOOR -> Color.web("#1f2630");
            case WALL -> Color.web("#0d1117");
            case SPAWN -> Color.web("#223d2f");
        };
    }

    public enum TileType {
        FLOOR,
        WALL,
        SPAWN
    }
}
