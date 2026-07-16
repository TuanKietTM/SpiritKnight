package com.soulknight.engine;

import com.soulknight.entity.Enemy;
import com.soulknight.entity.EnemyFactory;
import com.soulknight.entity.Player;
import com.soulknight.item.EnergyCrystal;
import com.soulknight.item.Item;
import com.soulknight.level.LevelManager;
import com.soulknight.map.MapManager;
import com.soulknight.mission.MissionManager;
import com.soulknight.utils.Constants;
import com.soulknight.utils.Vector2D;
import com.soulknight.weapon.Bullet;
import com.soulknight.weapon.Gun;
import com.soulknight.weapon.Weapon;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javafx.scene.canvas.GraphicsContext;

public final class GameWorld {

    private final Random random = new Random();
    private final InputHandler inputHandler;
    private final Camera camera = new Camera();
    private final LevelManager levelManager = new LevelManager();
    private final MissionManager missionManager = new MissionManager();
    private final EnemyFactory enemyFactory = new EnemyFactory(levelManager, missionManager);

    private GameStateListener stateListener;

    private MapManager mapManager;
    private Player player;
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<Bullet> bullets = new ArrayList<>();
    private final List<Item> items = new ArrayList<>();
    private GameState state = GameState.INTRO;
    private double enemySpawnTimer;
    private Vector2D pendingPortalPosition;

    public interface GameStateListener {
        void onStateChanged(GameState newState);
    }

    public GameWorld(InputHandler inputHandler) {
        this.inputHandler = inputHandler;
//        startNewRun();
    }

    public void setGameStateListener(GameStateListener listener) {
        this.stateListener = listener;
        // Kích hoạt trạng thái ban đầu cho giao diện
        if (this.stateListener != null) {
            this.stateListener.onStateChanged(this.state);
        }
    }

    public void changeState(GameState newState) {
// thay doi trang thai
        if ((this.state == GameState.MAIN_MENU || this.state == GameState.GAME_OVER || this.state == GameState.GAME_VICTORY)
                && newState == GameState.PLAYING) {
            startNewRun();
        }

        if (this.state != newState) {
            this.state = newState;
            if (stateListener != null) {
                stateListener.onStateChanged(newState);
            }
        }
    }

    public void update(double deltaSeconds, double viewportWidth, double viewportHeight) {
        switch (state) {
            case INTRO -> {
                if (inputHandler.consumeConfirmRequest()) {
                    changeState(GameState.MAIN_MENU);
                }
            }
            case MAIN_MENU -> {
                inputHandler.consumeConfirmRequest();
            }
            case PLAYING -> updatePlaying(deltaSeconds, viewportWidth, viewportHeight, true);
            case LEVEL_CLEAR -> updateLevelClear(deltaSeconds, viewportWidth, viewportHeight);
            case PAUSED -> {
            }
            case GAME_OVER, GAME_VICTORY -> {
                if (inputHandler.consumeConfirmRequest()) {
                    startNewRun();
                    changeState(GameState.PLAYING);
                }
            }
        }
    }

    public void render(GraphicsContext graphicsContext, double renderWidth, double renderHeight) {
        graphicsContext.clearRect(0.0, 0.0, renderWidth, renderHeight);
        if (state != GameState.INTRO && state != GameState.MAIN_MENU && mapManager != null && player != null) {
            renderWorld(graphicsContext, renderWidth, renderHeight);
        }
    }

    private void updatePlaying(double deltaSeconds, double viewportWidth, double viewportHeight, boolean allowSpawns) {
        player.update(this, deltaSeconds);


        for (Enemy enemy : enemies) {
            enemy.update(this, deltaSeconds);
        }

        updateBullets(deltaSeconds);
        updateItemCollection();

        if (allowSpawns) {
            updateSpawnTimers(deltaSeconds);
        }

        camera.follow(player.getPosition(), viewportWidth, viewportHeight, mapManager.getWorldWidth(), mapManager.getWorldHeight());

        if (!player.isAlive()) {
            inputHandler.consumeConfirmRequest();
            changeState(GameState.GAME_OVER);
            return;
        }

        if (missionManager.isMissionComplete()) {
            if (levelManager.getCurrentLevel().bossLevel()) {
                changeState(GameState.GAME_VICTORY);
            } else {
                changeState(GameState.LEVEL_CLEAR);
                mapManager.openExitPortal(pendingPortalPosition);
            }
        }
    }

    private void updateLevelClear(double deltaSeconds, double viewportWidth, double viewportHeight) {
        player.update(this, deltaSeconds);
        camera.follow(player.getPosition(), viewportWidth, viewportHeight, mapManager.getWorldWidth(), mapManager.getWorldHeight());

        if (mapManager.isPlayerInsideExitPortal(player.getPosition())) {
            advanceToNextLevel();
        }
    }

    private void updateBullets(double deltaSeconds) {
        for (Bullet bullet : bullets) {
            bullet.update(deltaSeconds);
            if (!mapManager.isWalkable(bullet.getPosition().getX(), bullet.getPosition().getY(), bullet.getRadius())) {
                bullet.deactivate();
                continue;
            }

            if (bullet.getOwner() instanceof Player) {
                for (Enemy enemy : enemies) {
                    if (enemy.isAlive() && bullet.intersects(enemy)) {
                        enemy.takeDamage(bullet.getDamage());
                        bullet.deactivate();
                        break;
                    }
                }
            } else if (bullet.intersects(player)) {
                player.takeDamage(bullet.getDamage());
                bullet.deactivate();
            }
        }

        bullets.removeIf(bullet -> !bullet.isActive());
        enemies.removeIf(enemy -> !enemy.isAlive());
    }

    private void updateItemCollection() {
        for (Item item : items) {
            if (!item.isCollected() && item.intersects(player.getPosition(), player.getRadius())) {
                item.collect();
            }
        }
        items.removeIf(Item::isCollected);
    }

    private void updateSpawnTimers(double deltaSeconds) {
        LevelManager.LevelDefinition level = levelManager.getCurrentLevel();
        if (level.spawnIntervalSeconds() <= 0) {
            return;
        }

        enemySpawnTimer += deltaSeconds;
        if (enemySpawnTimer < level.spawnIntervalSeconds()) {
            return;
        }

        enemySpawnTimer = 0.0;
        if (level.number() == 3) {
            spawnEliteMinions(2);
        } else if (level.number() == 2) {
            spawnMixedWave();
        }
    }

    private void spawnMixedWave() {
        List<Vector2D> spawnPoints = createSpawnPoints(4);
        enemies.addAll(enemyFactory.createInitialEnemies(random, player.getPosition(), spawnPoints));
    }

    private void spawnEliteMinions(int count) {
        for (int i = 0; i < count; i++) {
            Vector2D spawnPoint = mapManager.findRandomWalkablePosition(random, Constants.ENEMY_RADIUS);
            if (spawnPoint != null) {
                enemies.add(enemyFactory.createEliteMinion(spawnPoint));
            }
        }
    }

    private void renderWorld(GraphicsContext graphicsContext, double renderWidth, double renderHeight) {
        mapManager.render(graphicsContext, camera, renderWidth, renderHeight);

        for (Bullet bullet : bullets) {
            bullet.render(graphicsContext, camera);
        }

        for (Item item : items) {
            item.render(graphicsContext, camera);
        }

        for (Enemy enemy : enemies) {
            enemy.render(graphicsContext, camera);
        }

        player.render(graphicsContext, camera);
    }

    private void startNewRun() {
        levelManager.startNewRun();
        loadCurrentLevel(true);
    }

    //(vitdung) chỉnh lại hàm này để test loadMap từ txt
    private void loadCurrentLevel(boolean freshRun) {
        String mapPath = "/maps/level1_1.txt";
        this.mapManager = new MapManager(mapPath, Constants.TILE_SIZE);
        this.mapManager.closeExitPortal();
        // đoạn portal chưa rõ lắm
        this.pendingPortalPosition = mapManager.getExitPortalPosition();
        if (this.pendingPortalPosition == null) {
            this.pendingPortalPosition = mapManager.getSpawnPoint();
        }

        if (player == null || freshRun) {
            player = new Player(mapManager.getSpawnPoint());
        } else {
            player.getPosition().set(mapManager.getSpawnPoint());
        }

        if (freshRun) {
            player.equipWeapon(new Gun("Blaster", 12, 0.18, 580.0, 0.0));
        }

        // reset lại các object
        enemies.clear();
        bullets.clear();
        items.clear();
        enemySpawnTimer = 0.0;

        // chỗ nhiệm vụ này chưa cho vào chế độ thường
        missionManager.setMission(levelManager.createMissionForCurrentLevel());

        if (levelManager.getCurrentLevel().number() == 2) {
            spawnEnergyCrystals(3);
        }

        // lấy danh sách các điểm enemy từ Mapmanager để cho quái xuất hiện ở vị trí đó trên bản đồ
        List<Vector2D> enemySpawnPoints = mapManager.getEnemySpawnPoints();

        if (!enemySpawnPoints.isEmpty()) {
            enemies.addAll(enemyFactory.createInitialEnemies(random, player.getPosition(), enemySpawnPoints));
        }
        // nếu là màn có boss
        if (levelManager.getCurrentLevel().bossLevel() && mapManager.getBossSpawnPoint() != null) {
            enemies.add(enemyFactory.createGrandKnight(mapManager.getBossSpawnPoint()));
        }
    }

    private void spawnEnergyCrystals(int count) {
        for (int i = 0; i < count; i++) {
            Vector2D position = mapManager.findRandomWalkablePosition(random, 20.0);
            if (position != null) {
                items.add(new EnergyCrystal(position, missionManager));
            }
        }
    }

    private List<Vector2D> createSpawnPoints(int count) {
        List<Vector2D> spawnPoints = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Vector2D spawnPoint = mapManager.findRandomWalkablePosition(random, Constants.ENEMY_RADIUS);
            if (spawnPoint != null && spawnPoint.distance(player.getPosition()) > 220.0) {
                spawnPoints.add(spawnPoint);
            }
        }
        if (spawnPoints.isEmpty()) {
            spawnPoints.add(mapManager.getBossSpawnPoint());
        }
        return spawnPoints;
    }

    private void advanceToNextLevel() {
        Weapon rewardWeapon = levelManager.getLevelRewardWeapon();
        if (rewardWeapon != null) {
            player.equipWeapon(rewardWeapon);
        }

        if (levelManager.advanceLevel()) {
            loadCurrentLevel(false);
            changeState(GameState.PLAYING);
        } else {
            changeState(GameState.GAME_VICTORY);
        }
    }

    public GameState getState() { return state; }
    public InputHandler getInputHandler() { return inputHandler; }
    public MapManager getMapManager() { return mapManager; }
    public Player getPlayer() { return player; }
    public List<Enemy> getEnemies() { return enemies; }
    public List<Item> getItems() { return items; }

    public void addBullet(Bullet bullet) { bullets.add(bullet); }
    public List<Bullet> getBullets() { return bullets; }
    public Camera getCamera() { return camera; }
    public LevelManager getLevelManager() { return levelManager; }
    public MissionManager getMissionManager() { return missionManager; }

    public Vector2D getMouseWorldPosition() {
        return camera.screenToWorld(inputHandler.getMousePosition());
    }

    public boolean canMoveTo(Vector2D position, double radius) {
        return mapManager.isWalkable(position.getX(), position.getY(), radius);
    }

    public void damageEnemiesInRange(Vector2D origin, double range, int damage) {
        for (Enemy enemy : enemies) {
            if (enemy.isAlive() && enemy.getPosition().distance(origin) <= range + enemy.getRadius()) {
                enemy.takeDamage(damage);
            }
        }
    }

    public boolean isPlaying() {
        return state == GameState.PLAYING;
    }
}