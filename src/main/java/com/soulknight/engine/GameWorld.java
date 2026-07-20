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
import com.soulknight.utils.SoundManager;
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
            // xpa sach trang thai phim
            if (inputHandler != null) {
                inputHandler.clearState();
            }
            if (stateListener != null) {
                stateListener.onStateChanged(newState);
            }
        }
    }

    public void update(double deltaSeconds, double viewportWidth, double viewportHeight) {
//        (cuong) khi pause thi dung render
        if (state == GameState.PAUSED) {
            return;
        }
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
//                nhap chuot confirm
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
        if (mapManager != null && mapManager.getRooms() != null) {
            for (com.soulknight.map.Room room : mapManager.getRooms()) {
                room.update(this, player, enemies, deltaSeconds);
            }
        }


        for (Enemy enemy : enemies) {
            enemy.update(this, deltaSeconds);
        }
        resolvePlayerEnemyCollisions(deltaSeconds);

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
        SoundManager.getInstance().playBGM("/assets/Audio/StartGame.mp3");
    }

    //(vitdung) chỉnh lại hàm này để test loadMap từ txt
    private void loadCurrentLevel(boolean freshRun) {
        String mapPath = "/maps/level1_1.json";
        this.mapManager = new MapManager(mapPath);
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
    public InputHandler getInputHandler() {
//        goi trong player de dieu khien nhan vat tu ban phim ,ngam ban tu chuot
        return inputHandler; }
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
//        xu li ngam ban tu chuot
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
    public void spawnEnemiesInRoom(com.soulknight.map.Room room) {
        List<Vector2D> roomSpawnPoints = new ArrayList<>();
        javafx.geometry.BoundingBox bound = room.getBound();

        //  Lấy số lượng quái dự kiến từ Factory dựa trên level hiện tại
        int desiredEnemyCount = enemyFactory.calculateEnemyCount();

        // 2. : Đảm bảo số quái một phòng không bao giờ vượt quá mức cho phép
        final int MAX_ENEMIES_PER_ROOM = 3;
        int finalEnemyCount = Math.min(desiredEnemyCount, MAX_ENEMIES_PER_ROOM);

        // Khoảng cách an toàn tối thiểu giữa quái và Player
        final double MIN_SAFE_DISTANCE = 150.0;

        // Vòng lặp chỉ chạy đúng bằng số lượng quái cần sinh thực tế
        for (int i = 0; i < finalEnemyCount; i++) {
            Vector2D point = null;
            boolean validPointFound = false;

            for (int attempt = 0; attempt < 10; attempt++) {
                Vector2D randomPoint = mapManager.findRandomWalkablePosition(random, com.soulknight.utils.Constants.ENEMY_RADIUS);

                if (randomPoint != null && bound.contains(randomPoint.getX(), randomPoint.getY())) {
                    if (randomPoint.distance(player.getPosition()) >= MIN_SAFE_DISTANCE) {

                        // Giữ khoảng cách giữa các con quái với nhau, tránh sinh đè lên nhau
                        boolean tooCloseToOtherEnemies = false;
                        for (Vector2D existingPoint : roomSpawnPoints) {
                            if (randomPoint.distance(existingPoint) < 40.0) { // cách nhau tối thiểu 40px
                                tooCloseToOtherEnemies = true;
                                break;
                            }
                        }

                        if (!tooCloseToOtherEnemies) {
                            point = randomPoint;
                            validPointFound = true;
                            break;
                        }
                    }
                }
            }

            if (!validPointFound) {
                // Điểm dự phòng nếu phòng quá chật, dịch chuyển ngẫu nhiên một chút quanh tâm để không bị dính chùm
                double offsetX = (random.nextDouble() - 0.5) * 30.0;
                double offsetY = (random.nextDouble() - 0.5) * 30.0;
                point = new Vector2D((bound.getMinX() + bound.getWidth() / 2) + offsetX, (bound.getMinY() + bound.getHeight() / 2) + offsetY);
            }

            roomSpawnPoints.add(point);
        }

        // 3. Cập nhật lại lệnh gọi Factory: Truyền trực tiếp danh sách điểm đã giới hạn số lượng
        enemies.addAll(enemyFactory.createInitialEnemiesAtPoints(random, player.getPosition(), roomSpawnPoints));
    }
//    xu li va cham giua entity va entity
//  Thêm thuật toán đẩy lùi, tạo vùng cấm không cho quái chồng lấn lên hình Player
private void resolvePlayerEnemyCollisions(double deltaSeconds) {
    if (player == null || !player.isAlive()) return;

    Vector2D pPos = player.getPosition();
    double pRadius = player.getRadius();

    for (Enemy enemy : enemies) {
        if (!enemy.isAlive()) continue;

        Vector2D ePos = enemy.getPosition();
        double eRadius = enemy.getRadius();

        double distance = pPos.distance(ePos);
        double minDist = pRadius + eRadius; // Khoảng cách tối thiểu để không chạm lề hình của nhau

        // Nếu khoảng cách thực tế nhỏ hơn tổng bán kính -> Đang bị đè hình!
        if (distance < minDist) {
            double overlap = minDist - distance; // Độ sâu bị lún hình vào nhau

            // Hướng đẩy từ tâm Player hướng thẳng ra tâm Quái
            Vector2D pushDirection = ePos.copy().subtract(pPos);

            if (pushDirection.length() == 0.0) {
                // Tránh trường hợp 2 tâm trùng khít hoàn toàn (Length = 0 không tạo được vector)
                pushDirection = new Vector2D(1.0, 0.0);
            }

            pushDirection.normalize();

            // Đẩy quái ra xa 1 nửa khoảng cách lún
            Vector2D pushEnemy = pushDirection.scale(overlap * 0.5);
            enemy.move(this, pushEnemy.getX(), pushEnemy.getY());

            // Đẩy ngược Player về phía sau 1 nửa khoảng cách lún để tạo phản lực mượt mà
            player.move(this, -pushEnemy.getX(), -pushEnemy.getY());
        }
    }
}
    /**
     * (cuong)Xu li he thong ESC, Mute
     * goi lien tuc o moi frame de tranh bi fxml button de len
     */
    public void handleGlobalInput() {
//        Kiem tra phim mute
        if (inputHandler.consumeToggleMuteRequest()) {
            SoundManager.getInstance().toggleMute();
        }

//       Kiem tra ESC de pause va resume
        if (inputHandler.consumeEscapeRequest()) {
            if (state == GameState.PLAYING) {
                changeState(GameState.PAUSED);
            } else if (state == GameState.PAUSED) {
                changeState(GameState.PLAYING);
            }
        }
    }
}