package com.soulknight.map;

import com.soulknight.engine.Camera;
import com.soulknight.engine.GameWorld;
import com.soulknight.entity.Enemy;
import com.soulknight.entity.Player;
import com.soulknight.pet.PetRoomEntryController;
import com.soulknight.pet.PetRoomInfo;
import com.soulknight.utils.Vector2D;
import javafx.geometry.BoundingBox;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Đại diện cho một phòng trong bản đồ.
 *
 * Chịu trách nhiệm:
 * - Theo dõi trạng thái phòng (Start, Fight, Rest, Boss).
 * - Điều khiển cửa phòng và kiểm tra va chạm cửa đóng.
 * - Quản lý các wave quái.
 * - Sinh và quản lý vật cản (Obstacles).
 */
public class Room {

    public enum RoomType {
        START, FIGHT, REST, BOSS
    }

    public enum RoomState {
        NOT_STARTED, IN_PROGRESS, CLEARED
    }

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

    // --- BIẾN QUẢN LÝ ANIMATION & BÓNG CỬA 2.5D ---
    private float doorProgress = 0.0f; // 0.0f = Mở hoàn toàn (rút xuống), 1.0f = Đóng hoàn toàn (nhô lên)
    private static final float DOOR_ANIM_SPEED = 6.0f; // Tốc độ trượt cửa

    private final String name;
    private final BoundingBox bound;
    private final RoomType type;
    private final List<BoundingBox> doors = new ArrayList<>();
    private final List<Obstacle> obstacles = new ArrayList<>();
    private final PetRoomEntryController petEntryController = new PetRoomEntryController();

    private RoomState state = RoomState.NOT_STARTED;
    private boolean isDoorClosed = false;
    private boolean obstaclesGenerated = false;

    private int currentWave;
    private int maxWaves = 2;
    private double waveDelayTimer;
    private boolean isWaitingForNextWave;

    public Room(String name, double x, double y, double width, double height) {
        this.name = name;
        this.bound = new BoundingBox(x, y, width, height);

        // Xác định RoomType ngay từ tên
        if (isStartRoom()) {
            this.type = RoomType.START;
            this.state = RoomState.CLEARED;
            this.isDoorClosed = false;
            this.maxWaves = 0;
        } else if (name != null && name.toLowerCase().contains("rest")) {
            this.type = RoomType.REST;
            this.state = RoomState.CLEARED;
            this.isDoorClosed = false;
            this.maxWaves = 0;
        } else if (name != null && name.toLowerCase().contains("boss")) {
            this.type = RoomType.BOSS;
            this.maxWaves = 1;
        } else {
            this.type = RoomType.FIGHT;
            this.maxWaves = 2;
        }
    }

    /**
     * Cập nhật tiến trình trượt cửa theo thời gian.
     */
    private void updateDoorAnimation(double deltaSeconds) {
        if (isDoorClosed) {
            if (doorProgress < 1.0f) {
                doorProgress += (float) (DOOR_ANIM_SPEED * deltaSeconds);
                if (doorProgress > 1.0f) doorProgress = 1.0f;
            }
        } else {
            if (doorProgress > 0.0f) {
                doorProgress -= (float) (DOOR_ANIM_SPEED * deltaSeconds);
                if (doorProgress < 0.0f) doorProgress = 0.0f;
            }
        }
    }

    /**
     * Cập nhật trạng thái phòng theo từng frame.
     */
    public void update(GameWorld gameWorld, Player player, List<Enemy> globalEnemies, double deltaSeconds) {
        updateDoorAnimation(deltaSeconds);
        if (type == RoomType.START || type == RoomType.REST || state == RoomState.CLEARED) {
            this.isDoorClosed = false;
            return;
        }

        if (gameWorld == null || player == null || player.getPosition() == null) {
            return;
        }

        Vector2D playerPosition = player.getPosition();

        // 1. Kiểm tra khi người chơi bước vào phòng
        if (state == RoomState.NOT_STARTED && !petEntryController.isPending() && bound.contains(playerPosition.getX(), playerPosition.getY())) {
            beginRoomActivation(player);
        }

        // 2. Chờ Pet đi vào phòng
        if (petEntryController.isPending()) {
            PetRoomEntryController.EntryResult result = petEntryController.update(gameWorld, this, player, deltaSeconds);

            if (result == PetRoomEntryController.EntryResult.WAITING) {
                return;
            }

            if (result == PetRoomEntryController.EntryResult.CANCELLED) {
                isDoorClosed = false;
                return;
            }

            completeRoomActivation(gameWorld);
            return;
        }

        if (state != RoomState.IN_PROGRESS) {
            return;
        }

        // 3. Quản lý các Wave quái
        updateWaves(gameWorld, deltaSeconds);
        checkRoomClear(globalEnemies);
    }

    private void beginRoomActivation(Player player) {
        petEntryController.begin();
        isDoorClosed = false;
        pushPlayerInside(player);
    }

    private void completeRoomActivation(GameWorld gameWorld) {
        petEntryController.finish();
        state = RoomState.IN_PROGRESS;
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

        clearRoom();
    }

    /**
     * Sinh vật cản một lần duy nhất cho phòng.
     */
    public void generateObstacles(Random random, GameWorld gameWorld) {
        if (random == null || type == RoomType.START || obstaclesGenerated) {
            return;
        }

        obstaclesGenerated = true;
        obstacles.clear();

        Player player = gameWorld != null ? gameWorld.getPlayer() : null;
        PetRoomInfo petInfo = PetRoomInfo.from(gameWorld);

        generateRandomObstacles(random, player, petInfo);
        generateGridObstacles(random, player, petInfo, false);

        if (obstacles.isEmpty()) {
            generateGridObstacles(random, player, petInfo, true);
        }

        if (obstacles.isEmpty()) {
            generateFallbackObstacle(random, player, petInfo);
        }
    }

    private void generateRandomObstacles(Random random, Player player, PetRoomInfo petInfo) {
        int attempts = 0;

        while (obstacles.size() < MAX_OBSTACLES && attempts < MAX_RANDOM_ATTEMPTS) {
            attempts++;
            BoundingBox candidate = createRandomObstacleBox(random, false);

            if (candidate == null || !isValidObstaclePosition(candidate, player, petInfo, false)) {
                continue;
            }

            addObstacle(candidate, random);
        }
    }

    private void generateGridObstacles(Random random, Player player, PetRoomInfo petInfo, boolean relaxed) {
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

                if (isValidObstaclePosition(candidate, player, petInfo, relaxed)) {
                    candidates.add(candidate);
                }
            }
        }

        Collections.shuffle(candidates, random);

        for (BoundingBox candidate : candidates) {
            if (obstacles.size() >= MAX_OBSTACLES) {
                break;
            }

            if (isValidObstaclePosition(candidate, player, petInfo, relaxed)) {
                addObstacle(candidate, random);
            }
        }
    }

    private void generateFallbackObstacle(Random random, Player player, PetRoomInfo petInfo) {
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

            double score = calculateSafetyScore(candidate, player, petInfo);

            if (score > bestScore) {
                bestScore = score;
                bestCandidate = candidate;
            }
        }

        if (bestCandidate != null && isValidObstaclePosition(bestCandidate, player, petInfo, true)) {
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

        return candidates;
    }

    private double calculateSafetyScore(BoundingBox candidate, Player player, PetRoomInfo petInfo) {
        double centerX = candidate.getMinX() + candidate.getWidth() / 2.0;
        double centerY = candidate.getMinY() + candidate.getHeight() / 2.0;
        double score = 0.0;

        if (player != null && player.getPosition() != null) {
            double dx = centerX - player.getPosition().getX();
            double dy = centerY - player.getPosition().getY();
            score += Math.sqrt(dx * dx + dy * dy);
        }

        if (petInfo != null && petInfo.exists()) {
            double dx = centerX - petInfo.getPosition().getX();
            double dy = centerY - petInfo.getPosition().getY();
            score += Math.sqrt(dx * dx + dy * dy) * 0.75;
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

    private boolean isValidObstaclePosition(BoundingBox candidate, Player player, PetRoomInfo petInfo, boolean relaxed) {
        if (candidate == null || !isBoxInsideRoom(candidate)) {
            return false;
        }

        if (overlapsExistingObstacle(candidate, relaxed) || isNearDoor(candidate, relaxed)) {
            return false;
        }

        double playerPadding = relaxed ? RELAXED_PLAYER_PADDING : PLAYER_SAFE_PADDING;
        double petPadding = relaxed ? RELAXED_PET_PADDING : PET_SAFE_PADDING;

        if (player != null && player.getPosition() != null) {
            if (rectangleIntersectsCircle(candidate, player.getPosition(), player.getRadius() + playerPadding)) {
                return false;
            }
        }

        if (petInfo != null && petInfo.exists()) {
            if (rectangleIntersectsCircle(candidate, petInfo.getPosition(), petInfo.getRadius() + petPadding)) {
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

    public void renderDoors(GraphicsContext graphicsContext, Camera camera, double tileSize) {
        if (graphicsContext == null || camera == null || tileSize <= 0.0) {
            return;
        }

        Image closedDoorTexture = Tile.getDoorImage(true);
        Image wallShadowImage = Tile.getWallFrontShadowImage();
        double zoom = camera.getZoom();
        double renderTileSize = tileSize * zoom;

        // Độ cao nhô lên từ dưới đất
        double maxOffsetY = renderTileSize;
        double currentOffsetY = maxOffsetY * (1.0 - doorProgress);

        for (BoundingBox door : doors) {
            int tilesX = Math.max(1, (int) Math.round(door.getWidth() / tileSize));
            int tilesY = Math.max(1, (int) Math.round(door.getHeight() / tileSize));

            // PHÂN BIỆT CỬA NGANG HAY CỬA DỌC
            // Cửa ngang là cửa chạy theo chiều ngang (Width > Height) - Nằm ngay dưới Tường Bắc
            boolean isHorizontalDoor = door.getWidth() > door.getHeight();

            for (int tileY = 0; tileY < tilesY; tileY++) {
                for (int tileX = 0; tileX < tilesX; tileX++) {
                    double worldX = door.getMinX() + tileX * tileSize;
                    double worldY = door.getMinY() + tileY * tileSize;
                    double screenX = camera.worldToScreenX(worldX);
                    double screenY = camera.worldToScreenY(worldY);

                    // =========================================================================
                    // 1. FIX BÓNG TƯỜNG (CHỈ VẼ CHO CỬA NGANG VÀ CHỈ TILE ĐẦU TIÊN)
                    // =========================================================================
                    // Cửa dọc (trên hành lang ngang) KHÔNG BAO GIỜ có bóng tường chiếu xuống!
                    boolean shouldDrawWallShadow = isHorizontalDoor && (tileY == 0);

                    if (shouldDrawWallShadow && wallShadowImage != null && doorProgress < 0.99f) {
                        // Cửa mở -> Hiện bóng tường. Khi cửa đóng (doorProgress tăng) -> Mờ dần và mất hẳn.
                        graphicsContext.setGlobalAlpha(1.0 - doorProgress);
                        graphicsContext.drawImage(wallShadowImage, screenX, screenY, renderTileSize, renderTileSize);
                        graphicsContext.setGlobalAlpha(1.0);
                    }

                    // =========================================================================
                    // 2. VẼ BÓNG NỀN 2.5D CỦA BẢN THÂN CỌC CỬA (KHI CỬA DẬP LÊN)
                    // =========================================================================
                    if (doorProgress > 0.05f) {
                        double shadowAlpha = doorProgress * 0.35;
                        double shadowHeight = 5.0 * zoom * doorProgress;

                        graphicsContext.setGlobalAlpha(shadowAlpha);
                        graphicsContext.setFill(Color.BLACK);
                        graphicsContext.fillRect(screenX, screenY + renderTileSize - shadowHeight, renderTileSize, shadowHeight);
                        graphicsContext.setGlobalAlpha(1.0);
                    }

                    // =========================================================================
                    // 3. VẼ CỌC CỬA TRƯỢT TỪ DƯỚI LÊN
                    // =========================================================================
                    if (closedDoorTexture != null && doorProgress > 0.01f) {
                        double doorRenderY = screenY + currentOffsetY;

                        graphicsContext.save();
                        graphicsContext.beginPath();
                        graphicsContext.rect(screenX, screenY, renderTileSize, renderTileSize);
                        graphicsContext.clip();

                        graphicsContext.drawImage(closedDoorTexture, screenX, doorRenderY, renderTileSize, renderTileSize);

                        graphicsContext.restore();
                    }
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
            boolean sameWidth = Double.compare(door.getWidth(), width) == 0;
            boolean sameHeight = Double.compare(door.getHeight(), height) == 0;

            if (sameX && sameY && sameWidth && sameHeight) {
                return;
            }
        }

        doors.add(new BoundingBox(x, y, width, height));
    }

    /**
     * Kiểm tra va chạm với cửa đang đóng.
     */
    public boolean isHitClosedDoor(double worldX, double worldY, double radius) {
        if (type == RoomType.START || state == RoomState.CLEARED || !isDoorClosed) {
            return false;
        }

        double safeRadius = Math.max(0.0, radius);
        double diameter = safeRadius * 2.0;

        for (BoundingBox door : doors) {
            if (door.intersects(worldX - safeRadius, worldY - safeRadius, diameter, diameter)) {
                return true;
            }
        }

        return false;
    }
    public boolean containsPosition(Vector2D position, double radius) {
        if (position == null || bound == null) {
            return false;
        }
        double safeRadius = Math.max(0.0, radius);
        return position.getX() - safeRadius >= bound.getMinX()
                && position.getX() + safeRadius <= bound.getMaxX()
                && position.getY() - safeRadius >= bound.getMinY()
                && position.getY() + safeRadius <= bound.getMaxY();
    }

    public void startBattle() {
        if (type != RoomType.START && type != RoomType.REST && state != RoomState.CLEARED) {
            this.isDoorClosed = true;
            this.state = RoomState.IN_PROGRESS;
        }
    }

    public void clearRoom() {
        this.state = RoomState.CLEARED;
        this.isDoorClosed = false;
        this.petEntryController.cancel();
    }

    private boolean isStartRoom() {
        return name != null && (name.equalsIgnoreCase("StartRoom") || name.toLowerCase().contains("spawn"));
    }
    public boolean isDoorClosedAtTile(int tileX, int tileY, double tileSize) {
        if (this.doorProgress <= 0.05f) {
            return false; // Cửa đang mở -> Coi như không đóng
        }

        double targetWorldX = tileX * tileSize;
        double targetWorldY = tileY * tileSize;

        for (BoundingBox door : doors) {
            // BoundingBox trong JavaFX / AWT dùng getMinX() hoặc getX() / getWidth()
            double minX = door.getMinX();
            double maxX = door.getMaxX();
            double minY = door.getMinY();
            double maxY = door.getMaxY();

            if (targetWorldX >= minX && targetWorldX < maxX &&
                    targetWorldY >= minY && targetWorldY < maxY) {
                return true;
            }
        }
        return false;
    }


    public String getName() {
        return name;
    }

    public RoomType getType() {
        return type;
    }

    public RoomState getState() {
        return state;
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

    public boolean isDoorsClosed() {
        return isDoorClosed;
    }

    public boolean isDoorClosed() {
        return isDoorClosed;
    }
}