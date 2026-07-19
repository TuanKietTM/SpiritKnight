package com.soulknight.map;


import javafx.scene.image.Image;
import javafx.scene.paint.Paint;

public final class Tile {

    private final TileType type;
    private double x;
    private double y;
    private double size;
    private static final Image FLOOR_IMAGE = new Image(Tile.class.getResourceAsStream("/assets/maps/floor.png"));
    private static final Image WALL_IMAGE = new Image(Tile.class.getResourceAsStream("/assets/maps/wall.png"));

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

    public Image getTexture() {
        return switch (type) {
            case FLOOR -> FLOOR_IMAGE;
            case WALL -> WALL_IMAGE;
            default -> null; // Các ô khác tạm thời chưa có ảnh, trả về null
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
