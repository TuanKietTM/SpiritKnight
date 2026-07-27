package com.soulknight.engine;

import com.soulknight.entity.Enemy;
import com.soulknight.entity.EnemyArchetype;
import com.soulknight.entity.EnemyFactory;
import com.soulknight.entity.Player;
import com.soulknight.item.EnergyCrystal;
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
import com.soulknight.weapon.Gun;
import com.soulknight.weapon.Melee;
import com.soulknight.weapon.SlashEffect;
import com.soulknight.weapon.Weapon;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

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
    // Danh sach hieu ung no khi dan va cham (tuong hoac muc tieu)
    private final List<ExplosionEffect> explosions = new ArrayList<>();
    // Danh sach hieu ung chem cua vu khi can chien (kiem)
    private final List<SlashEffect> slashEffects = new ArrayList<>();
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
            case PLAYING -> updatePlaying(deltaSeconds, viewportWidth, viewportHeight, false);
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
            // xử lý va chạm quái vs quái
            enemy.separateFromOtherEnemies(this,enemies, deltaSeconds);
        }
        resolvePlayerEnemyCollisions(deltaSeconds);

        updateBullets(deltaSeconds);
        updateExplosions(deltaSeconds);
        updateSlashEffects(deltaSeconds);
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
    }

    private void updateLevelClear(double deltaSeconds, double viewportWidth, double viewportHeight) {
        player.update(this, deltaSeconds);
        camera.follow(player.getPosition(), viewportWidth, viewportHeight, mapManager.getWorldWidth(), mapManager.getWorldHeight());

        if (mapManager.isPlayerInsideExitPortal(player.getPosition())) {
            advanceToNextLevel();
        }
    }

    private void updateBullets(double deltaSeconds) {
        List<Obstacle> allObstacles = getObstacles();

        for (Bullet bullet : bullets) {
            if (!bullet.isActive()) continue;

            // KIỂM TRA ĐẠN VỪA BẮN RA ĐÃ NẰM TRONG TƯỜNG CỨNG CHƯA?
            // Tránh lỗi đạn kẹt đệ quy gây StackOverflow khi đứng sát tường
            if (mapManager.isBulletCollidingWithWall(bullet.getPosition().getX(), bullet.getPosition().getY())) {
                bullet.deactivate();
                continue; // Hủy đạn ngay lập tức, không spawn nổ liên tục
            }

            // 2. Cho đạn di chuyển
            bullet.update(deltaSeconds);

            // 🎯 3. KIỂM TRA VA CHẠM TƯỜNG (Dùng hàm riêng cho Đạn thay vì isWalkable)
            if (mapManager.isBulletCollidingWithWall(bullet.getPosition().getX(), bullet.getPosition().getY())) {
                spawnBulletExplosion(bullet.getPosition());
                bullet.deactivate();
                continue;
            }

            // 4. Đạn va chạm với Vật cản (Thực hiện TRƯỚC khi va chạm Quái/Player)
            boolean hitObstacle = false;
            for (Obstacle obstacle : allObstacles) {
                if (obstacle.intersectsCircle(bullet.getPosition(), bullet.getRadius())) {
                    obstacle.takeDamage(bullet.getDamage());
                    spawnBulletExplosion(bullet.getPosition());
                    bullet.deactivate();
                    hitObstacle = true;
                    break;
                }
            }
            if (hitObstacle) {
                continue;
            }

            // 5. Va chạm với Entity (Enemy / Player)
            if (bullet.getOwner() instanceof Player) {
                for (Enemy enemy : enemies) {
                    if (enemy.isAlive() && bullet.intersects(enemy)) {
                        enemy.takeDamage(bullet.getDamage());
                        spawnBulletExplosion(bullet.getPosition());
                        bullet.deactivate();
                        break;
                    }
                }
            } else if (bullet.intersects(player)) {
                player.takeDamage(bullet.getDamage());
                spawnBulletExplosion(bullet.getPosition());
                bullet.deactivate();
            }
        }

        // Xóa đạn và quái chết
        bullets.removeIf(bullet -> !bullet.isActive());
        enemies.removeIf(enemy -> !enemy.isAlive());

        // Xóa vật cản nếu bị phá hủy
        if (mapManager != null && mapManager.getRooms() != null) {
            for (Room room : mapManager.getRooms()) {
                if (room.getObstacles() != null) {
                    room.getObstacles().removeIf(Obstacle::isDestroyed);
                }
            }
        }
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

    // Tao hieu ung chem truoc mat nhan vat theo huong ngam (goi tu vu khi can chien)
    public void spawnMeleeSlash(Vector2D origin, double aimAngle, double range) {
        slashEffects.add(new SlashEffect(origin.copy(), aimAngle, range, 0.22));
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
        // 1. Vẽ sàn nhà bẹt dưới cùng trước
        mapManager.renderFloor(graphicsContext, camera, renderWidth, renderHeight);

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
            double wallBottomY = wall.getY() + tileSize; // 🎯 ĐÁY Ô TƯỜNG
            renderList.add(new SortableObject(wallBottomY, () -> {
                double screenX = camera.worldToScreenX(wall.getX());
                double screenY = camera.worldToScreenY(wall.getY());
                double zoom = camera.getZoom();
                graphicsContext.drawImage(wall.getTexture(), screenX, screenY, tileSize * zoom, tileSize * zoom);
            }));
        }

// B. Thêm PLAYER (Mốc Y tính ở BÀN CHÂN)
        double playerFootY = player.getPosition().getY() + 10.0;
        renderList.add(new SortableObject(playerFootY, () -> {
            player.render(graphicsContext, camera);
        }));

        // C. Thêm ENEMIES
        for (Enemy enemy : enemies) {
            double enemyFootY = enemy.getPosition().getY() + 12.0;
            renderList.add(new SortableObject(enemyFootY, () -> {
                enemy.render(graphicsContext, camera);
            }));
        }

        // D. Thêm OBSTACLES (Vật cản)
        if (mapManager != null && mapManager.getRooms() != null) {
            for (Room room : mapManager.getRooms()) {
                if (room.getObstacles() != null) {
                    for (Obstacle obstacle : room.getObstacles()) {
                        double obsY = obstacle.getPosition().getY() + 16.0;
                        renderList.add(new SortableObject(obsY, () -> {
                            obstacle.render(graphicsContext, camera);
                        }));
                    }
                }
            }
        }


        renderList.sort((a, b) -> Double.compare(a.depthY, b.depthY));

        // Thực thi render theo thứ tự sâu/nông
        for (SortableObject obj : renderList) {
            obj.renderAction.run();
        }

        // 3. Hiệu ứng đạn, chém, nổ vẽ lên trên cùng
        for (Item item : items) item.render(graphicsContext, camera);
        for (Bullet bullet : bullets) bullet.render(graphicsContext, camera);
        for (SlashEffect slash : slashEffects) slash.render(graphicsContext, camera);
        for (ExplosionEffect explosion : explosions) explosion.render(graphicsContext, camera);
    }

    private void startNewRun() {
        levelManager.startNewRun();
        loadCurrentLevel(true);
        SoundManager.getInstance().playBGM("/assets/Audio/StartGame.mp3");
    }

    //(vitdung) chỉnh lại hàm này để test loadMap từ txt
    private void loadCurrentLevel(boolean freshRun) {
        String mapPath = "/maps/primeMap_1.json";
        this.mapManager = new MapManager(mapPath);
        this.mapManager.closeExitPortal();
        if (mapManager != null && mapManager.getRooms() != null) {
            for (Room room : mapManager.getRooms()) {
                // Không tạo vật cản ở phòng xuất phát để Player dễ di chuyển
                if (room.getName() != null && !room.getName().equalsIgnoreCase("StartRoom")) {
                    spawnObstaclesInRoom(room, 3); // Sinh 3 vật cản mỗi phòng
                }
            }
        }
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
            player.equipWeapon(new Gun("Blaster", 12, 0.18, 580.0, 0.0)
                    .withImage("/assets/WeaponImage/GunImage/OldPistol.png"));
        }

        // reset lại các object
        enemies.clear();
        bullets.clear();
        explosions.clear();
        slashEffects.clear();
        items.clear();
        enemySpawnTimer = 0.0;

        // chỗ nhiệm vụ này chưa cho vào chế độ thường
        missionManager.setMission(levelManager.createMissionForCurrentLevel());

        if (levelManager.getCurrentLevel().number() == 2) {
            spawnEnergyCrystals(3);
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

    // Doi qua lai giua sung va kiem (bam nut vu khi tren HUD de test)
    public void switchPlayerWeapon() {
        if (player == null) {
            return;
        }
        if (player.getWeapon() instanceof Melee) {
            // Dang cam kiem -> doi sang sung
            player.equipWeapon(new Gun("Blaster", 12, 0.18, 580.0, 0.0)
                    .withImage("/assets/WeaponImage/GunImage/OldPistol.png"));
        } else {
            // Dang cam sung -> doi sang kiem
            player.equipWeapon(new Melee("Old Sword", 25, 0.35, 60.0)
                    .withImage("/assets/WeaponImage/MeleeImage/Sprite_Old_Sword_of_Royal_Guard.png"));
        }
    }

    public Vector2D getMouseWorldPosition() {
//        xu li ngam ban tu chuot
        return camera.screenToWorld(inputHandler.getMousePosition());
    }
//Kiem tra xem di duoc khong
public boolean canMoveTo(Vector2D position, double radius) {
    // 1. Kiểm tra va chạm với Tường/Bản đồ
    if (!mapManager.isWalkable(position.getX(), position.getY(), radius)) {
        return false;
    }

    // 2. Kiểm tra va chạm với các Vật cản chưa bị phá hủy
    for (Obstacle obstacle : getObstacles()) {
        if (obstacle.intersectsCircle(position, radius)) {
            return false; // Bị cản lại, không cho đi qua
        }
    }

    return true;
}

    public boolean isPlaying() {
        return state == GameState.PLAYING;
    }
    public void spawnEnemiesInRoom(com.soulknight.map.Room room, int waveNumber)
    {
        if (room.getObstacles().isEmpty()) {
            spawnObstaclesInRoom(room, 3);
        }
        List<Vector2D> roomSpawnPoints = new ArrayList<>();
        javafx.geometry.BoundingBox bound = room.getBound();

        int baseEnemyCount = enemyFactory.calculateEnemyCount();

        //  Lấy số lượng quái dự kiến từ Factory dựa trên level hiện tại
        int desiredEnemyCount = baseEnemyCount + (waveNumber - 1);


        // 2. : Đảm bảo số quái một phòng không bao giờ vượt quá mức cho phép
        final int MAX_ENEMIES_PER_ROOM = Math.min(3 + (waveNumber / 2), 6);
        int finalEnemyCount = Math.min(desiredEnemyCount, MAX_ENEMIES_PER_ROOM);

        // Khoảng cách an toàn tối thiểu giữa quái và Player
        final double MIN_SAFE_DISTANCE = 140.0;

        // Vòng lặp chỉ chạy đúng bằng số lượng quái cần sinh thực tế
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
                        boolean insideObstacle = false;
                        for (Obstacle obstacle : getObstacles()) {
                            if (obstacle.intersectsCircle(randomPoint, Constants.ENEMY_RADIUS)) {
                                insideObstacle = true;
                                break;
                            }
                        }

                        if (!tooCloseToOtherEnemies && !insideObstacle) {
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

        // sinh quái tùy theo wave
        // 3. KHỞI TẠO ĐA DẠNG LOẠI QUÁI DỰA TRÊN WAVE
        for (Vector2D spawnPt : roomSpawnPoints) {
            // Tự động sinh ngẫu nhiên Slime / Cung thủ / Lợn rừng dựa theo wave
            Enemy enemy = enemyFactory.createEnemyByWave(random, spawnPt, waveNumber);
            enemies.add(enemy);
        }
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
    /**
     * Lấy toàn bộ danh sách Obstacle từ tất cả các phòng trên bản đồ hiện tại.
     */
    public List<Obstacle> getObstacles() {
        List<Obstacle> allObstacles = new ArrayList<>();
        if (mapManager != null && mapManager.getRooms() != null) {
            for (Room room : mapManager.getRooms()) {
                if (room.getObstacles() != null) {
                    allObstacles.addAll(room.getObstacles());
                }
            }
        }
        return allObstacles;
    }

    /**
     * Thêm thủ công một Obstacle vào một Room cụ thể.
     */
    public void addObstacleToRoom(Room room, Obstacle obstacle) {
        if (room != null && obstacle != null) {
            room.getObstacles().add(obstacle);
        }
    }
    /**
     * Sinh số lượng vật cản ngẫu nhiên vào Room, đảm bảo không đè tường và không chồng lên nhau.
     */
    public void spawnObstaclesInRoom(Room room, int maxCount) {
        if (room == null || room.getObstacles() == null) return;

        javafx.geometry.BoundingBox bound = room.getBound();
        double obsSize = 40.0;             // Kích thước vật cản
        double minWallPadding = 80.0;       // Khoảng cách an toàn tối thiểu cách tường
        double minObsDistance = 60.0;       // Khoảng cách tối thiểu giữa các vật cản

        int attempts = 0;
        while (room.getObstacles().size() < maxCount && attempts < 50) {
            attempts++;

            double minX = bound.getMinX() + minWallPadding;
            double minY = bound.getMinY() + minWallPadding;
            double maxX = bound.getMinX() + bound.getWidth() - minWallPadding - obsSize;
            double maxY = bound.getMinY() + bound.getHeight() - minWallPadding - obsSize;

            if (maxX <= minX || maxY <= minY) break;

            double x = minX + random.nextDouble() * (maxX - minX);
            double y = minY + random.nextDouble() * (maxY - minY);
            Vector2D newPos = new Vector2D(x, y);

            // Kiểm tra trùng lặp vị trí với các vật cản đã sinh trước đó
            boolean isOverlapped = false;
            for (Obstacle existing : room.getObstacles()) {
                if (newPos.distance(existing.getPosition()) < minObsDistance) {
                    isOverlapped = true;
                    break;
                }
            }

            // Nếu hợp lệ thì thêm vào phòng
            if (!isOverlapped) {
                boolean isDestructible = random.nextDouble() > 0.2; // 80% hòm gỗ (phá được), 20% cột đá
                Obstacle obstacle = new Obstacle(newPos, obsSize, obsSize, 30, isDestructible);
                room.getObstacles().add(obstacle);
            }
        }
    }
    public void damageEnemiesInRange(Vector2D origin, double range, int damage) {
        for (Enemy enemy : enemies) {
            if (enemy.isAlive() && enemy.getPosition().distance(origin) <= range + enemy.getRadius()) {
                enemy.takeDamage(damage);
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
        for (Obstacle obstacle : getObstacles()) {
            if (obstacle.isDestroyed()) continue;

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
}
//NOTE : cac ham xu ly va cham
// Player - titled (mapmanager): cua room
//Player - enemy (gameworld)
//enemy-enemy - (enemy)
//bullet - wall
//giua vat can player , enemy , bullet - xu li su khac nhau giua dan cua enemy voi vat can