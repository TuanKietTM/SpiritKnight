package com.soulknight.engine;

import com.soulknight.animation.ParticleManager;
import com.soulknight.animation.ShadowRenderer;
import com.soulknight.animation.SpawnEffect;
import com.soulknight.animation.PlayerDeathEffect;
import com.soulknight.animation.EnemySpawnEffect;
import com.soulknight.animation.EnemyDeathEffect;
import com.soulknight.entity.Enemy;
import com.soulknight.entity.EnemyFactory;
import com.soulknight.entity.Entity;
import com.soulknight.entity.Player;
import com.soulknight.item.EnergyCrystal;
import com.soulknight.item.GemItem;
import com.soulknight.item.GoldItem;
import com.soulknight.item.Item;
import com.soulknight.item.ItemMagnetSystem;
import com.soulknight.item.BuffItem;
import com.soulknight.buff.BuffInventoryManager;
import com.soulknight.buff.BuffType;
import com.soulknight.buff.effect.combat.CombatEffectManager;
import com.soulknight.buff.status.EnemyBurnManager;
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
import com.soulknight.weapon.render.IonExplosionEffect;
import com.soulknight.weapon.SlashEffect;
import com.soulknight.weapon.SoundWaveEffect;
import com.soulknight.weapon.Weapon;
import com.soulknight.weapon.WeaponSelectionManager;
import com.soulknight.weapon.WeaponType;
import com.soulknight.animation.EndingPortal;
import com.soulknight.pet.Pet;
import com.soulknight.pet.PetFactory;
import com.soulknight.pet.PetSelectionManager;
import com.soulknight.pet.PetType;
import com.soulknight.database.PlayerSave;
import com.soulknight.database.PlayerSaveDAO;
import com.soulknight.database.PlayerSaveMapper;
import com.soulknight.database.ShopDAO;
import com.soulknight.database.UserSession;
import com.soulknight.animation.FloatingTextManager;
import com.soulknight.map.RestRoomController;
import com.soulknight.map.RestShrine;
import com.soulknight.animation.RestHealEffect;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.IdentityHashMap;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.geometry.BoundingBox;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public final class GameWorld {

    private static final boolean DEBUG_LOGGING = false;
    private static final double ROOM_BUFF_DROP_CHANCE = 1;
    private static final double AUTO_SAVE_INTERVAL = 30.0;
    private static final double HOLY_NOVA_RADIUS_TILES = 20.0;
    private static final int HOLY_NOVA_DAMAGE = 200;
    private static final double DRAGON_BREATH_TILES = 10.0;
    private static final double DRAGON_BREATH_HALF_ANGLE = Math.toRadians(32.0);
    private static final int DRAGON_BREATH_DAMAGE = 5;
    private static final int DRAGON_EXPLOSION_DAMAGE = 33;
    private static final double DRAGON_EXPLOSION_RADIUS = 130.0;

    private final Random random = new Random();
    private final java.util.Set<Room> buffRewardedRooms = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private final java.util.Map<Room, Room.RoomState> previousRoomStates = new IdentityHashMap<>();
    private final InputHandler inputHandler;
    private final Camera camera = new Camera();
    private final DynamicBackground dynamicBackground = new DynamicBackground();
    private final LevelManager levelManager = new LevelManager();
    private final MissionManager missionManager = new MissionManager();
    private final EnemyFactory enemyFactory = new EnemyFactory(levelManager, missionManager);
    private final FloatingTextManager floatingTextManager = new FloatingTextManager();
    private final AtomicBoolean bankSyncInProgress = new AtomicBoolean(false);
    private final Object bankRewardLock = new Object();

    private final List<Enemy> enemies = new ArrayList<>();
    private final Map<Enemy, EnemySpawnEffect> enemySpawnEffects = new IdentityHashMap<>();
    private final Map<Enemy, EnemyDeathEffect> enemyDeathEffects = new IdentityHashMap<>();

    private final List<Bullet> bullets = new ArrayList<>();
    private final List<ExplosionEffect> explosions = new ArrayList<>();
    private final List<SoundWaveEffect> soundWaves = new ArrayList<>();
    private final List<SlashEffect> slashEffects = new ArrayList<>();
    private final List<IonExplosionEffect> ionExplosions = new ArrayList<>();

    private final List<Item> items = new ArrayList<>();
    private final ItemMagnetSystem itemMagnetSystem = new ItemMagnetSystem();
    private final ParticleManager particleManager = new ParticleManager();
    private final EnemyBurnManager enemyBurnManager = new EnemyBurnManager();
    private final CombatEffectManager combatEffectManager = new CombatEffectManager();
    private final List<Obstacle> obstacles = new ArrayList<>();
    private final List<Obstacle> readOnlyObstacles = java.util.Collections.unmodifiableList(obstacles);
    private final List<Obstacle> destroyedObstacleQueue = new ArrayList<>();
    private final List<RestHealEffect> restHealEffects = new ArrayList<>();

    private final ExecutorService databaseExecutor =
            Executors.newSingleThreadExecutor(runnable -> {
                Thread thread = new Thread(runnable, "soul-knight-database-worker");
                thread.setDaemon(true);
                return thread;
            });
    private final AtomicBoolean saveInProgress = new AtomicBoolean(false);
    private final AtomicBoolean saveLoadInProgress = new AtomicBoolean(false);
    private GameStateListener stateListener;
    private java.util.function.IntConsumer buffHotkeyListener;
    private MapManager mapManager;
    private Player player;
    private Pet currentPet;
    private WeaponType runWeaponSlot1;
    private WeaponType runWeaponSlot2;
    private int activeRunWeaponSlot = 0;
    private String currentPlayerName = "";
    private int gold = 0;
    private int gems = 0;
    private int pendingBankGold = 0;
    private int pendingBankGems = 0;
    private int score = 0;
    private int currentRoomNumber = 1;
    private Room currentRoom;
    private EndingPortal endingPortal;
    private boolean endingStoryTriggered;
    private double autoSaveTimer = 0.0;
    private double playerEnergy = 100.0;
    private PlayerSave pendingPlayerSave;
    private GameState state = GameState.INTRO;
    private double enemySpawnTimer;
    private Vector2D pendingPortalPosition;

    private boolean rewardPickerShown;
    private RewardPicker rewardPicker;
    private Room lastRewardRoom;
    private final java.util.Set<String> rewardedRoomKeys = new java.util.HashSet<>();
    private SpawnEffect playerSpawnEffect;
    private SpawnEffect petSpawnEffect;
    private PlayerDeathEffect playerDeathEffect;
    private boolean playerDeathHandled;

    public GameWorld(InputHandler inputHandler) {
        this.inputHandler = inputHandler;
    }

    // Lắng nghe sự kiện
    public void setGameStateListener(GameStateListener listener) {
        this.stateListener = listener;
        if (this.stateListener != null) {
            this.stateListener.onStateChanged(this.state);
        }
    }

    // Lắng nghe sự kiện
    public void setBuffHotkeyListener(java.util.function.IntConsumer listener) {
        this.buffHotkeyListener = listener;
    }

    // Hàm chuyển đổi State
    public void changeState(GameState newState) {
        if (newState == null || this.state == newState) return;

        GameState oldState = this.state;
        this.state = newState;

        // Chuyển sang State mới thì xóa phím bấm
        if (inputHandler != null) {
            inputHandler.clearState();
        }
        // Quản lí chuyển nhạc nền
        handleBGMStateChange(newState);

        if (stateListener != null) {
            stateListener.onStateChanged(newState);
        }
    }
    // Quản lí chuyển nhạc nền
    private void handleBGMStateChange(GameState newState) {
        SoundManager sound = SoundManager.getInstance();

        if (newState == GameState.PLAYING) {
            playGameBGM();
            return;
        }
        if (newState == GameState.PAUSED) {
            return;
        }
        if (newState == GameState.MAIN_MENU) {
            sound.stopBGM();
        }
    }

    // Cập nhật logic chính toàn bộ game
    public void update(double deltaSeconds, double viewportWidth, double viewportHeight) {
        // Paused thì return luôn
        if (state == GameState.PAUSED) {
            return;
        }
        // Buff
        if (state == GameState.PLAYING && inputHandler != null) {
            int buffIndex = inputHandler.consumeBuffHotkeyRequest();

            if (buffIndex >= 0 && buffHotkeyListener != null) {
                buffHotkeyListener.accept(buffIndex);
            }
        }

        dynamicBackground.update(deltaSeconds);

        switch (state) {
            case INTRO, MAIN_MENU -> {
                inputHandler.consumeConfirmRequest();
            }
            case PLAYING -> updatePlaying(deltaSeconds, viewportWidth, viewportHeight, false);
            case REWARD_PICK -> updateRewardPick();
            case ENDING_STORY -> {}
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

        // gọi hàm renderWorld
        if (state != GameState.INTRO && state != GameState.MAIN_MENU && mapManager != null && player != null) {
            renderWorld(graphicsContext, renderWidth, renderHeight);
        }
        if (state == GameState.REWARD_PICK && rewardPicker != null) {
            Vector2D mousePos = inputHandler != null ? inputHandler.getMousePosition() : null;
            rewardPicker.render(graphicsContext, mousePos);
        }
    }

    // update Playing state
    private void updatePlaying(double deltaSeconds, double viewportWidth, double viewportHeight, boolean allowSpawns) {
        if (player == null || mapManager == null) {
            return;
        }
        // cập nhật hiệu ứng
        if (playerSpawnEffect != null) {
            playerSpawnEffect.update(deltaSeconds);
        }
        if (petSpawnEffect != null) {
            petSpawnEffect.update(deltaSeconds);
        }
        if (playerDeathEffect != null) {
            playerDeathEffect.update(deltaSeconds);
        }

        updateEnemySpawnEffects(deltaSeconds);

        // Player còn sống thì update
        if (player.isAlive()) {
            player.update(this, deltaSeconds);
            updatePet(deltaSeconds);
            updateCurrentRoom();
        }
        if (currentRoom != null) {
            currentRoom.update(this, player, enemies, deltaSeconds);
        }
        Room.RoomState previousState = previousRoomStates.get(currentRoom);
        handleRoomBuffReward(currentRoom, previousState);
        handleBossRoomCleared(currentRoom, previousState);
        previousRoomStates.put(currentRoom, currentRoom.getState());

        for (Enemy enemy : enemies) {
            if (!enemy.isAlive() || isEnemySpawning(enemy)) {
                continue;
            }
            enemy.update(this, deltaSeconds);
            enemy.separateFromOtherEnemies(this, enemies, deltaSeconds);
        }

        resolvePlayerEnemyCollisions(deltaSeconds);
        updateBullets(deltaSeconds);
        processDestroyedObstacles();
        updateExplosions(deltaSeconds);
        updateIonExplosions(deltaSeconds);
        updateSlashEffects(deltaSeconds);
        updateSoundWaves(deltaSeconds);
        updateRestHealEffects(deltaSeconds);
        updateEnemyDeaths(deltaSeconds);
        enemyBurnManager.update(this, deltaSeconds);
        combatEffectManager.update(deltaSeconds);
        itemMagnetSystem.update(items, player, deltaSeconds);// hut item ve player
        updateItemCollection();
        updateEndingPortal(deltaSeconds);
        openRewardPickerOnMissionComplete();
        updateAutoSave(deltaSeconds);

        if (allowSpawns) {
            updateSpawnTimers(deltaSeconds);
        }

        camera.follow(player.getPosition(), viewportWidth, viewportHeight, mapManager.getWorldWidth(), mapManager.getWorldHeight());

        if (!player.isAlive()) {
            updatePlayerDeath();
            return;
        }
        // Hiệu ứng
        if (particleManager != null) {
            particleManager.update(deltaSeconds);
        }

        floatingTextManager.update(deltaSeconds);
    }

    // update pet
    private void updatePet(double deltaSeconds) {
        if (currentPet == null || player == null || player.getPosition() == null) {
            return;
        }
        currentPet.update(deltaSeconds, player.getPosition().getX(), player.getPosition().getY(), this);
    }

    private void updateBullets(double deltaSeconds) {

        for (Bullet bullet : bullets) {
            if (bullet == null || !bullet.isActive()) {
                continue;
            }

            double bulletX = bullet.getPosition().getX();
            double bulletY = bullet.getPosition().getY();

            // Kiểm tra va chạm với tường
            if (mapManager.isBulletCollidingWithWall(bulletX, bulletY, bullet.getRadius())) {
                if (bullet.isSoundWave()) {
                    spawnSoundWave(bullet.getPosition(), bullet.getDamage());
                }
                bullet.deactivate();
                continue;
            }

            // Tính phản đạn
            double prevX = bulletX;
            double prevY = bulletY;

            bullet.update(deltaSeconds);

            bulletX = bullet.getPosition().getX();
            bulletY = bullet.getPosition().getY();

            // Đạn va chạm tường sau khi di chuyển
            if (mapManager.isBulletCollidingWithWall(bulletX, bulletY, bullet.getRadius())) {
                if (bullet.canReflect()) {
                    // Kiểm tra trục phản
                    boolean flipX = mapManager.isBulletCollidingWithWall(bulletX, prevY, bullet.getRadius());
                    boolean flipY = mapManager.isBulletCollidingWithWall(prevX, bulletY, bullet.getRadius());
                    bullet.reflectOnce(new Vector2D(prevX, prevY), flipX, flipY);
                    spawnBulletExplosion(new Vector2D(bulletX, bulletY));
                    continue;
                }

                Vector2D impactPosition = bullet.getPosition().copy();
                if (bullet.explodesOnTerrain()) {
                    triggerIonProjectileExplosion(
                            impactPosition,
                            bullet.getTerrainExplosionRadius(),
                            bullet.getTerrainExplosionDamage(),
                            bullet.getOwner()
                    );
                } else {
                    if (bullet.isSoundWave()) {
                        spawnSoundWave(impactPosition, bullet.getDamage());
                    } else {
                        spawnBulletExplosion(impactPosition);
                    }
                }
                bullet.deactivate();
                continue;
            }
            // Đạn va chạm obstacle
            for (Obstacle obstacle : obstacles) {
                if (obstacle == null || obstacle.isDestroyed()) continue;
                if (!obstacle.intersectsCircle(bullet.getPosition(), bullet.getRadius())) continue;
                // đạn xuyên vật cản
                if (bullet.piercesObstacles()) {
                    if (!bullet.hasAlreadyHit(obstacle)) {
                        bullet.markHit(obstacle);
                        damageObstacle(obstacle, bullet.getDamage(), bullet.getPosition());
                    }

                    continue;
                }
                //  Đạn Ion xuyên enemy khi nổ gặp vật cản
                if (bullet.explodesOnTerrain()) {
                    Vector2D impactPosition = bullet.getPosition().copy();

                    damageObstacle(obstacle, bullet.getDamage(), impactPosition);
                    triggerIonProjectileExplosion(
                            impactPosition,
                            bullet.getTerrainExplosionRadius(),
                            bullet.getTerrainExplosionDamage(),
                            bullet.getOwner()
                    );
                    bullet.deactivate();
                    break;
                }

                damageObstacle(obstacle, bullet.getDamage(), bullet.getPosition());
                if (bullet.isSoundWave()) {
                    spawnSoundWave(bullet.getPosition(), bullet.getDamage());
                }
                bullet.deactivate();
                break;
            }
            // Nếu đạn thuộc về Player
            if (bullet.getOwner() instanceof Player) {
                for (Enemy enemy : enemies) {
                    if (enemy == null || !enemy.isAlive() || isEnemySpawning(enemy)) {
                        continue;
                    }

                    if (bullet.intersects(enemy)) {
                        // Đạn xuyên
                        if (bullet.isPiercing()) {
                            if (!bullet.hasAlreadyHit(enemy)) {
                                bullet.markHit(enemy);
                                int healthBefore = enemy.getHealth();
                                enemy.takeDamage(bullet.getDamage());
                                int realDamage = healthBefore
                                        - enemy.getHealth();

                                if (realDamage > 0 && bullet.getOwner() instanceof Player attackingPlayer) {

                                    attackingPlayer.getBuffManager().notifyDamageDealt(this, enemy, realDamage);
                                }
                                // Hiển thị log
                                floatingTextManager.spawnDamage(
                                        enemy.getPosition(),
                                        bullet.getDamage()
                                );
                                if (particleManager != null) {
                                    particleManager.spawnHitImpact(enemy.getPosition());
                                }

                            }
                            continue;
                        }

                        // Đạn thường
                        int healthBefore = enemy.getHealth();
                        enemy.takeDamage(bullet.getDamage());
                        int realDamage = healthBefore - enemy.getHealth();

                        if (realDamage > 0 && bullet.getOwner() instanceof Player attackingPlayer) {

                            attackingPlayer.getBuffManager().notifyDamageDealt(this, enemy, realDamage);
                        }
                        floatingTextManager.spawnDamage(
                                enemy.getPosition(),
                                bullet.getDamage()
                        );
                        if (particleManager != null) {
                            particleManager.spawnHitImpact(enemy.getPosition());
                        }
                        spawnBulletExplosion(bullet.getPosition());
                        if (bullet.isSoundWave()) {
                            spawnSoundWave(bullet.getPosition(), bullet.getDamage());
                        }
                        bullet.deactivate();
                        break;
                    }
                }
                // Đạn không thuộc về Player
            } else if (player != null && player.isAlive() && bullet.intersects(player)) {
                int healthBefore = player.getHealth();

                player.takeDamage(bullet.getDamage());

                int realDamage = healthBefore - player.getHealth();

                if (realDamage > 0) {
                    floatingTextManager.spawnDamage(
                            player.getPosition(),
                            realDamage
                    );
                }

                if (bullet.isSoundWave()) {
                    spawnSoundWave(bullet.getPosition(), bullet.getDamage());
                } else {
                    spawnBulletExplosion(bullet.getPosition());
                }
                bullet.deactivate();
            }
        }
        // Đạn không hoạt động
        bullets.removeIf(bullet -> bullet == null || !bullet.isActive());
    }

    private void updatePlayerDeath() {
        if (player == null || player.getPosition() == null) {
            return;
        }
        if (playerDeathEffect == null) {
            playerDeathEffect = new PlayerDeathEffect(player.getPosition(), 1.15);
            playerSpawnEffect = null;
            if (inputHandler != null) {
                inputHandler.clearState();
            }
        }
        if (!playerDeathEffect.isFinished()) {
            return;
        }
        if (playerDeathHandled) {
            return;
        }
        playerDeathHandled = true;
        inputHandler.consumeConfirmRequest();
        addScore(100);
        saveGameAsync();
        syncBankRewardsAsync();
        changeState(GameState.GAME_OVER);
    }
    
    // Hàm xử lí vật cản đã nổ
    private void processDestroyedObstacles() {
        if (destroyedObstacleQueue.isEmpty() || mapManager == null || mapManager.getRooms() == null) {
            return;
        }

        double tileSize = mapManager.getTileSize();

        for (Obstacle obstacle : destroyedObstacleQueue) {
            if (obstacle == null) {
                continue;
            }
            // Hiệu ứng
            if (particleManager != null) {
                particleManager.spawnWoodDebris(obstacle.getCenter());
            }

            if (obstacle.getPosition() != null) {
                int gridX = (int) (obstacle.getPosition().getX() / tileSize);
                int gridY = (int) (obstacle.getPosition().getY() / tileSize);
                // Đổi sang Floor
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

    // Hiệu ứng
    private void spawnBulletExplosion(Vector2D position) {
        explosions.add(new ExplosionEffect(position.copy(), 18.0, 0.3, Color.ORANGERED));
    }
    // Hiệu ứng
    private void spawnSoundWave(Vector2D position, int damage) {
        soundWaves.add(new SoundWaveEffect(position.copy(), 35.0, 0.5, damage));
    }
    // Hiệu ứng
    private void updateSoundWaves(double deltaSeconds) {
        for (SoundWaveEffect soundWave : soundWaves) {
            soundWave.update(deltaSeconds, enemies);
        }
        soundWaves.removeIf(soundWave -> !soundWave.isActive());
    }
    // Hiệu ứng
    public void triggerDeathExplosion(Vector2D center, int damage) {
        if (center == null || damage <= 0) {
            return;
        }
        final double EXPLOSION_RADIUS = 90.0;
        for (Enemy enemy : enemies) {
            if (enemy == null || !enemy.isAlive() || enemy.getPosition() == null) {
                continue;
            }
            double distance = center.distance(enemy.getPosition());
            if (distance > EXPLOSION_RADIUS + enemy.getRadius()) {
                continue;
            }
            int healthBefore = enemy.getHealth();
            enemy.takeDamage(damage);
            int realDamage = healthBefore - enemy.getHealth();
            if (realDamage <= 0) {
                continue;
            }
            floatingTextManager.spawnCustom("-" + realDamage, enemy.getPosition(), Color.ORANGERED
            );

            enemyBurnManager.applyBurn(enemy);
        }
        combatEffectManager.spawnDeathFireExplosion(center);
        SoundManager.getInstance().playSFXShort("death_explosion", 0.45);
    }
    // Hiệu ứng
    public void triggerDragonBreath() {
        if (player == null || !player.isAlive() || player.getPosition() == null || mapManager == null) {
            return;
        }
        Vector2D origin = player.getPosition().copy();
        double dirX = player.getLastMoveX();
        double dirY = player.getLastMoveY();
        double range = mapManager.getTileSize() * DRAGON_BREATH_TILES;
        List<Enemy> hitEnemies = new ArrayList<>();

        for (Enemy enemy : enemies) {
            if (enemy == null || !enemy.isAlive() || enemy.getPosition() == null || isEnemySpawning(enemy)) {
                continue;
            }

            if (isInsideDragonBreathCone(origin, dirX, dirY, range, enemy)) {
                hitEnemies.add(enemy);
            }
        }

        for (Enemy enemy : hitEnemies) {
            if (enemy == null || !enemy.isAlive()) {
                continue;
            }
            boolean alreadyBurning = enemyBurnManager.isBurning(enemy);
            int healthBefore = enemy.getHealth();
            enemy.takeDamage(DRAGON_BREATH_DAMAGE
            );
            int realDamage = healthBefore - enemy.getHealth();
            if (realDamage > 0) {
                floatingTextManager.spawnCustom("-" + realDamage, enemy.getPosition(), Color.ORANGERED);
            }

            if (alreadyBurning && enemy.isAlive()) {
                triggerDragonBreathExplosion(enemy.getPosition().copy());
            }

            if (enemy.isAlive()) {enemyBurnManager.applyBurn(enemy);
            }
        }
        combatEffectManager.spawnDragonBreath(origin, dirX, dirY, range, DRAGON_BREATH_HALF_ANGLE);
        SoundManager.getInstance().playSFXShort("dragon_breath", 0.75);
    }
    //    buff trc tan cong don chi mang
    public void triggerHolyNova() {
        if (player == null || !player.isAlive() || player.getPosition() == null || mapManager == null) {
            return;
        }
        Vector2D center = player.getPosition().copy();

        double radius = mapManager.getTileSize() * HOLY_NOVA_RADIUS_TILES;

        for (Enemy enemy : enemies) {
            if (enemy == null || !enemy.isAlive() || enemy.getPosition() == null || isEnemySpawning(enemy)) {
                continue;
            }
            double distance = center.distance(enemy.getPosition());
            if (distance > radius + enemy.getRadius()) {
                continue;
            }
            int healthBefore = enemy.getHealth();
            enemy.takeDamage(HOLY_NOVA_DAMAGE);
            int realDamage = healthBefore - enemy.getHealth();
            if (realDamage <= 0) {
                continue;
            }
            floatingTextManager.spawnCustom("-" + realDamage, enemy.getPosition(), Color.LIGHTYELLOW);
        }
        // Xóa đạn enemy
        destroyEnemyBulletsInHolyNova(center, radius);
        // Hồi hp cho player
        int healAmount = (int) Math.ceil(player.getMaxHealth() * 0.5);
        player.heal(healAmount);
        floatingTextManager.spawnCustom("+" + healAmount, player.getPosition(), Color.LIGHTGREEN);
        combatEffectManager.spawnHolyNova(center, radius);
        SoundManager.getInstance().playSFXShort("holy_nova", 0.9);
    }

    private void destroyEnemyBulletsInHolyNova(Vector2D center, double radius) {
        for (Bullet bullet : bullets) {
            if (bullet == null || !bullet.isActive() || bullet.getPosition() == null) {
                continue;
            }
            /*
             * Dan cua Player khong bi Holy Nova pha.
             */
            if (bullet.getOwner() instanceof Player) {
                continue;
            }
            double distance = center.distance(bullet.getPosition());
            if (distance <= radius) {
                bullet.deactivate();
                /*
                 * Neu muon khi xoa dan co particle,
                 * co the them sau.
                 */
            }
        }
    }

    //Kiem tra quai nam trong pham vi hoi tho
    private boolean isInsideDragonBreathCone(Vector2D origin, double dirX, double dirY, double range, Enemy enemy) {
        if (origin == null || enemy == null || enemy.getPosition() == null) {
            return false;
        }
        double dx = enemy.getPosition().getX() - origin.getX();

        double dy = enemy.getPosition().getY() - origin.getY();

        double distance = Math.hypot(dx, dy);

        if (distance > range + enemy.getRadius()) {
            return false;
        }
        if (distance <= 0.001) {
            return true;
        }
        double targetDirX = dx / distance;
        double targetDirY = dy / distance;
        double dot = dirX * targetDirX + dirY * targetDirY;
        return dot >= Math.cos(DRAGON_BREATH_HALF_ANGLE);
    }
    // Hiệu ứng
    private void triggerDragonBreathExplosion(Vector2D center) {
        if (center == null) {
            return;
        }

        List<Enemy> snapshot = new ArrayList<>(enemies);
        for (Enemy enemy : snapshot) {
            if (enemy == null || !enemy.isAlive() || enemy.getPosition() == null) {
                continue;
            }
            double distance = center.distance(enemy.getPosition());
            if (distance > DRAGON_EXPLOSION_RADIUS + enemy.getRadius()) {
                continue;
            }
            int healthBefore = enemy.getHealth();
            enemy.takeDamage(DRAGON_EXPLOSION_DAMAGE);
            int realDamage = healthBefore - enemy.getHealth();

            if (realDamage <= 0) {
                continue;
            }
            floatingTextManager.spawnCustom("-" + realDamage, enemy.getPosition(), Color.RED);
        }
        combatEffectManager.spawnDragonExplosion(center);
        SoundManager.getInstance().playSFXShort("dragon_explosion", 0.5);
    }
    // Hiệu ứng
    public void spawnChainLightning(Vector2D source, List<Enemy> targets) {
        if (source == null || targets == null || targets.isEmpty()) {
            return;
        }

        List<Vector2D> points = new ArrayList<>();

        points.add(source.copy());

        for (Enemy enemy : targets) {
            if (enemy == null || enemy.getPosition() == null) {
                continue;
            }

            points.add(enemy.getPosition().copy());
        }

        if (points.size() < 2) {
            return;
        }

        combatEffectManager.spawnPurpleChainLightning(points);
    }

    // Hiệu ứng
    private void triggerIonProjectileExplosion(Vector2D center, double radius, int damage, Entity owner) {
        if (center == null || radius <= 0.0) return;

        // Khong dung ExplosionEffect chung nua.
        ionExplosions.add(new IonExplosionEffect(center, radius));

        if (owner instanceof Player attackingPlayer) {
            for (Enemy enemy : enemies) {
                if (enemy == null || !enemy.isAlive() || enemy.getPosition() == null || isEnemySpawning(enemy)) continue;

                double distance = center.distance(enemy.getPosition());
                if (distance > radius + enemy.getRadius()) continue;

                int healthBefore = enemy.getHealth();

                enemy.takeDamage(damage);

                int realDamage = healthBefore - enemy.getHealth();
                if (realDamage <= 0) continue;

                floatingTextManager.spawnCustom("-" + realDamage, enemy.getPosition(), Color.web("#76FFE9"));
                attackingPlayer.getBuffManager().notifyDamageDealt(this, enemy, realDamage);
            }
        }

        SoundManager.getInstance().playSFX("ion_explosion");
    }

    private void updateExplosions(double deltaSeconds) {
        for (ExplosionEffect explosion : explosions) {
            explosion.update(deltaSeconds);
        }
        explosions.removeIf(explosion -> !explosion.isActive());
    }

    private void updateIonExplosions(double deltaSeconds) {
        for (IonExplosionEffect explosion : ionExplosions) {
            if (explosion != null) explosion.update(deltaSeconds);
        }
        ionExplosions.removeIf(explosion -> explosion == null || !explosion.isActive());
    }

    private void updateSlashEffects(double deltaSeconds) {
        for (SlashEffect slash : slashEffects) {
            slash.update(deltaSeconds);
        }
        slashEffects.removeIf(slash -> !slash.isActive());
    }

    public void spawnMeleeSlash(Entity owner, double aimAngle) {
        slashEffects.add(new SlashEffect(owner, aimAngle));
    }

    // Update nhặt vật phẩm
    private void updateItemCollection() {
        if (player == null || player.getPosition() == null) return;

        for (Item item : items) {
            if (item == null || item.isCollected()) continue;
            if (!item.intersects(player.getPosition(), player.getRadius())) continue;
            // Vật phẩm là Gold
            if (item instanceof GoldItem goldItem) {
                int amount = goldItem.getAmount();
                addGold(amount);
                floatingTextManager.spawnGold(player.getPosition(), amount);
                particleManager.spawnCoinBurst(player.getPosition(), amount);
                // Vật phẩm là Gem
            } else if (item instanceof GemItem gemItem) {
                int amount = gemItem.getAmount();
                addGems(amount);
                floatingTextManager.spawnCustom("+" + amount, player.getPosition(), Color.MEDIUMPURPLE);
                // Vật phẩm là buff
            } else if (item instanceof BuffItem buffItem) {
                BuffType type = buffItem.getBuffType();
                BuffInventoryManager.getInstance().add(type, 1);
                floatingTextManager.spawnCustom("+1 " + type.getDisplayName(), player.getPosition(), Color.LIMEGREEN);
                saveCollectedBuff(type);
                // Vật phẩm là Energy
            } else if (item instanceof EnergyCrystal energyCrystal){
                double amount = energyCrystal.getAmount();
                double before = player.getMana();
                player.restoreMana(amount);
                double restored = player.getMana() - before;
                if (restored > 0) {
                    floatingTextManager.spawnCustom("+" + (int) restored + " MANA",
                            player.getPosition(), Color.AQUA);
                }
            }
                item.collect();
        }
        items.removeIf(Item::isCollected);
    }

    // Đếm thời gian sinh các đợt quái
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

            spawnMixedWave();

    }

    private void spawnMixedWave() {

        if (currentRoom == null) {
            return;
        }

        List<Vector2D> spawnPoints = currentRoom.getRandomSpawnPoints(4);

        if (spawnPoints == null || spawnPoints.isEmpty()) {
            return;
        }

        for (Enemy enemy : enemyFactory.createInitialEnemies(
                random,
                player.getPosition(),
                spawnPoints
        )) {
            addEnemyWithSpawnEffect(enemy, 0.0);
        }
    }
    // Hàm render quan trọng nhất
    private void renderWorld(GraphicsContext graphicsContext, double renderWidth, double renderHeight) {

        // Tầng 1 vẽ lớp dưới cùng
        dynamicBackground.render(graphicsContext, camera, renderWidth, renderHeight);
        mapManager.renderFloor(graphicsContext, camera, renderWidth, renderHeight);
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

        // Tầng 2 vẽ Y-Sorting
        class SortableObject {
            final double depthY;
            final Runnable renderAction;
            SortableObject(double depthY, Runnable renderAction) {
                this.depthY = depthY;
                this.renderAction = renderAction;
            }
        }
        // Danh sách chứa các entity cần Y-Sorting
        List<SortableObject> renderList = new ArrayList<>();
        double tileSize = mapManager.getTileSize();
        // Load Walls
        for (Tile wall : mapManager.getWallTiles()) {
            if (wall == null || !isVisibleInCamera(wall.getX(), wall.getY(), tileSize, tileSize, renderWidth, renderHeight)) {
                continue;
            }
            double wallBottomY = wall.getY() + tileSize; // day o tuong
            renderList.add(new SortableObject(wallBottomY, () -> {
                double screenX = camera.worldToScreenX(wall.getX());
                double screenY = camera.worldToScreenY(wall.getY());
                double zoom = camera.getZoom();
                graphicsContext.drawImage(wall.getTexture(), screenX, screenY, tileSize * zoom, tileSize * zoom);
            }));
        }
        // Load Player
        if (player != null && player.getPosition() != null) {
            double playerFootY = player.getPosition().getY() + 10.0;
            renderList.add(new SortableObject(playerFootY, () -> {
                if (playerDeathEffect != null) {
                    double alpha = playerDeathEffect.getEntityAlpha();

                    if (alpha > 0.0) {
                        double zoom = camera.getZoom();
                        double screenX = camera.worldToScreenX(player.getPosition().getX());
                        double screenY = camera.worldToScreenY(player.getPosition().getY());

                        double scaleX = playerDeathEffect.getEntityScaleX();
                        double scaleY = playerDeathEffect.getEntityScaleY();
                        graphicsContext.save();
                        graphicsContext.setGlobalAlpha(alpha);
                        graphicsContext.translate(playerDeathEffect.getShakeX() * zoom, playerDeathEffect.getShakeY() * zoom);

                        graphicsContext.translate(screenX, screenY);
                        graphicsContext.scale(scaleX, scaleY);
                        graphicsContext.translate(-screenX, -screenY);

                        ShadowRenderer.render(graphicsContext, camera, player.getPosition(), 22.0, 6.0);
                        player.render(graphicsContext, camera);
                        graphicsContext.restore();
                    }

                    playerDeathEffect.renderSoulCopy(graphicsContext, camera, player
                    );
                    playerDeathEffect.render(graphicsContext, camera);
                    return;
                }

                boolean isSpawning = playerSpawnEffect != null && playerSpawnEffect.isSpawning();

                if (isSpawning) {
                    double alpha = playerSpawnEffect.getEntityAlpha();
                    if (alpha > 0.0) {
                        graphicsContext.save();
                        graphicsContext.setGlobalAlpha(alpha);

                        ShadowRenderer.render(graphicsContext, camera, player.getPosition(), 22.0, 6.0);

                        graphicsContext.restore();
                    }

                    graphicsContext.save();
                    graphicsContext.setGlobalAlpha(alpha);
                    player.render(graphicsContext, camera);
                    graphicsContext.restore();

                    playerSpawnEffect.render(graphicsContext, camera);
                } else {
                    ShadowRenderer.render(graphicsContext, camera, player.getPosition(), 22.0, 6.0);
                    player.render(graphicsContext, camera);
                }
            }));
        }

        // load pet
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

        // load Ending Portal
        if (endingPortal != null && endingPortal.getPosition() != null) {
            Vector2D portalPosition = endingPortal.getPosition();

            // Position cua portal la diem chan portal tren mat dat.
            double portalDepthY = portalPosition.getY();

            renderList.add(new SortableObject(
                    portalDepthY,
                    () -> endingPortal.render(graphicsContext, camera)
            ));
        }

        // loadEnemy
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
                EnemyDeathEffect deathEffect = enemyDeathEffects.get(enemy);
                if (deathEffect != null) {
                    double alpha = deathEffect.getEntityAlpha();

                    if (alpha > 0.0) {
                        graphicsContext.save();
                        graphicsContext.setGlobalAlpha(alpha);
                        double zoom = camera.getZoom();
                        graphicsContext.translate(deathEffect.getShakeX() * zoom, deathEffect.getShakeY() * zoom);

                        ShadowRenderer.render(graphicsContext, camera, enemy.getPosition(), 22.0, 6.0, 10.0, false);

                        enemy.render(graphicsContext, camera);
                        graphicsContext.restore();
                    }

                    deathEffect.render(graphicsContext, camera);
                    return;
                }

                EnemySpawnEffect spawnEffect = enemySpawnEffects.get(enemy);

                if (spawnEffect != null && spawnEffect.isSpawning()) {
                    double alpha = spawnEffect.getEntityAlpha();

                    if (alpha > 0.0) {
                        graphicsContext.save();
                        graphicsContext.setGlobalAlpha(alpha);

                        ShadowRenderer.render(graphicsContext, camera, enemy.getPosition(), 22.0, 6.0, 10.0, false);

                        enemy.render(graphicsContext, camera);
                        graphicsContext.restore();
                    }

                    spawnEffect.render(graphicsContext, camera);
                    return;
                }

                ShadowRenderer.render(graphicsContext, camera, enemy.getPosition(), 22.0, 6.0, 10.0, false);

                enemy.render(graphicsContext, camera);
            }));


        }

        // load Obstacles
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
        // Load Door
        for (Room room : mapManager.getRooms()) {
            for (BoundingBox door : room.getDoors()) {
                if (door == null) continue;
                if (!isVisibleInCamera(door.getMinX(), door.getMinY(), door.getWidth(), door.getHeight(),
                        renderWidth, renderHeight)) {
                    continue;
                }

                double doorBottomY = door.getMaxY();

                renderList.add(new SortableObject(doorBottomY, () -> {
                    room.renderSingleDoor(graphicsContext, camera, door, tileSize);
                }));
            }
        }
        // Render cac object rieng cua Rest Room.
        if (currentRoom != null && currentRoom.getType() == Room.RoomType.REST) {
            RestRoomController restController = currentRoom.getRestRoomController();

            if (restController != null && restController.getShrine() != null) {
                RestShrine shrine = restController.getShrine();

                renderList.add(new SortableObject(
                        shrine.getPosition().getY(),
                        () -> shrine.render(graphicsContext, camera)
                ));
            }
        }
        // Y-Sorting
        renderList.sort((a, b) -> Double.compare(a.depthY, b.depthY));
        for (SortableObject obj : renderList) {
            obj.renderAction.run();
        }

        for (RestHealEffect effect : restHealEffects) {
            if (effect != null) {
                effect.render(graphicsContext, camera);
            }
        }

        // Tầng 3 render hiệu ứng weapon
        for (Item item : items) item.render(graphicsContext, camera);
        for (Bullet bullet : bullets) {
            if (bullet == null || bullet.getPosition() == null || !bullet.isActive()) {
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
        for (SoundWaveEffect soundWave : soundWaves) soundWave.render(graphicsContext, camera);
        for (IonExplosionEffect explosion : ionExplosions) {
            if (explosion != null) explosion.render(graphicsContext, camera);
        }

        combatEffectManager.render(graphicsContext, camera);

        if (particleManager != null) {
            particleManager.render(graphicsContext, camera);
        }
        floatingTextManager.render(graphicsContext, camera);
    }

    private void startNewRun() {
        endingPortal = null;
        endingStoryTriggered = false;

        levelManager.startNewRun();

        if (pendingPlayerSave == null) {
            currentRoomNumber = 1;
        }

        rewardedRoomKeys.clear();

        initializeRunWeaponLoadout();

        loadCurrentLevel(true);

        playGameBGM();
    }

    private void loadCurrentLevel(boolean freshRun) {
        String mapPath = "/maps/primeMap_1.json";
        this.mapManager = new MapManager(mapPath);
        this.mapManager.closeExitPortal();
        this.pendingPortalPosition = null;
        loadObstaclesFromCurrentMap();

        Vector2D spawnPoint = mapManager.getSpawnPoint();
        if (this.player == null || freshRun) {
            this.player = new Player(spawnPoint);
        } else {
            this.player.getPosition().set(spawnPoint);
        }
        this.player.setDragonBreathAction(this::triggerDragonBreath);
        this.player.setHolyNovaAction(this::triggerHolyNova);
        this.playerDeathEffect = null;
        this.playerDeathHandled = false;

        applyPendingPlayerSave();

        restorePlayerRoomPosition();

        createSelectedPet();

        this.playerSpawnEffect = new SpawnEffect(spawnPoint, 0.4, 0.7);
        if (currentPet != null) {

            Vector2D petSpawnPos = new Vector2D(spawnPoint.getX() + 25, spawnPoint.getY() + 10);
            currentPet.getPosition().set(petSpawnPos);
            this.petSpawnEffect = new SpawnEffect(petSpawnPos, 0.55, 0.7);
        }

        if (freshRun) {

            activeRunWeaponSlot = 0;

            equipActiveRunWeapon();
        }

        this.enemies.clear();
        this.enemySpawnEffects.clear();
        this.enemyDeathEffects.clear();
        this.bullets.clear();
        this.explosions.clear();
        this.ionExplosions.clear();
        this.slashEffects.clear();
        this.items.clear();
        this.combatEffectManager.clear();
        this.enemyBurnManager.clear();
        this.destroyedObstacleQueue.clear();
        this.restHealEffects.clear();
        this.enemySpawnTimer = 0.0;
        floatingTextManager.clear();

        if (this.missionManager != null && this.levelManager != null) {
            this.missionManager.setMission(this.levelManager.createMissionForCurrentLevel());
        }

        this.rewardPickerShown = false;
        this.rewardPicker = null;
        this.lastRewardRoom = null;

        if (this.levelManager != null
                && this.levelManager.getCurrentLevel().bossLevel()
                && this.mapManager.getBossSpawnPoint() != null) {
            this.enemies.add(this.enemyFactory.createGrandKnight(this.mapManager.getBossSpawnPoint()));
        }
    }

    private void createSelectedPet() {
        if (player == null) {
            currentPet = null;
            return;
        }

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

            room.loadObstaclesFromTiles(currentTiles);
        }

        rebuildObstacleCache();
    }

    private void openRewardPickerOnMissionComplete() {
        if (rewardPickerShown) {
            return;
        }
        if (player == null || player.getPosition() == null || currentRoom == null) {
            return;
        }

        // Chỉ mở hộp khi phòng vừa được dọn sạch (không phải START room)
        if (currentRoom.getState() != Room.RoomState.CLEARED) {
            return;
        }
        if (currentRoom.getType() == Room.RoomType.START || currentRoom.getType() == Room.RoomType.REST
        || currentRoom.getType() == Room.RoomType.BOSS) {
            return;
        }
        if (!currentRoom.hasSpawnedEnemies()) {
            return;
        }
        String rewardKey = levelManager.getCurrentLevel().number() + ":" + currentRoomNumber;
        if (rewardedRoomKeys.contains(rewardKey)) {
            return;
        }

        Weapon rewardWeapon = getValidRewardWeapon();
        WeaponType rewardType = findWeaponType(rewardWeapon);
        if (rewardType != null && (rewardType == runWeaponSlot1 || rewardType == runWeaponSlot2)) {
            rewardWeapon = null;
        }
        if (rewardWeapon == null) {
            rewardPickerShown = true;
            rewardedRoomKeys.add(rewardKey);
            lastRewardRoom = currentRoom;
            return;
        }

        int goldReward = 30 + random.nextInt(51);

        rewardPicker = new RewardPicker(rewardWeapon, goldReward);
        rewardPickerShown = true;
        rewardedRoomKeys.add(rewardKey);
        lastRewardRoom = currentRoom;

        changeState(GameState.REWARD_PICK);

        // Phát âm thanh "portal" ngắn (0.5 giây) thay vì toàn bộ file
        SoundManager.getInstance().playSFXShort("Portal", 0.5);

        debug("Mo bang chon phan thuong: vang x" + goldReward + " hoac " + rewardWeapon.getName());
    }

    private void updateRewardPick() {
        if (rewardPicker == null) {
            changeState(GameState.PLAYING);
            return;
        }

        if (!inputHandler.consumeConfirmRequest()) return;

        Vector2D clickPos = inputHandler.getMousePosition();
        String choice = rewardPicker.handleClick(clickPos);

        if (choice == null) return;

        // Chon weapon moi chi chuyen sang buoc chon slot, chua dong RewardPicker.
        if ("weapon".equals(choice)) {
            rewardPicker.showWeaponSlotSelection(runWeaponSlot1, runWeaponSlot2);
            SoundManager.getInstance().playSFX("button");
            return;
        }

        if ("slot1".equals(choice)) {
            if (replaceRunWeaponSlot(1, rewardPicker.getWeapon())) {
                finishRewardPick();
            }
            return;
        }

        if ("slot2".equals(choice)) {
            if (replaceRunWeaponSlot(2, rewardPicker.getWeapon())) {
                finishRewardPick();
            }
            return;
        }

        if ("gold".equals(choice)) {
            applyGoldReward();
            finishRewardPick();
        }
    }
    private void finishRewardPick() {
        rewardPicker = null;
        rewardPickerShown = false;
        changeState(GameState.PLAYING);
    }
    private void applyGoldReward() {
        if (rewardPicker == null) return;

        int amount = rewardPicker.getGoldAmount();

        addGold(amount);
        addScore(amount * 2);

        if (player != null && player.getPosition() != null) {
            floatingTextManager.spawnGold(player.getPosition(), amount);
            particleManager.spawnCoinBurst(player.getPosition(), amount);
        }

        SoundManager.getInstance().playSFX("button");
        debug("Nguoi choi chon vang: +" + amount);
    }
    // Weapon reward chi thay loadout cua run hien tai, khong thay loadout trong Shop.
    private boolean replaceRunWeaponSlot(int slot, Weapon rewardWeapon) {
        if (rewardWeapon == null || player == null) return false;
        if (slot != 1 && slot != 2) return false;

        WeaponType rewardType = findWeaponType(rewardWeapon);

        if (rewardType == null) {
            System.err.println("Khong tim thay WeaponType cho reward: " + rewardWeapon.getName());
            return false;
        }

        // Khong cho cung weapon xuat hien 2 lan trong run loadout.
        if (isWeaponAlreadyInRunLoadout(rewardType)) {
            showDuplicateWeaponMessage(rewardType);
            return false;
        }

        if (slot == 1) {
            runWeaponSlot1 = rewardType;
            activeRunWeaponSlot = 0;
        } else {
            runWeaponSlot2 = rewardType;
            activeRunWeaponSlot = 1;
        }

        // Player cam ngay weapon vua thay vao slot.
        equipActiveRunWeapon();

        if (player.getPosition() != null) {
            floatingTextManager.spawnCustom(rewardType.getDisplayName() + " -> SLOT " + slot, player.getPosition(), Color.GOLD);
        }

        SoundManager.getInstance().playSFX("switch");
        debug("Weapon reward -> Slot " + slot + ": " + rewardType);

        return true;
    }

    private boolean isWeaponAlreadyInRunLoadout(WeaponType type) {
        if (type == null) return false;
        return type == runWeaponSlot1 || type == runWeaponSlot2;
    }

    private void showDuplicateWeaponMessage(WeaponType weapon) {
        if (weapon == null || player == null || player.getPosition() == null) return;

        floatingTextManager.spawnCustom(weapon.getDisplayName() + " ALREADY EQUIPPED", player.getPosition(), Color.ORANGE);
        SoundManager.getInstance().playSFX("button");
    }
    private WeaponType findWeaponType(Weapon weapon) {
        if (weapon == null || weapon.getName() == null) return null;

        String weaponName = normalizeWeaponName(weapon.getName());

        for (WeaponType type : WeaponType.values()) {
            if (normalizeWeaponName(type.getDisplayName()).equals(weaponName)) return type;
            if (normalizeWeaponName(type.name()).equals(weaponName)) return type;
        }

        return null;
    }

    private String normalizeWeaponName(String value) {
        if (value == null) return "";
        return value.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
    }
    private Weapon getValidRewardWeapon() {
        for (int attempt = 0; attempt < 10; attempt++) {
            Weapon weapon = levelManager.getRewardWeaponForCurrentLevel();
            WeaponType type = findWeaponType(weapon);

            if (type == null) continue;

            // Reward phai khac ca 2 weapon dang co.
            if (type != runWeaponSlot1 && type != runWeaponSlot2) {
                return weapon;
            }
        }

        return null;
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

        if (levelManager.advanceLevel()) {

            /*
             * loadCurrentLevel(false):
             * Player cu duoc giu lai.
             * Weapon dang cam cung duoc giu.
             */
            loadCurrentLevel(false);

            changeState(GameState.PLAYING);

            saveGameAsync();

        } else {

            saveGameAsync();

            syncBankRewardsAsync();

            changeState(
                    GameState.GAME_VICTORY
            );
        }
    }

    public GameState getState() {
        return state;
    }

    public InputHandler getInputHandler() {
//        goi trong player de dieu khien nhan vat tu ban phim ,ngam ban tu chuot
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

    public List<Item> getItems() {
        return items;
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

    public LevelManager getLevelManager() {
        return levelManager;
    }

    public MissionManager getMissionManager() {
        return missionManager;
    }

    public String getCurrentPlayerName() {
        return currentPlayerName;
    }

    public void setCurrentPlayerName(
            String currentPlayerName
    ) {
        if (currentPlayerName == null
                || currentPlayerName.isBlank()) {

            this.currentPlayerName = "";
            return;
        }

        this.currentPlayerName =
                currentPlayerName.trim();
    }

    public int getGold() {
        return gold;
    }

    public void setGold(int gold) {
        this.gold = Math.max(0, gold);
    }

    public void addGold(int amount) {
        if (amount <= 0) return;

        gold += amount;

        synchronized (bankRewardLock) {
            pendingBankGold += amount;
        }
    }
    public int getGems() {
        return gems;
    }

    public void setGems(int gems) {
        this.gems = Math.max(0, gems);
    }

    public void addGems(int amount) {
        if (amount <= 0) return;

        gems += amount;

        synchronized (bankRewardLock) {
            pendingBankGems += amount;
        }
    }
    public List<Obstacle> getObstacles() {
        return readOnlyObstacles;
    }

    public FloatingTextManager getFloatingTextManager() {
        return floatingTextManager;
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
    public void switchPlayerWeapon() {

        if (player == null ||
                !player.isAlive()) {

            return;
        }

        /*
         * Khong co weapon nao.
         */
        if (runWeaponSlot1 == null &&
                runWeaponSlot2 == null) {

            return;
        }

        /*
         * Chi co Slot 1.
         */
        if (runWeaponSlot2 == null) {
            activeRunWeaponSlot = 0;
            equipActiveRunWeapon();
            return;
        }

        /*
         * Chi co Slot 2.
         */
        if (runWeaponSlot1 == null) {
            activeRunWeaponSlot = 1;
            equipActiveRunWeapon();
            return;
        }

        /*
         * Doi Slot 1 <-> Slot 2.
         */
        activeRunWeaponSlot = activeRunWeaponSlot == 0 ? 1 : 0;
        equipActiveRunWeapon();
        SoundManager.getInstance().playSFX("switch");
        debug("Switch weapon -> Slot " + (activeRunWeaponSlot + 1) + ": " + getActiveRunWeaponType());
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

    public int spawnEnemiesInRoom(com.soulknight.map.Room room, int waveNumber) {
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
        for (int i = 0; i < roomSpawnPoints.size(); i++) {
            Vector2D spawnPt = roomSpawnPoints.get(i);
            Enemy enemy = enemyFactory.createEnemyByWave(random, spawnPt, waveNumber);
            addEnemyWithSpawnEffect(enemy, i * 0.08);
        }
        return roomSpawnPoints.size();
    }

    private void addEnemyWithSpawnEffect(Enemy enemy, double delay) {
        if (enemy == null || enemy.getPosition() == null) {
            return;
        }

        enemies.add(enemy);

        enemySpawnEffects.put(enemy, new EnemySpawnEffect(enemy.getPosition(), delay, 0.9));
    }

    private void updateEnemySpawnEffects(double deltaSeconds) {
        if (enemySpawnEffects.isEmpty()) {
            return;
        }

        for (EnemySpawnEffect effect : enemySpawnEffects.values()) {
            effect.update(deltaSeconds);
        }

        enemySpawnEffects.entrySet().removeIf(entry -> entry.getValue().isFinished());
    }

    private boolean isEnemySpawning(Enemy enemy) {
        EnemySpawnEffect effect = enemySpawnEffects.get(enemy);

        return effect != null && effect.blocksEnemyLogic();
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
            if (!enemy.isAlive() || isEnemySpawning(enemy)) continue;

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

                // Đẩy ngược Player về phía sau 1 nửa khoảng cách lún để tạo phản lực mượt m�
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
            if (!enemy.isAlive() || isEnemySpawning(enemy)) {
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
            int healthBefore = enemy.getHealth();

            enemy.takeDamage(damage);

            int realDamage = healthBefore - enemy.getHealth();

            if (realDamage > 0 && player != null) {

                player.getBuffManager().notifyDamageDealt(this, enemy, realDamage);
            }
//            chem enemy sing ra tia lua
            if (particleManager != null) {
                particleManager.spawnHitImpact(enemy.getPosition());
            }
            floatingTextManager.spawnDamage(enemy.getPosition(), damage);
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



    //    data base
    public void saveGameAsync() {
        if (currentPlayerName == null || currentPlayerName.isBlank()) {
            return;
        }
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
        loadGameAsync(playerName, null);
    }

    public void loadGameAsync(String playerName, java.util.function.Consumer<Boolean> callback) {
        if (playerName == null || playerName.isBlank()) {
            System.err.println("Khong the load save vi username trong.");
            runLoadCallback(callback, false);
            return;
        }

        if (!saveLoadInProgress.compareAndSet(false, true)) {
            System.err.println("Dang co mot tien trinh load save khac.");
            runLoadCallback(callback, false);
            return;
        }

        String safePlayerName = playerName.trim();
        pendingPlayerSave = null;

        databaseExecutor.submit(() -> {
            boolean success = false;

            try {
                PlayerSaveDAO dao = new PlayerSaveDAO();
                java.util.Optional<PlayerSave> saveOptional = dao.findByName(safePlayerName);

                if (saveOptional.isPresent()) {
                    pendingPlayerSave = saveOptional.get();
                    success = true;

                    System.out.println("Da tai save cua " + safePlayerName + ": room=" + pendingPlayerSave.getCurrentRoom());
                } else {
                    System.out.println("Khong tim thay save cua " + safePlayerName + ".");
                }

            } catch (RuntimeException exception) {
                System.err.println("Cloud Load loi: " + exception.getMessage());
                exception.printStackTrace();

            } finally {
                saveLoadInProgress.set(false);
                runLoadCallback(callback, success);
            }
        });
    }

    private void runLoadCallback(java.util.function.Consumer<Boolean> callback, boolean success) {
        if (callback == null) return;

        javafx.application.Platform.runLater(() -> callback.accept(success));
    }

    public void shutdown() {
        syncBankRewardsNow();
        databaseExecutor.shutdown();
    }

    private void syncBankRewardsNow() {
        int userId = UserSession.getCurrentUserId();
        if (userId <= 0) return;

        final int goldToDeposit;
        final int gemsToDeposit;

        synchronized (bankRewardLock) {
            goldToDeposit = pendingBankGold;
            gemsToDeposit = pendingBankGems;
        }

        if (goldToDeposit <= 0 && gemsToDeposit <= 0) {
            return;
        }

        try {
            ShopDAO shopDAO = new ShopDAO();
            if (shopDAO.depositRunRewards(userId, goldToDeposit, gemsToDeposit)) {
                synchronized (bankRewardLock) {
                    pendingBankGold = Math.max(0, pendingBankGold - goldToDeposit);
                    pendingBankGems = Math.max(0, pendingBankGems - gemsToDeposit);
                }
            }

        } catch (RuntimeException exception) {
            System.err.println("Khong the chot tien khi tat game: " + exception.getMessage());
        }
    }

    public void saveBeforeReturnToMenu() {
        saveGameAsync();
        syncBankRewardsAsync();
    }

    private void applyPendingPlayerSave() {
        if (pendingPlayerSave == null || player == null) {
            return;
        }
        PlayerSave saveToApply = pendingPlayerSave;
        PlayerSaveMapper.applyToWorld(this, saveToApply);
        synchronized (bankRewardLock) {
            pendingBankGold = 0;
            pendingBankGems = 0;
        }

        pendingPlayerSave = null;
        System.out.println("Đã áp dụng save vào Player: player=" + saveToApply.getPlayerName() + ", room=" + currentRoomNumber);
    }

    //    luu buff khu nhat duoc
    private void saveCollectedBuff(BuffType type) {
        int userId = UserSession.getCurrentUserId();
        if (userId <= 0) {
            return;
        }
        databaseExecutor.submit(() -> {
            try {
                ShopDAO dao = new ShopDAO();
                dao.grantBuff(userId, type.name(), 1);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    //    dong bo vang , gem
    public void syncBankRewardsAsync() {
        int userId = UserSession.getCurrentUserId();
        if (userId <= 0) {
            return;
        }

        if (!bankSyncInProgress.compareAndSet(false, true)) {
            return;
        }

        final int goldToDeposit;
        final int gemsToDeposit;
        synchronized (bankRewardLock) {
            goldToDeposit = pendingBankGold;
            gemsToDeposit = pendingBankGems;
            pendingBankGold = 0;
            pendingBankGems = 0;
        }

        if (goldToDeposit <= 0 && gemsToDeposit <= 0) {
            bankSyncInProgress.set(false);
            return;
        }

        databaseExecutor.submit(() -> {
            boolean success = false;

            try {
                ShopDAO shopDAO = new ShopDAO();
                success = shopDAO.depositRunRewards(userId, goldToDeposit, gemsToDeposit);

                if (success) {
                    debug("Da cong vao tai khoan: +" + goldToDeposit + " gold, +" + gemsToDeposit + " gem.");
                }

            } catch (RuntimeException exception) {
                System.err.println("Khong the dong bo tien tich luy: " + exception.getMessage()
                );

            } finally {
                if (!success) {
                    // Loi database thi tra lai de lan sau thu lai
                    synchronized (bankRewardLock) {
                        pendingBankGold += goldToDeposit;
                        pendingBankGems += gemsToDeposit;
                    }
                }

                bankSyncInProgress.set(false);

                // Neu luc dang save nguoi choi nhat them tien, dong bo tiep
                boolean hasMoreRewards;
                synchronized (bankRewardLock) {
                    hasMoreRewards = pendingBankGold > 0 || pendingBankGems > 0;
                }

                if (hasMoreRewards) {
                    javafx.application.Platform.runLater(this::syncBankRewardsAsync);
                }
            }
        });
    }

    public boolean saveGameNow() {
        if (player == null || currentPlayerName.isBlank()) {
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

    private void updateEnemyDeaths(double deltaSeconds) {
        for (int i = enemies.size() - 1; i >= 0; i--) {
            Enemy enemy = enemies.get(i);
            if (enemy == null || enemy.isAlive()) {
                continue;
            }
            EnemyDeathEffect deathEffect = enemyDeathEffects.get(enemy);

            if (deathEffect == null) {

                enemySpawnEffects.remove(enemy);
                // Bao cho cac buff biet Enemy nay vua chet.
                if (player != null && player.getBuffManager() != null) {
                    player.getBuffManager().notifyEnemyKilled(this, enemy);
                }
                deathEffect = new EnemyDeathEffect(enemy.getPosition(), enemy.getRadius(), 0.5);
                enemyDeathEffects.put(enemy, deathEffect);
            }
            deathEffect.update(deltaSeconds);

            if (!deathEffect.isFinished()) {
                continue;
            }
            giveEnemyReward(enemy);
            enemyDeathEffects.remove(enemy);
            enemies.remove(i);
        }

    }

    private void giveEnemyReward(Enemy enemy) {
        if (enemy == null || enemy.getPosition() == null) {
            return;
        }
        int scoreReward = 100;
        // Gold: luon roi
        int goldReward = random.nextInt(6) + 5;
        // Gem: hiem
        double gemDropChance = 0.15;
        // Energy: kha thuong xuyen vi weapon can energy de hoat dong
        double energyDropChance = 0.35;
        addScore(scoreReward);
        floatingTextManager.spawnCustom("+" + scoreReward + " SCORE", enemy.getPosition(), Color.LIGHTYELLOW);
        Vector2D enemyPosition = enemy.getPosition().copy();
        Vector2D goldPosition = enemyPosition.copy().add(-8.0, 0.0);
        items.add(new GoldItem(goldPosition, goldReward, null));
        boolean droppedGem = random.nextDouble() < gemDropChance;
        if (droppedGem) {
            Vector2D gemPosition = enemyPosition.copy().add(8.0, 0.0);
            items.add(new GemItem(gemPosition, 1, null));
        }
        boolean droppedEnergy = random.nextDouble() < energyDropChance;
        if (droppedEnergy) {
            // Moi crystal hoi ngau nhien 8 -> 15 mana
            double manaAmount = 8 + random.nextInt(8);
            Vector2D energyPosition =
                    enemyPosition.copy().add(0.0, -10.0);
            items.add(new EnergyCrystal(energyPosition, manaAmount));
        }
        debug("Quai roi " + goldReward + " gold" + (droppedGem ? " + gem" : "") + (droppedEnergy ? " + energy" : "")
        );
    }

    //    ham dong bo thoi gian thuc moi 30s
    private void updateAutoSave(double deltaSeconds) {
        autoSaveTimer += deltaSeconds;

        if (autoSaveTimer < AUTO_SAVE_INTERVAL) {
            return;
        }
        autoSaveTimer = 0.0;
        // Luu run de Continue
        saveGameAsync();
        // Cong phan thuong moi vao tai khoan
        syncBankRewardsAsync();
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

    private void handleRoomBuffReward(Room room, Room.RoomState previousState) {
        if (room == null) {
            return;
        }
        if (room.getState() != Room.RoomState.CLEARED) {
            return;
        }
        if (previousState == Room.RoomState.CLEARED) {
            return;
        }
        if (buffRewardedRooms.contains(room)) {
            return;
        }
        buffRewardedRooms.add(room);
        if (room.getType() == Room.RoomType.START || room.getType() == Room.RoomType.REST) {
            return;
        }
        if (random.nextDouble() > ROOM_BUFF_DROP_CHANCE) {
            return;
        }
        spawnRoomBuff(room);
    }

    //sinh bufff trong phong
    private void spawnRoomBuff(Room room) {

        Vector2D position = mapManager.findRandomWalkablePositionInRoom(room, random, 18);
        if (position == null) {
            return;
        }

        BuffType[] types = BuffType.values();

        BuffType buff = types[random.nextInt(types.length)];

        items.add(new BuffItem(position, buff, null));
    }

    private void restorePlayerRoomPosition() {
        if (player == null || player.getPosition() == null || mapManager == null
                || mapManager.getRooms() == null || mapManager.getRooms().isEmpty()) {
            return;
        }

        int savedRoomNumber = currentRoomNumber;
        int roomIndex = savedRoomNumber - 1;
        if (roomIndex < 0 || roomIndex >= mapManager.getRooms().size()) {
            System.err.println("Phòng đã lưu không hợp lệ: " + savedRoomNumber);
            currentRoomNumber = 1;
            return;
        }

        Room savedRoom = mapManager.getRooms().get(roomIndex);

        if (savedRoom == null || savedRoom.getBound() == null) {
            System.err.println("Không tìm thấy dữ liệu phòng: " + savedRoomNumber);
            currentRoomNumber = 1;
            return;
        }

        /*
         * Thử vị trí trung tâm phòng trước.
         */
        double centerX = savedRoom.getBound().getMinX() + savedRoom.getBound().getWidth() / 2.0;

        double centerY = savedRoom.getBound().getMinY() + savedRoom.getBound().getHeight() / 2.0;

        Vector2D targetPosition = new Vector2D(centerX, centerY);

        /*
         * Nếu tâm phòng bị tường hoặc obstacle chặn,
         * tìm một vị trí đi được khác trong chính phòng đó.
         */
        if (!canMoveTo(targetPosition, player.getRadius())) {
            targetPosition = findWalkablePositionInSavedRoom(savedRoom);
        }

        if (targetPosition == null) {
            System.err.println("Không tìm được vị trí hợp lệ trong phòng " + savedRoomNumber);

            /*
             * Không gán currentRoomNumber về 1 ở đây.
             * Giữ nguyên số phòng save để dễ phát hiện lỗi.
             */
            return;
        }
        player.getPosition().set(targetPosition);
        currentRoom = savedRoom;
        currentRoomNumber = savedRoomNumber;

        System.out.println("Đã khôi phục Player tại phòng " + currentRoomNumber + " | position=" + player.getPosition());
    }

    private Vector2D findWalkablePositionInSavedRoom(Room savedRoom) {
        if (savedRoom == null || savedRoom.getBound() == null || mapManager == null || player == null) {
            return null;
        }
        /*
         * Ưu tiên dùng hàm tìm vị trí ngẫu nhiên
         * trong phòng đã có sẵn trong MapManager.
         */
        for (int attempt = 0; attempt < 50; attempt++) {
            Vector2D candidate =
                    mapManager.findRandomWalkablePositionInRoom(savedRoom, random, player.getRadius());

            if (candidate == null) {
                continue;
            }

            if (!savedRoom.getBound().contains(candidate.getX(), candidate.getY())) {
                continue;
            }

            if (canMoveTo(candidate, player.getRadius())) {
                return candidate;
            }
        }

        /*
         * Fallback: quét các điểm trong phòng theo tile,
         * tránh phụ thuộc hoàn toàn vào random.
         */
        double tileSize = mapManager.getTileSize();
        double minX = savedRoom.getBound().getMinX() + tileSize;
        double minY = savedRoom.getBound().getMinY() + tileSize;
        double maxX = savedRoom.getBound().getMaxX() - tileSize;
        double maxY = savedRoom.getBound().getMaxY() - tileSize;
        for (double y = minY; y <= maxY; y += tileSize) {
            for (double x = minX; x <= maxX; x += tileSize) {
                Vector2D candidate = new Vector2D(x, y);
                if (canMoveTo(candidate, player.getRadius())) {
                    return candidate;
                }
            }
        }
        return null;
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

    public boolean isSaveLoadInProgress() {
        return saveLoadInProgress.get();
    }

    public void startNewGameFromMenu() {
        /*
         * Chot phan thuong cua run cu truoc khi reset HUD.
         */
        syncBankRewardsAsync();
        pendingPlayerSave = null;
        currentRoomNumber = 1;
        // Chi reset tien cua run, khong reset users.gold_bank
        gold = 0;
        gems = 0;
        score = 0;
        playerEnergy = 100.0;
        autoSaveTimer = 0.0;
        playerDeathEffect = null;
        playerDeathHandled = false;
        /*
         * pending da duoc snapshot trong syncBankRewardsAsync().
         * Tien moi cua run moi se duoc tinh tu 0.
         */
        synchronized (bankRewardLock) {
            pendingBankGold = 0;
            pendingBankGems = 0;
        }
        levelManager.startNewRun();
        initializeRunWeaponLoadout();
        loadCurrentLevel(true);
        playGameBGM();
        changeState(GameState.PLAYING);
        saveGameAsync();
    }
    public void continueGameFromMenu() {

        if (pendingPlayerSave == null) {
            System.err.println("Khong co save de Continue.");
            return;
        }

        /*
         * TAM THOI:
         * dung loadout hien tai tu Shop.
         *
         * Sau nay nen restore 2 slot tu PlayerSave.
         */
        initializeRunWeaponLoadout();

        loadCurrentLevel(true);

        playGameBGM();

        changeState(GameState.PLAYING);
    }
    private void initializeRunWeaponLoadout() {
        WeaponSelectionManager manager = WeaponSelectionManager.getInstance();
        runWeaponSlot1 = manager.getSlot1();
        runWeaponSlot2 = manager.getSlot2();
        activeRunWeaponSlot = 0;
        if (runWeaponSlot1 == null) {
            runWeaponSlot1 = WeaponType.BLASTER;
        }
        if (runWeaponSlot2 == null) {
            runWeaponSlot2 = WeaponType.OLD_SWORD;
        }
    }
    private void equipActiveRunWeapon() {

        if (player == null) {
            return;
        }
        WeaponType weaponType = getActiveRunWeaponType();
        if (weaponType == null) {
            return;
        }
        player.equipWeapon(weaponType.createWeapon());
    }
    private WeaponType getActiveRunWeaponType() {
        if (activeRunWeaponSlot == 0) {
            return runWeaponSlot1;
        }
        return runWeaponSlot2;
    }

    // Tao portal ket thuc khi Boss Room vua duoc clear.
    private void handleBossRoomCleared(Room room, Room.RoomState previousState) {
        if (room == null || endingPortal != null || endingStoryTriggered) return;
        if (room.getType() != Room.RoomType.BOSS) return;

        boolean justCleared =
                previousState != Room.RoomState.CLEARED
                        && room.getState() == Room.RoomState.CLEARED;

        if (!justCleared || !room.hasSpawnedEnemies()) return;

        spawnEndingPortal(room);
    }
    private void spawnEndingPortal(Room room) {
        if (room == null || room.getBound() == null) return;

        BoundingBox bound = room.getBound();

        double x = bound.getMinX() + bound.getWidth() * 0.5;
        double y = bound.getMinY() + bound.getHeight() * 0.5;

        endingPortal = new EndingPortal(new Vector2D(x, y));

        SoundManager.getInstance().playSFX("portal_open");

        debug("Ending portal spawned.");
    }
    private void updateEndingPortal(double deltaSeconds) {
        if (endingPortal == null || endingStoryTriggered || player == null) return;

        endingPortal.update(deltaSeconds);

        if (!endingPortal.intersects(player)) return;

        endingStoryTriggered = true;

        SoundManager.getInstance().stopBGM();
        changeState(GameState.ENDING_STORY);
    }
    // Ket thuc run sau cinematic Boss.
    public void finishEndingRun() {
        SoundManager.getInstance().stopBGM();

        endingPortal = null;
        endingStoryTriggered = false;

        rewardPicker = null;
        rewardPickerShown = false;

        currentRoom = null;
        currentRoomNumber = 1;

        /*
         * Khong load map tai day.
         * New Game se tu start run moi tu Spawn Room.
         */
    }

    // Hien feedback khi Player su dung Rest Shrine.
    public void showRestShrineEffect(Vector2D position) {
        if (position == null) return;
        restHealEffects.add(new RestHealEffect(position));
        floatingTextManager.spawnCustom(
                "RESTORED",
                position.copy().add(0.0, -22.0),
                Color.AQUAMARINE
        );

        if (particleManager != null) {
            particleManager.spawnHitImpact(position);
        }
    }
    // Cap nhat hieu ung hoi phuc cua Rest Shrine.
    private void updateRestHealEffects(double deltaSeconds) {
        for (RestHealEffect effect : restHealEffects) {
            if (effect != null) {
                effect.update(deltaSeconds);
            }
        }
        restHealEffects.removeIf(
                effect -> effect == null || effect.isFinished()
        );
    }

    private void playGameBGM() {
        SoundManager sound = SoundManager.getInstance();
        sound.stopBGM();
        sound.playBGM("/assets/Audio/StartGame.mp3");
    }
    public interface GameStateListener {
        void onStateChanged(GameState newState);
    }

}
//NOTE : cac ham xu ly va cham
// Player - titled (mapmanager): cua room
//Player - enemy (gameworld)
//enemy-enemy - (enemy)
//bullet - wall
//giua vat can player , enemy , bullet - xu li su khac nhau giua dan cua enemy voi vat can