package com.soulknight.engine;

import com.soulknight.animation.ParticleManager;
import com.soulknight.animation.ShadowRenderer;
import com.soulknight.animation.SpawnEffect;
import com.soulknight.entity.Enemy;
import com.soulknight.entity.EnemyFactory;
import com.soulknight.entity.Entity;
import com.soulknight.entity.Player;
import com.soulknight.item.EnergyCrystal;
import com.soulknight.item.GemItem;
import com.soulknight.item.GoldItem;
import com.soulknight.item.Item;
import com.soulknight.level.LevelManager;
import com.soulknight.map.MapManager;
import com.soulknight.map.Obstacle;
import com.soulknight.map.Room;
import com.soulknight.map.Tile;
import com.soulknight.mission.MissionManager;
import com.soulknight.utils.Constants;
import com.soulknight.utils.SoundManager;
import com.soulknight.utils.Vector2D;
import com.soulknight.weapon.Bullet;
import com.soulknight.weapon.ExplosionEffect;
import com.soulknight.weapon.Melee;
import com.soulknight.weapon.SlashEffect;
import com.soulknight.weapon.Weapon;
import com.soulknight.weapon.WeaponSelectionManager;
import com.soulknight.weapon.WeaponType;
import com.soulknight.pet.Pet;
import com.soulknight.pet.PetFactory;
import com.soulknight.pet.PetSelectionManager;
import com.soulknight.pet.PetType;
import com.soulknight.database.PlayerSave;
import com.soulknight.database.PlayerSaveDAO;
import com.soulknight.database.PlayerSaveMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.geometry.BoundingBox;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public final class GameWorld {

    private static final boolean DEBUG_LOGGING = false;

    private final Random random = new Random();
    private final InputHandler inputHandler;
    private final Camera camera = new Camera();
    private final LevelManager levelManager = new LevelManager();
    private final MissionManager missionManager = new MissionManager();
    private final EnemyFactory enemyFactory = new EnemyFactory(levelManager, missionManager);

    private GameStateListener stateListener;

    private MapManager mapManager;
    private Player player;
    private Pet currentPet;
    private String currentPlayerName = "Knight";

    private int gold = 0;
    private int gems = 0;
    private int score = 0;
    private int currentRoomNumber = 1;
    private Room currentRoom;

    private double autoSaveTimer = 0.0;
    private static final double AUTO_SAVE_INTERVAL = 30.0;
    private double playerEnergy = 100.0;

    private PlayerSave pendingPlayerSave;

    private final List<Enemy> enemies = new ArrayList<>();
    private final List<Bullet> bullets = new ArrayList<>();
    // Danh sach hieu ung no khi dan va cham (tuong hoac muc tieu)
    private final List<ExplosionEffect> explosions = new ArrayList<>();
    // Danh sach hieu ung chem cua vu khi can chien (kiem)
    private final List<SlashEffect> slashEffects = new ArrayList<>();
    private final List<Item> items = new ArrayList<>();
    // Sửa tên biến từ effectManager -> particleManager
    private final ParticleManager particleManager = new ParticleManager();
    private GameState state = GameState.INTRO;
    private double enemySpawnTimer;
    private Vector2D pendingPortalPosition;
    //    Bien cho hieu ung dau tien cua start room
    private SpawnEffect playerSpawnEffect;
    private SpawnEffect petSpawnEffect;
    private final List<Obstacle> obstacles = new ArrayList<>();
    private final List<Obstacle> readOnlyObstacles = java.util.Collections.unmodifiableList(obstacles);
    private final List<Obstacle> destroyedObstacleQueue = new ArrayList<>();

    private final ExecutorService databaseExecutor =
            Executors.newSingleThreadExecutor(runnable -> {
                Thread thread = new Thread(runnable, "soul-knight-database-worker");
                thread.setDaemon(true);
                return thread;
            });

    private final AtomicBoolean saveInProgress = new AtomicBoolean(false);

    public interface GameStateListener {
        void onStateChanged(GameState newState);
    }

    public GameWorld(InputHandler inputHandler) {
        this.inputHandler = inputHandler;
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
        // Nếu game đang Pause thì ngưng toàn bộ logic cập nhật
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
            case PLAYING -> updatePlaying(deltaSeconds, viewportWidth, viewportHeight, false);
            case LEVEL_CLEAR -> updateLevelClear(deltaSeconds, viewportWidth, viewportHeight);
            case GAME_OVER, GAME_VICTORY -> {
                // Nhấp chuột hoặc bấm nút Confirm để quay lại chơi mới
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
        if (player == null || mapManager == null) {
            return;
        }

        // 1. Cập nhật đếm giờ cho hiệu ứng Spawn của Player & Pet
        if (playerSpawnEffect != null) {
            playerSpawnEffect.update(deltaSeconds);
        }
        if (petSpawnEffect != null) {
            petSpawnEffect.update(deltaSeconds);
        }

        player.update(this, deltaSeconds);
        updatePet(deltaSeconds);
        updateCurrentRoom();

        if (currentRoom != null) {
            currentRoom.update(this, player, enemies, deltaSeconds);
        }


        for (Enemy enemy : enemies) {
            enemy.update(this, deltaSeconds);
            // xử lý va chạm quái vs quái
            enemy.separateFromOtherEnemies(this,enemies, deltaSeconds);
        }
        resolvePlayerEnemyCollisions(deltaSeconds);

        updateBullets(deltaSeconds);
        processDestroyedObstacles();
        updateExplosions(deltaSeconds);
        updateSlashEffects(deltaSeconds);
        updateItemCollection();
        updateAutoSave(deltaSeconds);

        if (allowSpawns) {
            updateSpawnTimers(deltaSeconds);
        }

        camera.follow(player.getPosition(), viewportWidth, viewportHeight, mapManager.getWorldWidth(), mapManager.getWorldHeight());

        if (!player.isAlive()) {
            inputHandler.consumeConfirmRequest();
            changeState(GameState.GAME_OVER);
            addScore(100);
            saveGameAsync();
            return;
        }
        if (particleManager != null) {
            particleManager.update(deltaSeconds);
        }
    }

    private void updateLevelClear(double deltaSeconds, double viewportWidth, double viewportHeight) {
        player.update(this, deltaSeconds);
        updatePet(deltaSeconds);
        camera.follow(player.getPosition(), viewportWidth, viewportHeight, mapManager.getWorldWidth(), mapManager.getWorldHeight());

        if (mapManager.isPlayerInsideExitPortal(player.getPosition())) {
            advanceToNextLevel();
        }
    }
    //    update pet
    private void updatePet(double deltaSeconds) {
        if (currentPet == null || player == null || player.getPosition() == null) {
            return;
        }
        // 🔥 Truyền "this" (GameWorld) để Pet thừa hưởng toàn bộ MapManager + Vật cản (Obstacle)
        currentPet.update(deltaSeconds, player.getPosition().getX(), player.getPosition().getY(), this);
    }

    private void updateBullets(double deltaSeconds) {
        for (Bullet bullet : bullets) {
            if (bullet == null || !bullet.isActive()) {
                continue;
            }

            double bulletX = bullet.getPosition().getX();
            double bulletY = bullet.getPosition().getY();

            // Đạn xuất hiện trong tường
            if (mapManager.isBulletCollidingWithWall(bulletX, bulletY)) {
                bullet.deactivate();
                continue;
            }

            bullet.update(deltaSeconds);

            bulletX = bullet.getPosition().getX();
            bulletY = bullet.getPosition().getY();

            // Đạn va chạm tường sau khi di chuyển
            if (mapManager.isBulletCollidingWithWall(bulletX, bulletY)) {
                spawnBulletExplosion(bullet.getPosition());
                bullet.deactivate();
                continue;
            }

            // Đạn va chạm obstacle
            for (Obstacle obstacle : obstacles) {
                if (obstacle == null || obstacle.isDestroyed()) {
                    continue;
                }

                if (!obstacle.intersectsCircle(bullet.getPosition(), bullet.getRadius())) {
                    continue;
                }

                damageObstacle(obstacle, bullet.getDamage(), bullet.getPosition());
                bullet.deactivate();
                break;
            }

            // Đạn đã trúng obstacle thì không kiểm tra enemy nữa
            if (!bullet.isActive()) {
                continue;
            }

            if (bullet.getOwner() instanceof Player) {
                for (Enemy enemy : enemies) {
                    if (enemy == null || !enemy.isAlive()) {
                        continue;
                    }

                    if (bullet.intersects(enemy)) {
                        enemy.takeDamage(bullet.getDamage());
                        spawnBulletExplosion(
                                bullet.getPosition()
                        );

                        bullet.deactivate();
                        break;
                    }
                }
            } else if (player != null && bullet.intersects(player)) {
                player.takeDamage(bullet.getDamage());
                spawnBulletExplosion(bullet.getPosition());
                bullet.deactivate();
            }
        }

        bullets.removeIf(
                bullet -> bullet == null || !bullet.isActive()
        );

        removeDeadEnemiesAndGiveRewards();
    }
    private void processDestroyedObstacles() {
        if (destroyedObstacleQueue.isEmpty() || mapManager == null || mapManager.getRooms() == null) {
            return;
        }

        double tileSize = mapManager.getTileSize();

        for (Obstacle obstacle : destroyedObstacleQueue) {
            if (obstacle == null) {
                continue;
            }

            if (particleManager != null) {
                particleManager.spawnWoodDebris(obstacle.getCenter());
            }

            if (obstacle.getPosition() != null) {
                int gridX = (int) (obstacle.getPosition().getX() / tileSize);
                int gridY = (int) (obstacle.getPosition().getY() / tileSize);
                mapManager.setTileType(gridX, gridY, Tile.TileType.FLOOR);
            }

            obstacles.remove(obstacle);

            for (Room room : mapManager.getRooms()) {
                if (room == null || room.getObstacles() == null) {
                    continue;
                }
                if (room.getObstacles().remove(obstacle)) {
                    break;
                }
            }
        }

        destroyedObstacleQueue.clear();
    }

    // Tao hieu ung no tai vi tri dan va cham (tuong hoac muc tieu)
    private void spawnBulletExplosion(Vector2D position) {
        explosions.add(new ExplosionEffect(position.copy(), 18.0, 0.3, Color.ORANGERED));
    }

    // Cap nhat vong doi hieu ung no, xoa cai da ket thuc
    private void updateExplosions(double deltaSeconds) {
        for (ExplosionEffect explosion : explosions) {
            explosion.update(deltaSeconds);
        }
        explosions.removeIf(explosion -> !explosion.isActive());
    }

    // Cap nhat vong doi hieu ung chem, xoa cai da ket thuc
    private void updateSlashEffects(double deltaSeconds) {
        for (SlashEffect slash : slashEffects) {
            slash.update(deltaSeconds);
        }
        slashEffects.removeIf(slash -> !slash.isActive());
    }

    // Tao hieu ung chem bam theo nhan vat theo huong ngam (goi tu vu khi can chien)
    public void spawnMeleeSlash(Entity owner, double aimAngle) {
        slashEffects.add(new SlashEffect(owner, aimAngle));
    }

    private void updateItemCollection() {
        if (player == null || player.getPosition() == null) {
            return;
        }

        for (Item item : items) {
            if (item.isCollected()) {
                continue;
            }

            if (!item.intersects(player.getPosition(), player.getRadius())) {
                continue;
            }

            if (item instanceof GoldItem goldItem) {
                addGold(goldItem.getAmount());
                debug("Đã nhặt " + goldItem.getAmount() + " vàng.");
            } else if (item instanceof GemItem gemItem) {
                addGems(gemItem.getAmount());
                debug("Đã nhặt " + gemItem.getAmount() + " kim cương.");
            }

            item.collect();
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
        // 1. Vẽ sàn nhà bẹt dưới cùng trước
        mapManager.renderFloor(graphicsContext, camera, renderWidth, renderHeight);
        // Vẽ bóng của obstacle dưới các sprite. Obstacle đã được load một lần khi load map.
        for (Obstacle obstacle : obstacles) {
            if (obstacle == null || obstacle.isDestroyed() || obstacle.getPosition() == null) {
                continue;
            }
            if (!isVisibleInCamera(obstacle.getPosition().getX(), obstacle.getPosition().getY(),
                    obstacle.getWidth(), obstacle.getHeight(), renderWidth, renderHeight)) {
                continue;
            }
            obstacle.renderShadow(graphicsContext, camera);
        }

        // 2. Danh sách Y-Sorting
        class SortableObject {
            double depthY;
            Runnable renderAction;
            SortableObject(double depthY, Runnable renderAction) {
                this.depthY = depthY;
                this.renderAction = renderAction;
            }
        }

        List<SortableObject> renderList = new ArrayList<>();
        double tileSize = mapManager.getTileSize();

// A. Thêm các ô TƯỜNG vào Y-Sorting (Mốc Y tính ở ĐÁY ô Tile Tường)
        for (Tile wall : mapManager.getWallTiles()) {
            if (wall == null || !isVisibleInCamera(wall.getX(), wall.getY(), tileSize, tileSize, renderWidth, renderHeight)) {
                continue;
            }
            double wallBottomY = wall.getY() + tileSize; // ĐÁY Ô TƯỜNG
            renderList.add(new SortableObject(wallBottomY, () -> {
                double screenX = camera.worldToScreenX(wall.getX());
                double screenY = camera.worldToScreenY(wall.getY());
                double zoom = camera.getZoom();
                graphicsContext.drawImage(wall.getTexture(), screenX, screenY, tileSize * zoom, tileSize * zoom);
            }));
        }

// B. Thêm PLAYER (Mốc Y tính ở BÀN CHÂN)
        if (player != null && player.getPosition() != null) {
            double playerFootY = player.getPosition().getY() + 10.0;
            renderList.add(new SortableObject(playerFootY, () -> {
                boolean isSpawning = playerSpawnEffect != null && playerSpawnEffect.isSpawning();

                if (isSpawning) {
                    double alpha = playerSpawnEffect.getEntityAlpha();

                    // chi ve bong khi cac doi tuong xuat hien
                    if (alpha > 0.0) {
                        graphicsContext.save();
                        graphicsContext.setGlobalAlpha(alpha);
                        ShadowRenderer.render(graphicsContext, camera, player.getPosition(), 22.0, 6.0);
                        graphicsContext.restore();
                    }

                    // ve player mo dan
                    graphicsContext.save();
                    graphicsContext.setGlobalAlpha(alpha);
                    player.render(graphicsContext, camera);
                    graphicsContext.restore();

                    // ve hieu ung cot sang de len tren
                    playerSpawnEffect.render(graphicsContext, camera);
                } else {
                    // khi da spawn xong ve bong va player binh thuong
                    ShadowRenderer.render(graphicsContext, camera, player.getPosition(), 22.0, 6.0);
                    player.render(graphicsContext, camera);
                }
            }));
        }

        // C. Thêm PET
        if (currentPet != null && currentPet.getPosition() != null) {
            double petFootY = currentPet.getPosition().getY() + 8.0;
            renderList.add(new SortableObject(petFootY, () -> {
                boolean isSpawning = petSpawnEffect != null && petSpawnEffect.isSpawning();

                if (isSpawning) {
                    double alpha = petSpawnEffect.getEntityAlpha();

                    if (alpha > 0.0) {
                        graphicsContext.save();
                        graphicsContext.setGlobalAlpha(alpha);
                        ShadowRenderer.render(graphicsContext, camera, currentPet.getPosition(), 16.0, 4.0, 8.0, false);
                        graphicsContext.restore();
                    }

                    graphicsContext.save();
                    graphicsContext.setGlobalAlpha(alpha);
                    currentPet.render(graphicsContext, camera);
                    graphicsContext.restore();

                    petSpawnEffect.render(graphicsContext, camera);
                } else {
                    ShadowRenderer.render(graphicsContext, camera, currentPet.getPosition(), 16.0, 4.0, 8.0, false);
                    currentPet.render(graphicsContext, camera);
                }
            }));
        }
        // D. Thêm ENEMIES
        for (Enemy enemy : enemies) {
            if (enemy == null || enemy.getPosition() == null) continue;
            double enemyDiameter = enemy.getRadius() * 2.0;
            if (!isVisibleInCamera(enemy.getPosition().getX() - enemy.getRadius(),
                    enemy.getPosition().getY() - enemy.getRadius(), enemyDiameter, enemyDiameter,
                    renderWidth, renderHeight)) {
                continue;
            }

            double enemyFootY = enemy.getPosition().getY() + 12.0;
            renderList.add(new SortableObject(enemyFootY, () -> {
                // Vẽ bóng Enemy (Width: 22, Height: 6, OffsetY: 10)
                ShadowRenderer.render(graphicsContext, camera, enemy.getPosition(), 22.0, 6.0, 10.0, false);

                enemy.render(graphicsContext, camera);
            }));
        }

        // E. Thêm thân/sprite OBSTACLE vào Y-Sorting. Bóng đã được vẽ trên sàn ở phía trên.
        for (Obstacle obstacle : obstacles) {
            if (obstacle == null || obstacle.isDestroyed() || obstacle.getPosition() == null) {
                continue;
            }

            if (!isVisibleInCamera(obstacle.getPosition().getX(), obstacle.getPosition().getY(),
                    obstacle.getWidth(), obstacle.getHeight(), renderWidth, renderHeight)) {
                continue;
            }

            double obstacleBottomY = obstacle.getPosition().getY() + obstacle.getHeight();
            renderList.add(new SortableObject(obstacleBottomY,
                    () -> obstacle.render(graphicsContext, camera)));
        }
        // E. Thêm các ô CỬA (DOORS) vào Y-Sorting (Mốc Y tính ở ĐÁY BoundingBox Cửa)
        for (Room room : mapManager.getRooms()) {
            for (BoundingBox door : room.getDoors()) {
                if (door == null) continue;
                if (!isVisibleInCamera(door.getMinX(), door.getMinY(), door.getWidth(), door.getHeight(),
                        renderWidth, renderHeight)) {
                    continue;
                }

                // ĐÁY CỦA Ô CỬA LÀM MỐC XẾP LỚP 2.5D
                double doorBottomY = door.getMaxY();

                renderList.add(new SortableObject(doorBottomY, () -> {
                    room.renderSingleDoor(graphicsContext, camera, door, tileSize);
                }));
            }
        }


        renderList.sort((a, b) -> Double.compare(a.depthY, b.depthY));

        // Thực thi render theo thứ tự sâu/nông
        for (SortableObject obj : renderList) {
            obj.renderAction.run();
        }

        // 3. Hiệu ứng đạn, chém, nổ vẽ lên trên cùng
        for (Item item : items) item.render(graphicsContext, camera);
        for (Bullet bullet : bullets) {
            if (bullet == null || bullet.getPosition() == null) {
                continue;
            }
            double diameter = bullet.getRadius() * 2.0;
            if (!isVisibleInCamera(bullet.getPosition().getX() - bullet.getRadius(),
                    bullet.getPosition().getY() - bullet.getRadius(), diameter, diameter,
                    renderWidth, renderHeight)) {
                continue;
            }
            bullet.render(graphicsContext, camera);
        }
        for (SlashEffect slash : slashEffects) slash.render(graphicsContext, camera);
        for (ExplosionEffect explosion : explosions) explosion.render(graphicsContext, camera);
        if (particleManager != null) {
            particleManager.render(graphicsContext, camera);
        }
    }


    private void startNewRun() {
        levelManager.startNewRun();
        currentRoomNumber = 1;
        loadCurrentLevel(true);
        SoundManager.getInstance().playBGM("/assets/Audio/StartGame.mp3");
    }

    //(vitdung) chỉnh lại hàm này để test loadMap từ txt
    private void loadCurrentLevel(boolean freshRun) {
        String mapPath = "/maps/primeMap_1.json";
        this.mapManager = new MapManager(mapPath);
        this.mapManager.closeExitPortal();
        this.pendingPortalPosition = null;
        loadObstaclesFromCurrentMap();


        // 3. Khởi tạo hoặc Đặt lại vị trí Người chơi (Player)
        Vector2D spawnPoint = mapManager.getSpawnPoint();
        if (this.player == null || freshRun) {
            this.player = new Player(spawnPoint);
        } else {
            this.player.getPosition().set(spawnPoint);
        }


        /*
         * Player phải được tạo trước rồi mới khôi phục HP
         * và các dữ liệu trong database.
         */
        applyPendingPlayerSave();
//        dua player den phong da luu
        restorePlayerRoomPosition();
        refreshCurrentRoomReference();

        // 4. Khởi tạo Pet đi theo
        createSelectedPet();
//        tao hieu ung spawn
        this.playerSpawnEffect = new SpawnEffect(spawnPoint, 0.4, 0.7);
        if (currentPet != null) {
            // player xuat hien truoc pet xuat hien sau mot chut
            Vector2D petSpawnPos = new Vector2D(spawnPoint.getX() + 25, spawnPoint.getY() + 10);
            currentPet.getPosition().set(petSpawnPos);
            this.petSpawnEffect = new SpawnEffect(petSpawnPos, 0.55,0.7);
        }

        // 5. Trang bị vũ khí cho lượt chơi mới
        if (freshRun) {
            this.player.equipWeapon(WeaponSelectionManager.getInstance().getSelectedWeapon().createWeapon());
        }

        // 6. Reset toàn bộ danh sách Thực thể & Hiệu ứng của màn cũ
        this.enemies.clear();
        this.bullets.clear();
        this.explosions.clear();
        this.slashEffects.clear();
        this.items.clear();
        this.destroyedObstacleQueue.clear();
        this.enemySpawnTimer = 0.0;

        // 7. Tạo nhiệm vụ cho Level hiện tại
        if (this.missionManager != null && this.levelManager != null) {
            this.missionManager.setMission(this.levelManager.createMissionForCurrentLevel());
        }

        // 8. Sinh các vật phẩm đặc thù theo Level
        if (this.levelManager != null && this.levelManager.getCurrentLevel().number() == 2) {
            spawnEnergyCrystals(3);
        }

        // 9. Sinh Boss nếu đây là Màn Boss
        if (this.levelManager != null
                && this.levelManager.getCurrentLevel().bossLevel()
                && this.mapManager.getBossSpawnPoint() != null) {
            this.enemies.add(this.enemyFactory.createGrandKnight(this.mapManager.getBossSpawnPoint()));
        }
    }
    private void refreshCurrentRoomReference() {
        currentRoom = null;

        if (player == null || player.getPosition() == null || mapManager == null || mapManager.getRooms() == null) {
            return;
        }

        double playerX = player.getPosition().getX();
        double playerY = player.getPosition().getY();

        List<Room> rooms = mapManager.getRooms();

        for (int i = 0; i < rooms.size(); i++) {
            Room room = rooms.get(i);

            if (room == null || room.getBound() == null) {
                continue;
            }

            if (room.getBound().contains(playerX, playerY)) {
                currentRoom = room;
                currentRoomNumber = i + 1;
                return;
            }
        }
    }
    private void createSelectedPet() {
        if (player == null) {
            currentPet = null;
            return;
        }

        // Lấy loại Pet đã được lưu trong PetSelectionManager (từ Shop)
        PetType selectedType = PetSelectionManager.getInstance().getSelectedPet();

        if (selectedType != null && selectedType.hasPet()) {
            currentPet = PetFactory.create(
                    selectedType,
                    player.getPosition().getX(),
                    player.getPosition().getY()
            );
        } else {
            currentPet = null;
        }
    }
    private void loadObstaclesFromCurrentMap() {
        if (mapManager == null || mapManager.getRooms() == null) {
            return;
        }
        Tile[][] currentTiles = mapManager.getTiles();

        for (Room room : mapManager.getRooms()) {
            if (room == null) {
                continue;
            }

            // Tile map là nguồn dữ liệu duy nhất của obstacle; chỉ load một lần khi đổi map/level.
            room.loadObstaclesFromTiles(currentTiles);
        }

        rebuildObstacleCache();
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
            saveGameAsync();
        } else {
            changeState(GameState.GAME_VICTORY);
            saveGameAsync();
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


    public String getCurrentPlayerName() {
        return currentPlayerName;
    }

    public void setCurrentPlayerName(String currentPlayerName) {
        if (currentPlayerName == null || currentPlayerName.isBlank()) {
            this.currentPlayerName = "Knight";
            return;
        }

        this.currentPlayerName = currentPlayerName.trim();
    }

    public int getGold() {
        return gold;
    }

    public void setGold(int gold) {
        this.gold = Math.max(0, gold);
    }

    public void addGold(int amount) {
        if (amount > 0) {
            gold += amount;
        }
    }

    public boolean spendGold(int amount) {
        if (amount <= 0 || gold < amount) {
            return false;
        }

        gold -= amount;
        return true;
    }

    public int getGems() {
        return gems;
    }

    public void setGems(int gems) {
        this.gems = Math.max(0, gems);
    }

    public void addGems(int amount) {
        if (amount > 0) {
            gems += amount;
        }
    }

    public boolean spendGems(int amount) {
        if (amount <= 0 || gems < amount) {
            return false;
        }

        gems -= amount;
        return true;
    }
    public List<Obstacle> getObstacles() {
        return readOnlyObstacles;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = Math.max(0, score);
    }

    public void addScore(int amount) {
        if (amount > 0) {
            score += amount;
        }
    }

    public int getCurrentRoomNumber() {
        return currentRoomNumber;
    }

    public void setCurrentRoomNumber(int currentRoomNumber) {
        this.currentRoomNumber = Math.max(1, currentRoomNumber);
    }

    public double getPlayerEnergy() {
        return playerEnergy;
    }

    public void setPlayerEnergy(double playerEnergy) {
        this.playerEnergy = Math.max(0.0, playerEnergy);
    }


    // Doi qua lai giua sung va kiem (bam nut vu khi tren HUD de test)
    public void switchPlayerWeapon() {
        if (player == null) {
            return;
        }
        if (player.getWeapon() instanceof Melee) {
            // Dang cam kiem -> doi sang sung
            player.equipWeapon(WeaponType.BLASTER.createWeapon());
        } else {
            // Dang cam sung -> doi sang kiem (chi so lay tu WeaponType de khong lech tam danh)
            player.equipWeapon(WeaponType.OLD_SWORD.createWeapon());
        }
    }

    public Vector2D getMouseWorldPosition() {
//        xu li ngam ban tu chuot
        return camera.screenToWorld(inputHandler.getMousePosition());
    }
    //Kiem tra xem di duoc khong
    public boolean canMoveTo(Vector2D position, double radius) {
        if (position == null || mapManager == null || radius < 0.0) {
            return false;
        }

        if (!mapManager.isWalkable(position.getX(), position.getY(), radius)) {
            return false;
        }

        for (Obstacle obstacle : obstacles) {
            if (obstacle == null || obstacle.isDestroyed()) {
                continue;
            }

            if (obstacle.intersectsCircle(position, radius)) {
                return false;
            }
        }
        return true;
    }
    public boolean isPlaying() {
        return state == GameState.PLAYING;
    }
    public void spawnEnemiesInRoom(com.soulknight.map.Room room, int waveNumber){
        List<Vector2D> roomSpawnPoints = new ArrayList<>();
        javafx.geometry.BoundingBox bound = room.getBound();

        int baseEnemyCount = enemyFactory.calculateEnemyCount();
        int desiredEnemyCount = baseEnemyCount + (waveNumber - 1);
        final int MAX_ENEMIES_PER_ROOM = Math.min(3 + (waveNumber / 2), 6);
        int finalEnemyCount = Math.min(desiredEnemyCount, MAX_ENEMIES_PER_ROOM);
        final double MIN_SAFE_DISTANCE = 140.0;
        for (int i = 0; i < finalEnemyCount; i++) {
            Vector2D point = null;
            boolean validPointFound = false;

            for (int attempt = 0; attempt < 20; attempt++) {
                Vector2D randomPoint = mapManager.findRandomWalkablePositionInRoom(room, random, Constants.ENEMY_RADIUS);

                if (randomPoint != null && bound.contains(randomPoint.getX(), randomPoint.getY())) {
                    if (randomPoint.distance(player.getPosition()) >= MIN_SAFE_DISTANCE) {

                        // Giữ khoảng cách giữa các con quái với nhau, tránh sinh đè lên nhau
                        boolean tooCloseToOtherEnemies = false;
                        for (Vector2D existingPoint : roomSpawnPoints) {
                            if (randomPoint.distance(existingPoint) < 45.0) { // cách nhau tối thiểu 40px
                                tooCloseToOtherEnemies = true;
                                break;
                            }
                        }
//                        Kiem tra vi tri co dung vao vat can hay khong
                        boolean insideObstacle = intersectsRoomObstacle(room, randomPoint, Constants.ENEMY_RADIUS);

                        if (!tooCloseToOtherEnemies && !insideObstacle) {
                            point = randomPoint;
                            validPointFound = true;
                            break;
                        }
                    }
                }
            }

            if (!validPointFound) {
                // Không ép spawn ở tâm phòng vì vị trí đó có thể nằm trong obstacle.
                continue;
            }

            roomSpawnPoints.add(point);
        }

        // sinh quái tùy theo wave
        // 3. KHỞI TẠO ĐA DẠNG LOẠI QUÁI DỰA TRÊN WAVE
        for (Vector2D spawnPt : roomSpawnPoints) {
            // Tự động sinh ngẫu nhiên Slime / Cung thủ / Lợn rừng dựa theo wave
            Enemy enemy = enemyFactory.createEnemyByWave(random, spawnPt, waveNumber);
            enemies.add(enemy);
        }
    }
    private boolean intersectsRoomObstacle(Room room, Vector2D position, double radius) {
        if (room == null || position == null || room.getObstacles() == null) {
            return false;
        }

        for (Obstacle obstacle : room.getObstacles()) {
            if (obstacle == null || obstacle.isDestroyed()) {
                continue;
            }
            if (obstacle.intersectsCircle(position, radius)) {
                return true;
            }
        }
        return false;
    }

    //    xu li va cham giua entity va entity - giua quai va player
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
    // Gay sat thuong theo hinh quat: chi trung ke dich nam trong tam danh
    // va lech khong qua halfArcRadians so voi huong ngam (dung cho vu khi can chien)
    public void damageEnemiesInArc(Vector2D origin, double aimAngle, double range, double halfArcRadians, int damage) {
        for (Enemy enemy : enemies) {
            if (!enemy.isAlive()) {
                continue;
            }
            double dx = enemy.getPosition().getX() - origin.getX();
            double dy = enemy.getPosition().getY() - origin.getY();
            double distance = Math.hypot(dx, dy);
            if (distance > range + enemy.getRadius()) {
                continue;
            }
            // Ke dich dinh sat nguoi thi luon trung, khong can xet goc
            if (distance > enemy.getRadius()) {
                double angleToEnemy = Math.atan2(dy, dx);
                // Chuan hoa do lech goc ve [-PI, PI] roi so voi nua goc quet
                double diff = Math.atan2(Math.sin(angleToEnemy - aimAngle), Math.cos(angleToEnemy - aimAngle));
                if (Math.abs(diff) > halfArcRadians) {
                    continue;
                }
            }
            enemy.takeDamage(damage);
//            chem enemy sing ra tia lua
            if (particleManager != null) {
                particleManager.spawnHitImpact(enemy.getPosition());
            }
        }

        // Nhat chem cung pha duoc vat can (hom go) trong hinh quat
        for (Obstacle obstacle : obstacles) {
            if (obstacle == null || obstacle.isDestroyed() || !obstacle.isDestructible()) {
                continue;
            }
            if (!obstacle.intersectsCircle(origin, range)) {
                continue;
            }
            // Xet goc theo diem gan nhat tren vat can (vat can to nen tam co the lech ngoai cung)
            double closestX = Math.max(obstacle.getPosition().getX(),
                    Math.min(origin.getX(), obstacle.getPosition().getX() + obstacle.getWidth()));
            double closestY = Math.max(obstacle.getPosition().getY(),
                    Math.min(origin.getY(), obstacle.getPosition().getY() + obstacle.getHeight()));
            double dx = closestX - origin.getX();
            double dy = closestY - origin.getY();
            // Dang dung sat vat can thi luon trung, khong can xet goc
            if (dx != 0.0 || dy != 0.0) {
                double angleToObstacle = Math.atan2(dy, dx);
                double diff = Math.atan2(Math.sin(angleToObstacle - aimAngle), Math.cos(angleToObstacle - aimAngle));
                if (Math.abs(diff) > halfArcRadians) {
                    continue;
                }
            }
            boolean wasAlive = !obstacle.isDestroyed();
            obstacle.takeDamage(damage);
            Vector2D impactPoint = new Vector2D(closestX, closestY);
            if (particleManager != null) {
                particleManager.spawnMeleeObstacleImpact(impactPoint, aimAngle);
            }
            if (wasAlive && obstacle.isDestroyed() && !destroyedObstacleQueue.contains(obstacle)) {
                destroyedObstacleQueue.add(obstacle);
            }
        }
    }
    /**
     * Kiem tra xem duong ngam ban co vat can khong
     * giong thuat toan trong bai co tuong
     */
    public boolean hasClearLineOfSight(Vector2D start, Vector2D end) {
        double distance = start.distance(end);
        if (distance == 0) return true;
        double stepSize = 12.0;
        int steps = (int) (distance / stepSize);

        Vector2D direction = end.copy().subtract(start).normalize();
        for (int i = 1; i <= steps; i++) {
            Vector2D checkPoint = start.copy().add(direction.getX() * (i * stepSize), direction.getY() * (i * stepSize));
            if (!mapManager.isWalkable(checkPoint.getX(), checkPoint.getY(), 4.0)) {
                return false; // Bị tường che
            }
        }
//        Kiem tra xem co trung vat can nao tren duong hay khong
        for (Obstacle obstacle : obstacles) {
            if (obstacle == null || obstacle.isDestroyed()) continue;

            // BoundingBox cua vat can
            double minX = obstacle.getPosition().getX();
            double minY = obstacle.getPosition().getY();
            double maxX = minX + obstacle.getWidth();
            double maxY = minY + obstacle.getHeight();

            // Thuật toán kiểm tra đoạn thẳng (start -> end) có cắt Hình chữ nhật (Obstacle) hay không
            if (lineIntersectsRect(start.getX(), start.getY(), end.getX(), end.getY(), minX, minY, maxX, maxY)) {
                return false; // Bị vật cản che
            }
        }

        return true; // Đường bắn hoàn toàn trống trải
    }

    /**
     * Thuật toán hỗ trợ kiểm tra đoạn thẳng cắt Hình chữ nhật (AABB)
     */
    private boolean lineIntersectsRect(double x1, double y1, double x2, double y2,
                                       double minX, double minY, double maxX, double maxY) {
        // Kiểm tra nhanh xem 2 điểm có nằm hẳn về 1 phía của Hộp không
        if ((x1 < minX && x2 < minX) || (x1 > maxX && x2 > maxX) ||
                (y1 < minY && y2 < minY) || (y1 > maxY && y2 > maxY)) {
            return false;
        }

        // Kiểm tra va chạm đoạn thẳng với 4 cạnh của hình chữ nhật
        return lineIntersectsLine(x1, y1, x2, y2, minX, minY, maxX, minY) ||
                lineIntersectsLine(x1, y1, x2, y2, minX, maxY, maxX, maxY) ||
                lineIntersectsLine(x1, y1, x2, y2, minX, minY, minX, maxY) ||
                lineIntersectsLine(x1, y1, x2, y2, maxX, minY, maxX, maxY);
    }

    private boolean lineIntersectsLine(double x1, double y1, double x2, double y2,
                                       double x3, double y3, double x4, double y4) {
        double denom = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1);
        if (denom == 0) return false;

        double ua = ((x4 - x3) * (y1 - y3) - (y4 - y3) * (x1 - x3)) / denom;
        double ub = ((x2 - x1) * (y1 - y3) - (y2 - y1) * (x1 - x3)) / denom;

        return (ua >= 0.0 && ua <= 1.0 && ub >= 0.0 && ub <= 1.0);
    }
    //    cac phuong thuc pet
    public void equipPet(PetType type) {
        PetType safeType = (type != null) ? type : PetType.NONE;

        // Luu lua chon
        PetSelectionManager.getInstance().selectPet(safeType);

        // Chua co player thi chi can luu lua chon
        if (player == null) {
            currentPet = null;
            return;
        }

        // Khoi tao instance pet
        if (safeType.hasPet()) {
            currentPet = PetFactory.create(
                    safeType,
                    player.getPosition().getX(),
                    player.getPosition().getY()
            );
        } else {
            currentPet = null;
        }
    }
    public void removePet() {
        equipPet(PetType.NONE);
    }
    public Pet getCurrentPet() {
        return currentPet;
    }
    public PetType getEquippedPetType() {
        return PetSelectionManager.getInstance().getSelectedPet();
    }

    //    cac phuong thuc vu khi (chon tu Shop)
    public void equipWeapon(WeaponType type) {
        WeaponType safeType = (type != null) ? type : WeaponType.OLD_PISTOL;

        // Luu lua chon
        WeaponSelectionManager.getInstance().selectWeapon(safeType);

        // Chua co player thi chi can luu lua chon
        if (player == null) {
            return;
        }

        player.equipWeapon(safeType.createWeapon());
    }
    //    data base
    public void saveGameAsync() {
        if (player == null) {
            debug("Chưa có Player nên chưa thể lưu game.");
            return;
        }

        if (!saveInProgress.compareAndSet(false, true)) {
            return;
        }

        final PlayerSave save;
        try {
            save = PlayerSaveMapper.fromWorld(this);
        } catch (RuntimeException exception) {
            saveInProgress.set(false);
            System.err.println("Không thể tạo dữ liệu save: " + exception.getMessage());
            return;
        }

        databaseExecutor.submit(() -> {
            try {
                PlayerSaveDAO dao = new PlayerSaveDAO();
                if (dao.save(save)) {
                    debug("Cloud Save thành công: " + save.getPlayerName());
                } else {
                    System.err.println("Cloud Save thất bại: " + save.getPlayerName());
                }
            } catch (RuntimeException exception) {
                System.err.println("Lỗi trong database worker: " + exception.getMessage());
            } finally {
                saveInProgress.set(false);
            }
        });
    }

    public void loadGameAsync(String playerName) {
        String safePlayerName = playerName == null || playerName.isBlank() ? "Knight" : playerName.trim();

        databaseExecutor.submit(() -> {
            try {
                PlayerSaveDAO dao = new PlayerSaveDAO();
                dao.findByName(safePlayerName).ifPresentOrElse(
                        save -> javafx.application.Platform.runLater(() -> {
                            pendingPlayerSave = save;
                            if (player != null) {
                                applyPendingPlayerSave();
                                restorePlayerRoomPosition();
                                refreshCurrentRoomReference();
                            }
                            debug("Cloud Load thành công: " + safePlayerName);
                        }),
                        () -> debug("Chưa có save của " + safePlayerName + ". Sẽ bắt đầu game mới.")
                );
            } catch (RuntimeException exception) {
                System.err.println("Cloud Load lỗi: " + exception.getMessage());
            }
        });
    }

    public void shutdown() {
        databaseExecutor.shutdown();
    }

    private void applyPendingPlayerSave() {
        if (pendingPlayerSave == null || player == null) {
            return;
        }

        PlayerSaveMapper.applyToWorld(this, pendingPlayerSave);

        debug("Đã áp dụng save vào Player.");

        pendingPlayerSave = null;
    }
    public boolean saveGameNow() {
        if (player == null) {
            return false;
        }

        try {
            PlayerSave save = PlayerSaveMapper.fromWorld(this);

            PlayerSaveDAO dao = new PlayerSaveDAO();

            boolean result = dao.save(save);

            if (result) {
                debug("Đã lưu game trước khi thoát.");
            }
            return result;

        } catch (RuntimeException exception) {
            System.err.println("Lỗi lưu khi thoát: " + exception.getMessage()
            );
            return false;
        }
    }
    private void removeDeadEnemiesAndGiveRewards() {
        for (int i = enemies.size() - 1; i >= 0; i--) {
            Enemy enemy = enemies.get(i);

            if (!enemy.isAlive()) {
                giveEnemyReward(enemy);
                enemies.remove(i);
            }
        }
    }
    private void giveEnemyReward(Enemy enemy) {
        if (enemy == null || enemy.getPosition() == null) {
            return;
        }

        int scoreReward = 100;
        int goldReward = random.nextInt(6) + 5;
        double gemDropChance = 0.15;

        addScore(scoreReward);

        Vector2D enemyPosition = enemy.getPosition().copy();
        Vector2D goldPosition = enemyPosition.copy().add(-8.0, 0.0);
        items.add(new GoldItem(goldPosition, goldReward, null));

        boolean droppedGem = random.nextDouble() < gemDropChance;
        if (droppedGem) {
            Vector2D gemPosition = enemyPosition.copy().add(8.0, 0.0);
            items.add(new GemItem(gemPosition, 1, null));
        }

        debug("Đã tiêu diệt quái | +" + scoreReward + " điểm");
        debug("Quái rơi " + goldReward + " vàng" + (droppedGem ? " và 1 kim cương." : "."));
    }
    private void updateAutoSave(double deltaSeconds) {
        autoSaveTimer += deltaSeconds;

        if (autoSaveTimer >= AUTO_SAVE_INTERVAL) {
            autoSaveTimer = 0.0;
            saveGameAsync();
        }
    }
    private void updateCurrentRoom() {
        if (player == null || player.getPosition() == null || mapManager == null || mapManager.getRooms() == null) {
            return;
        }
        double playerX = player.getPosition().getX();
        double playerY = player.getPosition().getY();
        if (currentRoom != null && currentRoom.getBound() != null && currentRoom.getBound().contains(playerX, playerY)) {
            return;
        }
        List<Room> rooms = mapManager.getRooms();

        for (int i = 0; i < rooms.size(); i++) {
            Room room = rooms.get(i);

            if (room == null || room.getBound() == null) {
                continue;
            }

            if (!room.getBound().contains(playerX, playerY)) {
                continue;
            }

            boolean changedRoom = room != currentRoom;
            currentRoom = room;
            currentRoomNumber = i + 1;
            if (changedRoom) {
                debug("Player da vao phong " + currentRoomNumber);
                saveGameAsync();
            }
            return;
        }
    }
    private void restorePlayerRoomPosition() {
        if (player == null || mapManager == null || mapManager.getRooms() == null || mapManager.getRooms().isEmpty()) {
            return;
        }
        int roomIndex = currentRoomNumber - 1;
        if (roomIndex < 0 || roomIndex >= mapManager.getRooms().size()) {
            currentRoomNumber = 1;
            return;
        }

        Room savedRoom = mapManager.getRooms().get(roomIndex);

        if (savedRoom == null || savedRoom.getBound() == null) {
            currentRoomNumber = 1;
            return;
        }

        double roomCenterX = savedRoom.getBound().getMinX() + savedRoom.getBound().getWidth() / 2.0;
        double roomCenterY = savedRoom.getBound().getMinY() + savedRoom.getBound().getHeight() / 2.0;
        Vector2D roomCenter = new Vector2D(roomCenterX, roomCenterY);
        if (canMoveTo(roomCenter, player.getRadius())) {
            player.getPosition().set(roomCenter);
            debug("Đã khôi phục Player tại phòng " + currentRoomNumber);
        }
    }
    // Cache obstacle để các phép va chạm không phải tạo ArrayList mới mỗi lần gọi.
    private void rebuildObstacleCache() {
        obstacles.clear();

        if (mapManager == null || mapManager.getRooms() == null) {
            return;
        }

        for (Room room : mapManager.getRooms()) {
            if (room == null || room.getObstacles() == null) {
                continue;
            }

            for (Obstacle obstacle : room.getObstacles()) {
                if (obstacle != null && !obstacle.isDestroyed()) {
                    obstacles.add(obstacle);
                }
            }
        }
    }
    private void debug(String message) {
        if (DEBUG_LOGGING) {
            System.out.println(message);
        }
    }

    private boolean isVisibleInCamera(double worldX, double worldY, double objectWidth, double objectHeight,
                                      double renderWidth, double renderHeight) {
        double zoom = camera.getZoom();
        double screenX = camera.worldToScreenX(worldX);
        double screenY = camera.worldToScreenY(worldY);
        double screenWidth = objectWidth * zoom;
        double screenHeight = objectHeight * zoom;
        double margin = 64.0;

        return screenX + screenWidth >= -margin
                && screenY + screenHeight >= -margin
                && screenX <= renderWidth + margin
                && screenY <= renderHeight + margin;
    }

    private void damageObstacle(Obstacle obstacle, int damage, Vector2D impactPosition) {
        if (obstacle == null || obstacle.isDestroyed()) {
            return;
        }

        obstacle.takeDamage(damage);

        if (impactPosition != null) {
            particleManager.spawnHitImpact(impactPosition);
        }

        if (obstacle.isDestroyed() && !destroyedObstacleQueue.contains(obstacle)) {
            destroyedObstacleQueue.add(obstacle);
        }
    }

}
//NOTE : cac ham xu ly va cham
// Player - titled (mapmanager): cua room
//Player - enemy (gameworld)
//enemy-enemy - (enemy)
//bullet - wall
//giua vat can player , enemy , bullet - xu li su khac nhau giua dan cua enemy voi vat can