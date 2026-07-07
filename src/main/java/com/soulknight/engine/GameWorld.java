package com.soulknight.engine;

import com.soulknight.entity.Boss;
import com.soulknight.entity.Enemy;
import com.soulknight.entity.EnemyFactory;
import com.soulknight.entity.Player;
import com.soulknight.item.EnergyCrystal;
import com.soulknight.item.Item;
import com.soulknight.level.LevelManager;
import com.soulknight.map.MapManager;
import com.soulknight.mission.MissionManager;
import com.soulknight.ui.GameOverScreen;
import com.soulknight.ui.HUD;
import com.soulknight.ui.LevelClearScreen;
import com.soulknight.ui.Menu;
import com.soulknight.ui.VictoryScreen;
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
    private final HUD hud = new HUD();
    private final Menu menu = new Menu();
    private final GameOverScreen gameOverScreen = new GameOverScreen();
    private final LevelClearScreen levelClearScreen = new LevelClearScreen();
    private final VictoryScreen victoryScreen = new VictoryScreen();

    private MapManager mapManager;
    private Player player;
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<Bullet> bullets = new ArrayList<>();
    private final List<Item> items = new ArrayList<>();
    private GameState state = GameState.MAIN_MENU;
    private double enemySpawnTimer;
    private Vector2D pendingPortalPosition;

    public GameWorld(InputHandler inputHandler) {
        this.inputHandler = inputHandler;
        startNewRun();
    }

    public void update(double deltaSeconds, double viewportWidth, double viewportHeight) {
        switch (state) {
            case MAIN_MENU -> {
                if (inputHandler.consumeConfirmRequest()) {
                    startNewRun();
                    state = GameState.PLAYING;
                }
            }
            case PLAYING -> updatePlaying(deltaSeconds, viewportWidth, viewportHeight, true);
            case LEVEL_CLEAR -> updateLevelClear(deltaSeconds, viewportWidth, viewportHeight);
            case GAME_OVER, GAME_VICTORY -> {
                if (inputHandler.consumeConfirmRequest()) {
                    startNewRun();
                    state = GameState.PLAYING;
                }
            }
        }
    }

    public void render(GraphicsContext graphicsContext, double renderWidth, double renderHeight) {
        graphicsContext.clearRect(0.0, 0.0, renderWidth, renderHeight);

        switch (state) {
            case MAIN_MENU -> menu.render(graphicsContext, renderWidth, renderHeight);
            case PLAYING -> renderWorld(graphicsContext, renderWidth, renderHeight);
            case LEVEL_CLEAR -> {
                renderWorld(graphicsContext, renderWidth, renderHeight);
                levelClearScreen.render(graphicsContext, renderWidth, renderHeight, levelManager, missionManager);
            }
            case GAME_OVER -> {
                renderWorld(graphicsContext, renderWidth, renderHeight);
                gameOverScreen.render(graphicsContext, renderWidth, renderHeight);
            }
            case GAME_VICTORY -> {
                renderWorld(graphicsContext, renderWidth, renderHeight);
                victoryScreen.render(graphicsContext, renderWidth, renderHeight);
            }
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
            state = GameState.GAME_OVER;
            return;
        }

        if (missionManager.isMissionComplete()) {
            if (levelManager.getCurrentLevel().bossLevel()) {
                state = GameState.GAME_VICTORY;
            } else {
                state = GameState.LEVEL_CLEAR;
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
        hud.render(graphicsContext, player, levelManager, missionManager, enemies.size(), items.size(), renderWidth, renderHeight);
    }

    private void startNewRun() {
        levelManager.startNewRun();
        loadCurrentLevel(true);
    }

    private void loadCurrentLevel(boolean freshRun) {
        mapManager = new MapManager(Constants.MAP_WIDTH, Constants.MAP_HEIGHT, Constants.TILE_SIZE, random);
        mapManager.closeExitPortal();
        pendingPortalPosition = mapManager.findRandomWalkablePosition(random, Constants.PORTAL_RADIUS);
        if (pendingPortalPosition == null) {
            pendingPortalPosition = mapManager.getSpawnPoint();
        }

        if (player == null || freshRun) {
            player = new Player(mapManager.getSpawnPoint());
        } else {
            player.getPosition().set(mapManager.getSpawnPoint());
        }

        if (freshRun) {
            player.equipWeapon(new Gun("Blaster", 12, 0.18, 580.0, 0.0));
        }

        enemies.clear();
        bullets.clear();
        items.clear();
        enemySpawnTimer = 0.0;

        missionManager.setMission(levelManager.createMissionForCurrentLevel());

        if (levelManager.getCurrentLevel().number() == 2) {
            spawnEnergyCrystals(3);
        }

        List<Vector2D> spawnPoints = createSpawnPoints(levelManager.getCurrentLevel().baseEnemyCount());
        enemies.addAll(enemyFactory.createInitialEnemies(random, player.getPosition(), spawnPoints));
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
            state = GameState.PLAYING;
        } else {
            state = GameState.GAME_VICTORY;
        }
    }

    public InputHandler getInputHandler() {
        return inputHandler;
    }

    public MapManager getMapManager() {
        return mapManager;
    }

    public Player getPlayer() {
        return player;
    }

    public List<Enemy> getEnemies() {
        return enemies;
    }

    public void addBullet(Bullet bullet) {
        bullets.add(bullet);
    }

    public List<Bullet> getBullets() {
        return bullets;
    }

    public Camera getCamera() {
        return camera;
    }

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
