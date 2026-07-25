package com.soulknight.pet;

import com.soulknight.engine.GameWorld;
import com.soulknight.entity.Player;
import com.soulknight.map.Room;
import com.soulknight.utils.Vector2D;
import javafx.geometry.BoundingBox;

/**
 * Chịu trách nhiệm tìm vị trí an toàn và đưa Pet vào phòng.
 *
 * Room không tự teleport Pet.
 */
public final class PetRoomPlacementService {

    private static final double GRID_STEP = 20.0;

    private PetRoomPlacementService() {
    }

    public static boolean placePetInsideRoom(GameWorld gameWorld, Room room) {
        if (gameWorld == null || room == null || gameWorld.getCurrentPet() == null) {
            return false;
        }

        Player player = gameWorld.getPlayer();

        if (player == null || player.getPosition() == null) {
            return false;
        }

        double petRadius = gameWorld.getCurrentPet().getRadius();
        Vector2D playerPosition = player.getPosition();

        double[][] offsets = {
                {-42.0, 24.0},
                {42.0, 24.0},
                {-42.0, -24.0},
                {42.0, -24.0},
                {0.0, 48.0},
                {0.0, -48.0},
                {-65.0, 0.0},
                {65.0, 0.0}
        };

        // Trước tiên thử các vị trí gần Knight.
        for (double[] offset : offsets) {
            Vector2D candidate = playerPosition.copy().add(offset[0], offset[1]);

            if (!room.containsPosition(candidate, petRadius)) {
                continue;
            }

            if (!gameWorld.canMoveTo(candidate, petRadius)) {
                continue;
            }

            teleportPet(gameWorld, candidate);
            return true;
        }

        // Nếu quanh Knight không có chỗ, quét toàn bộ phòng.
        Vector2D safePosition = findSafePosition(gameWorld, room, petRadius);

        if (safePosition != null) {
            teleportPet(gameWorld, safePosition);
            return true;
        }
        teleportPet(gameWorld, playerPosition);
        return true;
    }

    private static Vector2D findSafePosition(GameWorld gameWorld, Room room, double petRadius) {
        BoundingBox bound = room.getBound();
        Vector2D playerPosition = gameWorld.getPlayer().getPosition();

        double minX = bound.getMinX() + petRadius;
        double minY = bound.getMinY() + petRadius;
        double maxX = bound.getMaxX() - petRadius;
        double maxY = bound.getMaxY() - petRadius;

        Vector2D bestPosition = null;
        double bestDistanceSquared = Double.MAX_VALUE;

        for (double y = minY; y <= maxY; y += GRID_STEP) {
            for (double x = minX; x <= maxX; x += GRID_STEP) {
                Vector2D candidate = new Vector2D(x, y);

                if (!gameWorld.canMoveTo(candidate, petRadius)) {
                    continue;
                }

                double distanceSquared = candidate.distanceSquared(playerPosition);

                if (distanceSquared < bestDistanceSquared) {
                    bestDistanceSquared = distanceSquared;
                    bestPosition = candidate;
                }
            }
        }

        return bestPosition;
    }

    private static void teleportPet(GameWorld gameWorld, Vector2D position) {
        gameWorld.getCurrentPet().teleport(position.getX(), position.getY());
    }
}