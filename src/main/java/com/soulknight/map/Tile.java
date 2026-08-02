package com.soulknight.map;

import javafx.scene.image.Image;

public final class Tile {

    private final TileType type;
    private double x;
    private double y;
    private double size;
    private Image customTexture;

    private static final Image FLOOR_1 = loadImage("/assets/maps/floor1.png");
    private static final Image FLOOR_2 = loadImage("/assets/maps/floor2.png");
    private static final Image FLOOR_3 = loadImage("/assets/maps/floor3.png");
    private static final Image FLOOR_4 = loadImage("/assets/maps/floor4.png");

    private static final Image WALL_TOP_IMAGE = loadImage("/assets/maps/wall.png");
    private static final Image WALL_FRONT_SHADOW_IMAGE = loadImage("/assets/maps/shawdown.png");
    private static final Image DOOR_OPEN_IMAGE = loadImage("/assets/maps/opendoor.png");
    private static final Image DOOR_CLOSED_IMAGE = loadImage("/assets/maps/closedoor.png");
    private static final Image BACK_IMAGE = loadImage("/assets/maps/back.png");

    // Anh vat can
    private static final Image BOX_IMAGE = loadImage("/assets/maps/wooden.png");
    private static final Image TREE_IMAGE = loadImage("/assets/maps/tree.png");
    private static final Image FIRE_IMAGE = loadImage("/assets/maps/fire.png");

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

    private static Image loadImage(String path) {
        var stream = Tile.class.getResourceAsStream(path);

        if (stream == null) {
            System.err.println("Khong tim thay asset: " + path);
            return null;
        }

        return new Image(stream);
    }

    public TileType getType() {
        return type;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getSize() {
        return size;
    }

    public boolean isWalkable() {
        return type != TileType.WALL
                && type != TileType.DOOR_CLOSED
                && type != TileType.OBSTACLE;
    }

    public Image getTexture() {
        if (customTexture != null) {
            return customTexture;
        }

        return switch (type) {
            case FLOOR, SPAWN -> FLOOR_1;
            case WALL -> WALL_TOP_IMAGE;
            case DOOR_OPEN -> DOOR_OPEN_IMAGE;
            case DOOR_CLOSED -> DOOR_CLOSED_IMAGE;
            case BACK -> BACK_IMAGE;
            case OBSTACLE -> BOX_IMAGE;
            default -> null;
        };
    }

    /**
     * Chon bien the floor theo toa do grid.
     * Cung mot toa do se luon cho cung mot texture.
     */
    public static Image getFloorImageByCoordinate(int gridX, int gridY) {
        long hash = gridX * 73856093L ^ gridY * 19349663L;
        int value = (int) Math.floorMod(hash, 100);

        /*
         * Ti le:
         * floor1: 55%
         * floor2: 20%
         * floor3: 15%
         * floor4: 10%
         */
        if (value < 55) {
            return FLOOR_1;
        }

        if (value < 75) {
            return FLOOR_2;
        }

        if (value < 90) {
            return FLOOR_3;
        }

        return FLOOR_4;
    }

    public static Image getFloor1Image() {
        return FLOOR_1;
    }

    public static Image getFloor2Image() {
        return FLOOR_2;
    }

    public static Image getFloor3Image() {
        return FLOOR_3;
    }

    public static Image getFloor4Image() {
        return FLOOR_4;
    }

    public static Image getWallTopImage() {
        return WALL_TOP_IMAGE;
    }

    public static Image getWallFrontShadowImage() {
        return WALL_FRONT_SHADOW_IMAGE;
    }

    public static Image getDoorImage(boolean isClosed) {
        return isClosed ? DOOR_CLOSED_IMAGE : DOOR_OPEN_IMAGE;
    }

    public static Image getBoxImage() {
        return BOX_IMAGE;
    }

    public static Image getTreeImage() {
        return TREE_IMAGE;
    }

    public static Image getFireImage() {
        return FIRE_IMAGE;
    }

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