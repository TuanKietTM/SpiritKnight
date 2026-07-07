package com.soulknight.map;

import com.soulknight.utils.Constants;
import java.util.Random;

public final class DungeonGenerator {

    public Tile[][] generate(int width, int height, Random random) {
        Tile[][] tiles = new Tile[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Tile.TileType type = Tile.TileType.FLOOR;
                if (x == 0 || y == 0 || x == width - 1 || y == height - 1) {
                    type = Tile.TileType.WALL;
                } else if (random.nextDouble() < 0.12 && !isInSafeZone(x, y, width, height)) {
                    type = Tile.TileType.WALL;
                }
                tiles[y][x] = new Tile(type);
            }
        }

        int spawnX = width / 2;
        int spawnY = height / 2;
        carveSquare(tiles, spawnX, spawnY, 2, Tile.TileType.SPAWN);
        carveSquare(tiles, Math.max(2, width - 4), Math.max(2, height - 4), 2, Tile.TileType.SPAWN);

        return tiles;
    }

    private void carveSquare(Tile[][] tiles, int centerX, int centerY, int radius, Tile.TileType type) {
        for (int y = Math.max(1, centerY - radius); y <= Math.min(tiles.length - 2, centerY + radius); y++) {
            for (int x = Math.max(1, centerX - radius); x <= Math.min(tiles[0].length - 2, centerX + radius); x++) {
                tiles[y][x] = new Tile(type);
            }
        }
    }

    private boolean isInSafeZone(int x, int y, int width, int height) {
        int centerX = width / 2;
        int centerY = height / 2;
        return Math.abs(x - centerX) <= Constants.SAFE_ZONE_RADIUS && Math.abs(y - centerY) <= Constants.SAFE_ZONE_RADIUS;
    }
}
