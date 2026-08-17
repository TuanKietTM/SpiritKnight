package com.soulknight.map;

import com.soulknight.engine.Camera;
import com.soulknight.engine.GameWorld;
import com.soulknight.entity.Enemy;
import com.soulknight.entity.Player;
import com.soulknight.pet.PetRoomEntryController;
import com.soulknight.utils.Vector2D;
import javafx.geometry.BoundingBox;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

import java.util.ArrayList;
import java.util.List;

public class Room {

    public enum RoomType {
        START, FIGHT, REST, BOSS
    }

    public enum RoomState {
        NOT_STARTED, IN_PROGRESS, CLEARED
    }

    private static final double PLAYER_PUSH_DISTANCE = 18.0;
    private static final double NEXT_WAVE_DELAY = 1.0;

    private final String name;
    private final BoundingBox bound;
    private final RoomType type;
    private final List<Obstacle> obstacles = new ArrayList<>();
    private final PetRoomEntryController petEntryController = new PetRoomEntryController();
    private final RoomDoorController doorController = new RoomDoorController();
    private boolean obstaclesLoaded = false;
    private int currentWave;
    private int maxWaves = 2;
    private double waveDelayTimer;
    private boolean isWaitingForNextWave;
    private boolean enemiesSpawned;
    private RestRoomController restRoomController;
    private RoomState state = RoomState.NOT_STARTED;

    public Room(String name, double x, double y, double width, double height) {
        this.name = name;
        this.bound = new BoundingBox(x, y, width, height);

        if (isStartRoom()) {
            this.type = RoomType.START;
            this.state = RoomState.CLEARED;
            this.doorController.setClosed(false);
            this.maxWaves = 0;
        } else if (name != null && name.toLowerCase().contains("rest")) {
            this.type = RoomType.REST;
            this.state = RoomState.CLEARED;
            this.doorController.setClosed(false);
            this.maxWaves = 0;
            this.restRoomController = new RestRoomController(this);
        } else if (name != null && name.toLowerCase().contains("boss")) {
            this.type = RoomType.BOSS;
            this.maxWaves = 1;
        } else {
            this.type = RoomType.FIGHT;
            this.maxWaves = 2;
        }
    }


    public void update(GameWorld gameWorld, Player player, List<Enemy> globalEnemies, double deltaSeconds) {
        // đề xuất xóa
        doorController.update(deltaSeconds);
        doorController.update(deltaSeconds);
        if (type == RoomType.REST) {
            doorController.setClosed(false);

            if (restRoomController != null) {
                restRoomController.update(gameWorld, player, deltaSeconds);
            }
            return;
        }

        if (type == RoomType.START || state == RoomState.CLEARED) {
            doorController.setClosed(false);
            return;
        }

        if (gameWorld == null || player == null || player.getPosition() == null) {
            return;
        }

        Vector2D playerPosition = player.getPosition();

        // Kiem tra nguoi choi buoc vao phong
        if (state == RoomState.NOT_STARTED && !petEntryController.isPending() && bound.contains(playerPosition.getX(), playerPosition.getY())) {
            beginRoomActivation(player);
        }

        // Cho pet di vao phong
        if (petEntryController.isPending()) {
            PetRoomEntryController.EntryResult result = petEntryController.update(gameWorld, this, player, deltaSeconds);

            if (result == PetRoomEntryController.EntryResult.WAITING) {
                return;
            }

            if (result == PetRoomEntryController.EntryResult.CANCELLED) {
                doorController.setClosed(false);
                return;
            }

            completeRoomActivation(gameWorld);
            return;
        }

        if (state != RoomState.IN_PROGRESS) {
            return;
        }

        // Quan lí cac wave quai
        updateWaves(gameWorld, deltaSeconds);
        checkRoomClear(globalEnemies);
    }

    private void beginRoomActivation(Player player) {
        petEntryController.begin();
        doorController.setClosed(false);
        pushPlayerInside(player);
    }

    private void completeRoomActivation(GameWorld gameWorld) {
        petEntryController.finish();
        state = RoomState.IN_PROGRESS;
        doorController.setClosed(true);
        currentWave = 1;
        waveDelayTimer = 0.0;
        isWaitingForNextWave = false;

        if (gameWorld.spawnEnemiesInRoom(this, currentWave) > 0) {
            enemiesSpawned = true;
        }
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
        if (gameWorld.spawnEnemiesInRoom(this, currentWave) > 0) {
            enemiesSpawned = true;
        }
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
     *Nap cac vat can tu map
     */
    public void loadObstaclesFromTiles(Tile[][] tiles) {
        if (tiles == null || obstaclesLoaded) return;
        obstaclesLoaded = true;
        obstacles.clear();
        int gridHeight = tiles.length;
        int gridWidth = tiles[0].length;
        for (int y = 0; y < gridHeight; y++) {
            for (int x = 0; x < gridWidth; x++) {
                Tile tile = tiles[y][x];
                if (tile != null && tile.getType() == Tile.TileType.OBSTACLE) {
                    double tileX = tile.getX();
                    double tileY = tile.getY();
                    double tileSize = tile.getSize();

                    if (tileX >= bound.getMinX() && tileX + tileSize <= bound.getMaxX() &&
                            tileY >= bound.getMinY() && tileY + tileSize <= bound.getMaxY()) {

                        Vector2D pos = new Vector2D(tileX, tileY);
                        Image tileSprite = tile.getTexture();
                        boolean isBox = (tileSprite == Tile.getBoxImage());

//                    do ben
                        boolean destructible = isBox; // chi hom go moi pha duoc
                        int hp = isBox ? 30 : 9999;  // 30 mau
                        obstacles.add(new Obstacle(pos, tileSize, tileSize, hp, destructible, tileSprite));
                    }
                }
            }
        }
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

//uy quen cac phuong thuc dỏo chop door controller

    public boolean isDoorBelongsToRoom(double doorX, double doorY, double doorWidth, double doorHeight) {
        return doorController.isDoorBelongsToRoom(bound, doorX, doorY, doorWidth, doorHeight);
    }

    public void addDoorCoordinate(double x, double y, double width, double height) {
        doorController.addDoorCoordinate(x, y, width, height);
    }

    public boolean isHitClosedDoor(double worldX, double worldY, double radius) {
        if (type == RoomType.START || state == RoomState.CLEARED) {
            return false;
        }
        return doorController.isHitClosedDoor(worldX, worldY, radius);
    }

    public boolean isDoorClosedAtTile(int tileX, int tileY, double tileSize) {
        return doorController.isDoorClosedAtTile(tileX, tileY, tileSize);
    }

    public boolean hasSpawnedEnemies() {
        return enemiesSpawned;
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

    public void clearRoom() {
        this.state = RoomState.CLEARED;
        this.doorController.setClosed(false);
        this.petEntryController.cancel();
    }

    private boolean isStartRoom() {
        return name != null && (name.equalsIgnoreCase("StartRoom") || name.toLowerCase().contains("spawn"));
    }

    public void renderSingleDoor(GraphicsContext graphicsContext, Camera camera, BoundingBox door, double tileSize) {
        doorController.renderSingleDoor(graphicsContext, camera, door, tileSize);
    }
    public boolean isUfoSummonUsed() {
        return ufoSummonUsed;
    }

    public void setUfoSummonUsed(boolean ufoSummonUsed) {
        this.ufoSummonUsed = ufoSummonUsed;
    }
    public int getCurrentWave() {
        return currentWave;
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
        return doorController.getDoors();
    }

    public RestRoomController getRestRoomController() {return restRoomController;}

    public boolean isDoorsClosed() {return doorController.isClosed();}
}