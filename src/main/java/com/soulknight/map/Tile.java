package com.soulknight.map;

import javafx.scene.image.Image;

public final class Tile {

    private final TileType type;
    private double x;
    private double y;
    private double size;

    // Load tài nguyên hình ảnh
    private static final Image FLOOR_IMAGE = new Image(Tile.class.getResourceAsStream("/assets/maps/floor.png"));
    private static final Image WALL_TOP_IMAGE = new Image(Tile.class.getResourceAsStream("/assets/maps/front_wall.png"));
    private static final Image WALL_FRONT_SHADOW_IMAGE = new Image(Tile.class.getResourceAsStream("/assets/maps/shawdown_wall.png"));

    private static final Image DOOR_OPEN_IMAGE = new Image(Tile.class.getResourceAsStream("/assets/maps/open_door.png"));
    private static final Image DOOR_CLOSED_IMAGE = new Image(Tile.class.getResourceAsStream("/assets/maps/close_door.png"));

    public Tile(double x, double y, double size, TileType type) {
        this.x = x;
        this.y = y;
        this.size = size;
        this.type = type;
    }

    public Tile(TileType type) {
        this.type = type;
    }

    public TileType getType() { return type; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getSize() { return size; }

    // Tường (WALL) và Cửa đóng là chướng ngại vật không thể đi qua
    public boolean isWalkable() {
        return type != TileType.WALL && type != TileType.DOOR_CLOSED;
    }

    // Lấy texture mặc định theo TileType
    public Image getTexture() {
        return switch (type) {
            case FLOOR -> FLOOR_IMAGE;
            case WALL -> WALL_TOP_IMAGE; // Mặc định tường vẽ mặt trên
            case DOOR_OPEN -> DOOR_OPEN_IMAGE;
            case DOOR_CLOSED -> DOOR_CLOSED_IMAGE;
            default -> null;
        };
    }

    // Các hàm getter tĩnh lấy Image riêng biệt phục vụ cho Render 3 Lớp ở MapManager
    public static Image getWallTopImage() {
        return WALL_TOP_IMAGE;
    }

    public static Image getWallFrontShadowImage() {
        return WALL_FRONT_SHADOW_IMAGE;
    }

    public static Image getDoorImage(boolean isClosed) {
        return isClosed ? DOOR_CLOSED_IMAGE : DOOR_OPEN_IMAGE;
    }

    public enum TileType {
        FLOOR,
        WALL,         // Loại tường duy nhất trong JSON (định vị Hitbox & Bề mặt)
        SPAWN,
        DOOR_OPEN,
        DOOR_CLOSED,
        DOOR,
        PORTAL,
        BOSS
    }
}