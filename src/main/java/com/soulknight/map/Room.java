package com.soulknight.map;

import com.soulknight.engine.Camera;
import com.soulknight.engine.GameWorld;
import com.soulknight.entity.Enemy;
import com.soulknight.entity.Player;
import com.soulknight.pet.Pet;
import com.soulknight.utils.Vector2D;
import javafx.geometry.BoundingBox;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Room {

    private static final double PET_ENTRY_TIMEOUT = 1.8;
    private static final double PLAYER_PUSH_DISTANCE = 18.0;
    private static final double NEXT_WAVE_DELAY = 1.0;

    private static final int MAX_OBSTACLES = 3;
    private static final int MAX_RANDOM_ATTEMPTS = 160;

    private static final double OBSTACLE_SIZE = 40.0;
    private static final double WALL_PADDING = 30.0;
    private static final double OBSTACLE_SPACING = 20.0;
    private static final double PLAYER_SAFE_PADDING = 30.0;
    private static final double PET_SAFE_PADDING = 18.0;
    private static final double DOOR_SAFE_PADDING = 24.0;
    private static final double GRID_STEP = 18.0;

    private static final double RELAXED_WALL_PADDING = 16.0;
    private static final double RELAXED_OBSTACLE_SPACING = 6.0;
    private static final double RELAXED_PLAYER_PADDING = 14.0;
    private static final double RELAXED_PET_PADDING = 8.0;
    private static final double RELAXED_DOOR_PADDING = 12.0;

    private final String name;
    private final BoundingBox bound;
    private final List<BoundingBox> doors = new ArrayList<>();
    private final List<Obstacle> obstacles = new ArrayList<>();

    private boolean isActived;
    private boolean isCleared;
    private boolean isDoorClosed;

    private boolean activationPending;
    private double petEntryWaitTimer;

    private boolean obstaclesGenerated;

    private int currentWave;
    private int maxWaves = 2;
    private double waveDelayTimer;
    private boolean isWaitingForNextWave;

    public Room(String name, double x, double y, double width, double height) {
        this.name = name;
        this.bound = new BoundingBox(x, y, width, height);

        if (isStartRoom()) {
            isActived = false;
            isCleared = true;
            isDoorClosed = false;
            maxWaves = 0;
        } else if ("BossRoom".equalsIgnoreCase(name)) {
            maxWaves = 1;
        }
    }

    public void update(GameWorld gameWorld, Player player, List<Enemy> globalEnemies, double deltaSeconds) {
        if (isCleared || gameWorld == null || player == null || player.getPosition() == null) {
            return;
        }

        Vector2D playerPosition = player.getPosition();

        if (!isActived && !activationPending && bound.contains(playerPosition.getX(), playerPosition.getY())) {
            beginRoomActivation(player);
        }

        if (activationPending) {
            updatePendingActivation(gameWorld, player, deltaSeconds);
            return;
        }

        if (!isActived) {
            return;
        }

        updateWaves(gameWorld, deltaSeconds);
        checkRoomClear(globalEnemies);
    }

    private void beginRoomActivation(Player player) {
        activationPending = true;
        petEntryWaitTimer = PET_ENTRY_TIMEOUT;
        isDoorClosed = false;

        pushPlayerInside(player);
    }

    private void updatePendingActivation(GameWorld gameWorld, Player player, double deltaSeconds) {
        Vector2D playerPosition = player.getPosition();

        if (!bound.contains(playerPosition.getX(), playerPosition.getY())) {
            cancelPendingActivation();
            return;
        }

        Pet pet = gameWorld.getCurrentPet();

        if (pet == null || pet.getPosition() == null) {
            completeRoomActivation(gameWorld);
            return;
        }

        if (isPositionSafelyInsideRoom(pet.getPosition(), pet.getRadius())) {
            completeRoomActivation(gameWorld);
            return;
        }

        petEntryWaitTimer -= Math.max(0.0, deltaSeconds);

        if (petEntryWaitTimer <= 0.0) {
            gameWorld.placePetInsideRoom(this);
            completeRoomActivation(gameWorld);
        }
    }

    private void cancelPendingActivation() {
        activationPending = false;
        petEntryWaitTimer = 0.0;
        isDoorClosed = false;
    }

    private void completeRoomActivation(GameWorld gameWorld) {
        activationPending = false;
        petEntryWaitTimer = 0.0;

        isActived = true;
        isDoorClosed = true;

        currentWave = 1;
        waveDelayTimer = 0.0;
        isWaitingForNextWave = false;

        generateObstacles(new Random(), gameWorld);
        gameWorld.spawnEnemiesInRoom(this, currentWave);
    }

    private void updateWaves(GameWorld gameWorld, double deltaSeconds) {
        if (!isWaitingForNextWave) {
            return;
        }

        waveDelayTimer -= Math.max(0.0, deltaSeconds);

        if (waveDelayTimer > 0.0) {
            return;
        }

        isWaitingForNextWave = false;
        currentWave++;

        gameWorld.spawnEnemiesInRoom(this, currentWave);
    }

    private void checkRoomClear(List<Enemy> globalEnemies) {
        if (globalEnemies == null) {
            return;
        }

        long aliveEnemiesInRoom = globalEnemies.stream()
                .filter(enemy -> enemy != null && enemy.isAlive() && enemy.getPosition() != null)
                .filter(enemy -> bound.contains(enemy.getPosition().getX(), enemy.getPosition().getY()))
                .count();

        if (aliveEnemiesInRoom > 0 || isWaitingForNextWave) {
            return;
        }

        if (currentWave < maxWaves) {
            isWaitingForNextWave = true;
            waveDelayTimer = NEXT_WAVE_DELAY;
            return;
        }

        isActived = false;
        isCleared = true;
        isDoorClosed = false;
        activationPending = false;
        petEntryWaitTimer = 0.0;
    }

    public void generateObstacles(Random random, GameWorld gameWorld) {
        if (random == null || isStartRoom() || obstaclesGenerated) {
            return;
        }

        obstaclesGenerated = true;
        obstacles.clear();

        Player player = gameWorld != null ? gameWorld.getPlayer() : null;
        Pet pet = gameWorld != null ? gameWorld.getCurrentPet() : null;

        generateRandomObstacles(random, player, pet);
        generateGridObstacles(random, player, pet, false);

        if (obstacles.isEmpty()) {
            generateGridObstacles(random, player, pet, true);
        }

        if (obstacles.isEmpty()) {
            generateFallbackObstacle(random, player, pet);
        }
    }

    private void generateRandomObstacles(Random random, Player player, Pet pet) {
        int attempts = 0;

        while (obstacles.size() < MAX_OBSTACLES && attempts < MAX_RANDOM_ATTEMPTS) {
            attempts++;

            BoundingBox candidate = createRandomObstacleBox(random, false);

            if (candidate == null || !isValidObstaclePosition(candidate, player, pet, false)) {
                continue;
            }

            addObstacle(candidate, random);
        }
    }

    private void generateGridObstacles(Random random, Player player, Pet pet, boolean relaxed) {
        if (obstacles.size() >= MAX_OBSTACLES) {
            return;
        }

        double padding = relaxed ? RELAXED_WALL_PADDING : WALL_PADDING;
        double minX = bound.getMinX() + padding;
        double minY = bound.getMinY() + padding;
        double maxX = bound.getMaxX() - padding - OBSTACLE_SIZE;
        double maxY = bound.getMaxY() - padding - OBSTACLE_SIZE;

        if (maxX < minX || maxY < minY) {
            return;
        }

        List<BoundingBox> candidates = new ArrayList<>();

        for (double y = minY; y <= maxY; y += GRID_STEP) {
            for (double x = minX; x <= maxX; x += GRID_STEP) {
                BoundingBox candidate = new BoundingBox(x, y, OBSTACLE_SIZE, OBSTACLE_SIZE);

                if (isValidObstaclePosition(candidate, player, pet, relaxed)) {
                    candidates.add(candidate);
                }
            }
        }

        Collections.shuffle(candidates, random);

        for (BoundingBox candidate : candidates) {
            if (obstacles.size() >= MAX_OBSTACLES) {
                break;
            }

            if (isValidObstaclePosition(candidate, player, pet, relaxed)) {
                addObstacle(candidate, random);
            }
        }
    }

    private void generateFallbackObstacle(Random random, Player player, Pet pet) {
        List<BoundingBox> candidates = createFallbackCandidates();

        BoundingBox bestCandidate = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (BoundingBox candidate : candidates) {
            if (!isBoxInsideRoom(candidate)) {
                continue;
            }

            if (overlapsExistingObstacle(candidate, true) || isNearDoor(candidate, true)) {
                continue;
            }

            double score = calculateSafetyScore(candidate, player, pet);

            if (score > bestScore) {
                bestScore = score;
                bestCandidate = candidate;
            }
        }

        if (bestCandidate != null && isValidObstaclePosition(bestCandidate, player, pet, true)) {
            addObstacle(bestCandidate, random);
        }
    }

    private List<BoundingBox> createFallbackCandidates() {
        List<BoundingBox> candidates = new ArrayList<>();

        double left = bound.getMinX() + RELAXED_WALL_PADDING;
        double right = bound.getMaxX() - RELAXED_WALL_PADDING - OBSTACLE_SIZE;
        double top = bound.getMinY() + RELAXED_WALL_PADDING;
        double bottom = bound.getMaxY() - RELAXED_WALL_PADDING - OBSTACLE_SIZE;

        double centerX = bound.getMinX() + (bound.getWidth() - OBSTACLE_SIZE) / 2.0;
        double centerY = bound.getMinY() + (bound.getHeight() - OBSTACLE_SIZE) / 2.0;

        candidates.add(new BoundingBox(left, top, OBSTACLE_SIZE, OBSTACLE_SIZE));
        candidates.add(new BoundingBox(right, top, OBSTACLE_SIZE, OBSTACLE_SIZE));
        candidates.add(new BoundingBox(left, bottom, OBSTACLE_SIZE, OBSTACLE_SIZE));
        candidates.add(new BoundingBox(right, bottom, OBSTACLE_SIZE, OBSTACLE_SIZE));
        candidates.add(new BoundingBox(centerX, centerY, OBSTACLE_SIZE, OBSTACLE_SIZE));
        candidates.add(new BoundingBox(centerX, top, OBSTACLE_SIZE, OBSTACLE_SIZE));
        candidates.add(new BoundingBox(centerX, bottom, OBSTACLE_SIZE, OBSTACLE_SIZE));
        candidates.add(new BoundingBox(left, centerY, OBSTACLE_SIZE, OBSTACLE_SIZE));
        candidates.add(new BoundingBox(right, centerY, OBSTACLE_SIZE, OBSTACLE_SIZE));

        return candidates;
    }

    private double calculateSafetyScore(BoundingBox candidate, Player player, Pet pet) {
        double centerX = candidate.getMinX() + candidate.getWidth() / 2.0;
        double centerY = candidate.getMinY() + candidate.getHeight() / 2.0;

        double score = 0.0;

        if (player != null && player.getPosition() != null) {
            double dx = centerX - player.getPosition().getX();
            double dy = centerY - player.getPosition().getY();
            score += Math.sqrt(dx * dx + dy * dy);
        }

        if (pet != null && pet.getPosition() != null) {
            double dx = centerX - pet.getPosition().getX();
            double dy = centerY - pet.getPosition().getY();
            score += Math.sqrt(dx * dx + dy * dy) * 0.75;
        }

        for (BoundingBox door : doors) {
            double doorCenterX = door.getMinX() + door.getWidth() / 2.0;
            double doorCenterY = door.getMinY() + door.getHeight() / 2.0;
            double dx = centerX - doorCenterX;
            double dy = centerY - doorCenterY;

            score += Math.sqrt(dx * dx + dy * dy) * 0.5;
        }

        return score;
    }

    private BoundingBox createRandomObstacleBox(Random random, boolean relaxed) {
        double padding = relaxed ? RELAXED_WALL_PADDING : WALL_PADDING;
        double minX = bound.getMinX() + padding;
        double minY = bound.getMinY() + padding;
        double maxX = bound.getMaxX() - padding - OBSTACLE_SIZE;
        double maxY = bound.getMaxY() - padding - OBSTACLE_SIZE;

        if (maxX < minX || maxY < minY) {
            return null;
        }

        double x = minX + random.nextDouble() * Math.max(1.0, maxX - minX);
        double y = minY + random.nextDouble() * Math.max(1.0, maxY - minY);

        return new BoundingBox(x, y, OBSTACLE_SIZE, OBSTACLE_SIZE);
    }

    private boolean isValidObstaclePosition(BoundingBox candidate, Player player, Pet pet, boolean relaxed) {
        if (candidate == null || !isBoxInsideRoom(candidate)) {
            return false;
        }

        if (overlapsExistingObstacle(candidate, relaxed) || isNearDoor(candidate, relaxed)) {
            return false;
        }

        double playerPadding = relaxed ? RELAXED_PLAYER_PADDING : PLAYER_SAFE_PADDING;
        double petPadding = relaxed ? RELAXED_PET_PADDING : PET_SAFE_PADDING;

        if (player != null && player.getPosition() != null) {
            double safeRadius = player.getRadius() + playerPadding;

            if (rectangleIntersectsCircle(candidate, player.getPosition(), safeRadius)) {
                return false;
            }
        }

        if (pet != null && pet.getPosition() != null) {
            double safeRadius = pet.getRadius() + petPadding;

            if (rectangleIntersectsCircle(candidate, pet.getPosition(), safeRadius)) {
                return false;
            }
        }

        return true;
    }

    private boolean isBoxInsideRoom(BoundingBox box) {
        return box != null
                && box.getMinX() >= bound.getMinX()
                && box.getMaxX() <= bound.getMaxX()
                && box.getMinY() >= bound.getMinY()
                && box.getMaxY() <= bound.getMaxY();
    }

    private boolean overlapsExistingObstacle(BoundingBox candidate, boolean relaxed) {
        double spacing = relaxed ? RELAXED_OBSTACLE_SPACING : OBSTACLE_SPACING;

        for (Obstacle obstacle : obstacles) {
            if (obstacle == null || obstacle.isDestroyed() || obstacle.getPosition() == null) {
                continue;
            }

            BoundingBox expandedObstacle = new BoundingBox(
                    obstacle.getPosition().getX() - spacing,
                    obstacle.getPosition().getY() - spacing,
                    obstacle.getWidth() + spacing * 2.0,
                    obstacle.getHeight() + spacing * 2.0
            );

            if (expandedObstacle.intersects(candidate)) {
                return true;
            }
        }

        return false;
    }

    private boolean isNearDoor(BoundingBox candidate, boolean relaxed) {
        double padding = relaxed ? RELAXED_DOOR_PADDING : DOOR_SAFE_PADDING;

        for (BoundingBox door : doors) {
            BoundingBox expandedDoor = new BoundingBox(
                    door.getMinX() - padding,
                    door.getMinY() - padding,
                    door.getWidth() + padding * 2.0,
                    door.getHeight() + padding * 2.0
            );

            if (expandedDoor.intersects(candidate)) {
                return true;
            }
        }

        return false;
    }

    private boolean rectangleIntersectsCircle(BoundingBox rectangle, Vector2D circlePosition, double circleRadius) {
        if (rectangle == null || circlePosition == null) {
            return false;
        }

        double closestX = Math.max(rectangle.getMinX(), Math.min(circlePosition.getX(), rectangle.getMaxX()));
        double closestY = Math.max(rectangle.getMinY(), Math.min(circlePosition.getY(), rectangle.getMaxY()));

        double differenceX = circlePosition.getX() - closestX;
        double differenceY = circlePosition.getY() - closestY;

        return differenceX * differenceX + differenceY * differenceY < circleRadius * circleRadius;
    }

    private void addObstacle(BoundingBox candidate, Random random) {
        Vector2D position = new Vector2D(candidate.getMinX(), candidate.getMinY());
        boolean destructible = random.nextDouble() > 0.2;

        obstacles.add(new Obstacle(position, OBSTACLE_SIZE, OBSTACLE_SIZE, 30, destructible));
    }

    private void pushPlayerInside(Player player) {
        if (player == null || player.getPosition() == null) {
            return;
        }

        Vector2D position = player.getPosition();

        double centerX = bound.getMinX() + bound.getWidth() / 2.0;
        double centerY = bound.getMinY() + bound.getHeight() / 2.0;

        double differenceX = centerX - position.getX();
        double differenceY = centerY - position.getY();
        double length = Math.sqrt(differenceX * differenceX + differenceY * differenceY);

        if (length <= 0.0001) {
            return;
        }

        double pushX = differenceX / length * PLAYER_PUSH_DISTANCE;
        double pushY = differenceY / length * PLAYER_PUSH_DISTANCE;

        position.add(pushX, pushY);
    }

    private boolean isPositionSafelyInsideRoom(Vector2D position, double radius) {
        if (position == null) {
            return false;
        }

        double safeRadius = Math.max(0.0, radius);

        double minX = bound.getMinX() + safeRadius;
        double maxX = bound.getMaxX() - safeRadius;
        double minY = bound.getMinY() + safeRadius;
        double maxY = bound.getMaxY() - safeRadius;

        return position.getX() >= minX && position.getX() <= maxX
                && position.getY() >= minY && position.getY() <= maxY;
    }

    public void renderDoors(GraphicsContext graphicsContext, Camera camera, double tileSize) {
        if (graphicsContext == null || camera == null || tileSize <= 0.0) {
            return;
        }

        Image doorTexture = Tile.getDoorImage(isDoorClosed);

        if (doorTexture == null) {
            return;
        }

        double zoom = camera.getZoom();

        for (BoundingBox door : doors) {
            int tilesX = Math.max(1, (int) Math.round(door.getWidth() / tileSize));
            int tilesY = Math.max(1, (int) Math.round(door.getHeight() / tileSize));

            for (int tileY = 0; tileY < tilesY; tileY++) {
                for (int tileX = 0; tileX < tilesX; tileX++) {
                    double worldX = door.getMinX() + tileX * tileSize;
                    double worldY = door.getMinY() + tileY * tileSize;
                    double screenX = camera.worldToScreenX(worldX);
                    double screenY = camera.worldToScreenY(worldY);

                    graphicsContext.drawImage(doorTexture, screenX, screenY, tileSize * zoom, tileSize * zoom);
                }
            }
        }
    }

    public boolean isDoorBelongsToRoom(double doorX, double doorY, double doorWidth, double doorHeight) {
        BoundingBox doorBox = new BoundingBox(doorX, doorY, doorWidth, doorHeight);
        BoundingBox expandedBound = new BoundingBox(bound.getMinX() - 10.0, bound.getMinY() - 10.0, bound.getWidth() + 20.0, bound.getHeight() + 20.0);

        return expandedBound.intersects(doorBox);
    }

    public void addDoorCoordinate(double x, double y, double width, double height) {
        for (BoundingBox door : doors) {
            boolean sameX = Double.compare(door.getMinX(), x) == 0;
            boolean sameY = Double.compare(door.getMinY(), y) == 0;

            if (sameX && sameY) {
                return;
            }
        }

        doors.add(new BoundingBox(x, y, width, height));
    }

    public boolean isHitClosedDoor(double worldX, double worldY, double radius) {
        if (!isDoorClosed) {
            return false;
        }

        double diameter = radius * 2.0;

        for (BoundingBox door : doors) {
            if (door.intersects(worldX - radius, worldY - radius, diameter, diameter)) {
                return true;
            }
        }

        return false;
    }

    public boolean isPlayerInside(double playerX, double playerY) {
        return bound.contains(playerX, playerY);
    }

    public boolean isPlayerInside(Player player) {
        return player != null && player.getPosition() != null
                && isPlayerInside(player.getPosition().getX(), player.getPosition().getY());
    }

    public boolean isPetInside(Pet pet) {
        return pet != null && pet.getPosition() != null
                && isPositionSafelyInsideRoom(pet.getPosition(), pet.getRadius());
    }

    private boolean isStartRoom() {
        return name != null && (name.equalsIgnoreCase("StartRoom") || name.toLowerCase().contains("spawn"));
    }

    public String getName() {
        return name;
    }

    public BoundingBox getBound() {
        return bound;
    }

    public List<Obstacle> getObstacles() {
        return obstacles;
    }

    public List<BoundingBox> getDoors() {
        return doors;
    }

    public boolean isActived() {
        return isActived;
    }

    public boolean isCleared() {
        return isCleared;
    }

    public boolean isDoorClosed() {
        return isDoorClosed;
    }

    public boolean isActivationPending() {
        return activationPending;
    }

    public double getPetEntryWaitTimer() {
        return petEntryWaitTimer;
    }

    public boolean isObstaclesGenerated() {
        return obstaclesGenerated;
    }
}