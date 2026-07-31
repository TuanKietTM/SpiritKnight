package com.soulknight.map;

import javafx.scene.image.Image;

public final class Tile {

    private final TileType type;
    private double x;
    private double y;
    private double size;
    private Image customTexture;

    private static final Image FLOOR_IMAGE = new Image(Tile.class.getResourceAsStream("/assets/maps/floor.png"));
    private static final Image WALL_TOP_IMAGE = new Image(Tile.class.getResourceAsStream("/assets/maps/front_wall.png"));
    private static final Image WALL_FRONT_SHADOW_IMAGE = new Image(Tile.class.getResourceAsStream("/assets/maps/shawdown_wall.png"));
    private static final Image DOOR_OPEN_IMAGE = new Image(Tile.class.getResourceAsStream("/assets/maps/opendoor.png"));
    private static final Image DOOR_CLOSED_IMAGE = new Image(Tile.class.getResourceAsStream("/assets/maps/closedoor.png"));
    private static final Image BACK_IMAGE = new Image(Tile.class.getResourceAsStream("/assets/maps/back.png"));

    // Ảnh cho vật cản
    private static final Image BOX_IMAGE = new Image(Tile.class.getResourceAsStream("/assets/maps/wooden.png"));
    private static final Image TREE_IMAGE = new Image(Tile.class.getResourceAsStream("/assets/maps/tree.png"));
    private static final Image FIRE_IMAGE = new Image(Tile.class.getResourceAsStream("/assets/maps/fire.png"));

    public Tile(double x, double y, double size, TileType type) {
        this.x = x;
        this.y = y;
        this.size = size;
        this.type = type;
    }

    public Tile(double x, double y, double size, TileType type, Image customTexture) {
        this(x, y, size, type);
        this.customTexture = customTexture;
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

    /**
     * Set cac vat khong the di qua duoc
     */
    public boolean isWalkable() {
        return type != TileType.WALL
                && type != TileType.DOOR_CLOSED
                && type != TileType.OBSTACLE;
    }

    /**
     * Lấy Texture chuẩn tương ứng với từng TileType
     */
    public Image getTexture() {
        if (customTexture != null) {
            return customTexture;
        }

        return switch (type) {
            case FLOOR, SPAWN -> FLOOR_IMAGE;
            case WALL -> WALL_TOP_IMAGE;
            case DOOR_OPEN -> DOOR_OPEN_IMAGE;
            case DOOR_CLOSED -> DOOR_CLOSED_IMAGE;
            case BACK -> BACK_IMAGE;
            case OBSTACLE -> BOX_IMAGE;
            default -> null;
        };
    }

    public static Image getWallTopImage() { return WALL_TOP_IMAGE; }
    public static Image getWallFrontShadowImage() { return WALL_FRONT_SHADOW_IMAGE; }
    public static Image getDoorImage(boolean isClosed) { return isClosed ? DOOR_CLOSED_IMAGE : DOOR_OPEN_IMAGE; }

    // Getter cho các ảnh vật cản
    public static Image getBoxImage() { return BOX_IMAGE; }
    public static Image getTreeImage() { return TREE_IMAGE; }
    public static Image getFireImage() { return FIRE_IMAGE; }

    public enum TileType {
        FLOOR,
        WALL,
        SPAWN,
        DOOR_OPEN,
        DOOR_CLOSED,
        BACK,
        OBSTACLE,
        PORTAL,
        BOSS
    }
}