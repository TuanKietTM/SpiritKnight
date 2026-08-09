package com.soulknight.pet;

import com.soulknight.engine.Camera;
import com.soulknight.engine.GameWorld;
import com.soulknight.entity.Enemy;
import com.soulknight.utils.Vector2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

import java.net.URL;
import java.util.ArrayDeque;
import java.util.Deque;

public final class Pet {

    public enum State {
        IDLE,
        FOLLOWING,
        CATCHING_UP,
        CHASING,
        ATTACKING,
        STUCK
    }

    private static final double COLLISION_RADIUS = 6.0;
    private static final double START_FOLLOW_DISTANCE = 48.0;
    private static final double WAYPOINT_REACH_DISTANCE = 12.0;
    private static final double BREADCRUMB_SPACING = 16.0;
    private static final int MAX_BREADCRUMBS = 180;
    private static final double CATCH_UP_DISTANCE = 190.0;
    private static final double TELEPORT_DISTANCE = 560.0;
    private static final double STUCK_TIME_LIMIT = 1.15;
    private static final double FORCE_TELEPORT_TIME = 2.6;

    // Thong so combat co ban cua Pet, sau nay co the chuyen sang PetType
    private static final double ATTACK_DETECT_RADIUS = 140.0;
    private static final double ATTACK_RANGE = 50.0;
    private static final double MAX_COMBAT_DISTANCE_FROM_PLAYER = 220.0;
    private static final double CHASE_SPEED_MULTIPLIER = 1.15;
    private static final double ATTACK_COOLDOWN = 1.0;
    private static final int ATTACK_DAMAGE = 4;

    private final PetType type;

    // Animation Sprite Sheets
    private final Image idleSpriteSheet;
    private final Image runSpriteSheet;

    private final Deque<Vector2D> breadcrumbs = new ArrayDeque<>();
    private final Vector2D position;

    private Vector2D lastRecordedPlayerPosition;
    private Vector2D lastPetPosition;

    private State state = State.IDLE;

    private double animTimer = 0.0; // Timer dùng riêng cho Animation
    private double stuckTime;
    private double attackCooldown;
    private Enemy currentTarget;

    private boolean active = true;
    private boolean facingRight = true;

    public Pet(PetType type, double spawnX, double spawnY) {
        if (type == null || !type.hasPet()) {
            throw new IllegalArgumentException("PetType phải khác null và khác NONE.");
        }

        this.type = type;
        this.position = new Vector2D(spawnX, spawnY);
        this.lastRecordedPlayerPosition = new Vector2D(spawnX, spawnY);
        this.lastPetPosition = new Vector2D(spawnX, spawnY);

        this.idleSpriteSheet = loadImage(type.getIdleImagePath());
        this.runSpriteSheet = loadImage(type.getRunImagePath());
    }

    public void update(double deltaSeconds, double playerX, double playerY, GameWorld world) {
        if (!active || deltaSeconds <= 0.0 || world == null || world.getMapManager() == null) return;

        double safeDelta = Math.min(deltaSeconds, 0.05);
        Vector2D playerPosition = new Vector2D(playerX, playerY);

        if (attackCooldown > 0.0) attackCooldown = Math.max(0.0, attackCooldown - safeDelta);

        // Van ghi lai duong di cua Player de Pet quay ve sau khi combat ket thuc
        recordPlayerPath(playerPosition, world);

        double distanceToPlayer = position.distance(playerPosition);

        if (distanceToPlayer >= TELEPORT_DISTANCE) {
            teleportToSafePosition(playerPosition, world);
            return;
        }

        // Khi Player khong qua xa, Pet uu tien Enemy thay vi breadcrumb cua Player
        currentTarget = findNearestEnemy(world, playerPosition);

        if (currentTarget != null) {
            updateEnemyChase(safeDelta, playerPosition, world);
            animTimer += safeDelta;
            lastPetPosition.set(position);
            return;
        }

        // Khong co Enemy hop le thi Pet quay lai co che follow Player nhu cu
        Vector2D target = chooseTarget(playerPosition);
        double distanceToTarget = position.distance(target);

        if (distanceToTarget <= WAYPOINT_REACH_DISTANCE) {
            removeReachedBreadcrumbs();
            target = chooseTarget(playerPosition);
            distanceToTarget = position.distance(target);
        }

        if (breadcrumbs.isEmpty() && distanceToPlayer <= START_FOLLOW_DISTANCE) {
            state = State.IDLE;
            stuckTime = 0.0;
            animTimer += safeDelta;
            lastPetPosition.set(position);
            return;
        }

        state = distanceToPlayer >= CATCH_UP_DISTANCE ? State.CATCHING_UP : State.FOLLOWING;

        double speedMultiplier = calculateSpeedMultiplier(distanceToPlayer);
        double moveDistance = type.getMoveSpeed() * speedMultiplier * safeDelta;

        boolean moved = moveSmart(target, moveDistance, world);

        updateStuckState(moved, safeDelta, playerPosition, world);
        updateFacingDirection();

        animTimer += safeDelta;
        lastPetPosition.set(position);
    }


    // Tim Enemy gan nhat nhung khong cho Pet bi keo qua xa khoi Player
    private Enemy findNearestEnemy(GameWorld world, Vector2D playerPosition) {
        if (world == null || playerPosition == null || world.getEnemies() == null || world.getEnemies().isEmpty()) return null;

        // Player da qua xa thi Pet bo combat va uu tien quay ve
        if (position.distance(playerPosition) > MAX_COMBAT_DISTANCE_FROM_PLAYER) return null;

        Enemy nearestEnemy = null;
        double nearestDistance = ATTACK_DETECT_RADIUS;

        for (Enemy enemy : world.getEnemies()) {
            if (enemy == null || !enemy.isAlive() || enemy.getPosition() == null) continue;

            // Khong duoi Enemy nam qua xa khoi Player de tranh Pet chay mat
            if (enemy.getPosition().distance(playerPosition) > MAX_COMBAT_DISTANCE_FROM_PLAYER) continue;

            double distance = position.distance(enemy.getPosition());
            if (distance > nearestDistance) continue;

            nearestDistance = distance;
            nearestEnemy = enemy;
        }

        return nearestEnemy;
    }

    // Khi co muc tieu Pet se tu di theo Enemy cho den khi vao tam danh
    private void updateEnemyChase(double deltaSeconds, Vector2D playerPosition, GameWorld world) {
        if (currentTarget == null || !currentTarget.isAlive() || currentTarget.getPosition() == null) return;

        Vector2D enemyPosition = currentTarget.getPosition();
        double distanceToEnemy = position.distance(enemyPosition);

        facingRight = enemyPosition.getX() >= position.getX();

        // Da vao tam danh thi dung lai va tan cong
        if (distanceToEnemy <= ATTACK_RANGE) {
            state = State.ATTACKING;
            stuckTime = 0.0;
            updateCombat(world);
            return;
        }

        state = State.CHASING;

        double moveDistance = type.getMoveSpeed() * CHASE_SPEED_MULTIPLIER * deltaSeconds;
        boolean moved = moveSmart(enemyPosition, moveDistance, world);

        updateStuckState(moved, deltaSeconds, playerPosition, world);

        // moveSmart co the thay doi huong, sau do quay lai dung ve phia Enemy
        facingRight = enemyPosition.getX() >= position.getX();

        // Neu frame nay vua vao tam danh thi cho phep danh ngay
        updateCombat(world);
    }

    // Pet chi tan cong khi Enemy da nam trong tam danh, khong tu y duoi qua xa
    private void updateCombat(GameWorld world) {
        if (currentTarget == null || !currentTarget.isAlive() || currentTarget.getPosition() == null) return;

        double distance = position.distance(currentTarget.getPosition());
        if (distance > ATTACK_RANGE) return;

        facingRight = currentTarget.getPosition().getX() >= position.getX();
        if (attackCooldown > 0.0) return;

        attackEnemy(currentTarget, world);
        attackCooldown = ATTACK_COOLDOWN;
    }

    // Ban dau dung instant hit de test AI Pet, sau nay co the doi thanh projectile
    private void attackEnemy(Enemy enemy, GameWorld world) {
        if (enemy == null || !enemy.isAlive()) return;

        int healthBefore = enemy.getHealth();
        enemy.takeDamage(ATTACK_DAMAGE);
        int realDamage = healthBefore - enemy.getHealth();

        if (realDamage <= 0) return;

        state = State.ATTACKING;

        if (world.getFloatingTextManager() != null) {
            world.getFloatingTextManager().spawnCustom(
                    "-" + realDamage,
                    enemy.getPosition(),
                    javafx.scene.paint.Color.LIGHTBLUE
            );
        }
    }

    private void recordPlayerPath(Vector2D playerPosition, GameWorld world) {
        if (lastRecordedPlayerPosition == null) {
            lastRecordedPlayerPosition = playerPosition.copy();
            return;
        }

        double distance = lastRecordedPlayerPosition.distance(playerPosition);
        if (distance < BREADCRUMB_SPACING) return;

        if (!world.canMoveTo(playerPosition, COLLISION_RADIUS)) return;

        if (distance > 220.0) {
            breadcrumbs.clear();
        }

        breadcrumbs.addLast(playerPosition.copy());
        lastRecordedPlayerPosition.set(playerPosition);

        while (breadcrumbs.size() > MAX_BREADCRUMBS) {
            breadcrumbs.removeFirst();
        }
    }

    private Vector2D chooseTarget(Vector2D playerPosition) {
        Vector2D waypoint = breadcrumbs.peekFirst();
        return waypoint != null ? waypoint : playerPosition;
    }

    private void removeReachedBreadcrumbs() {
        while (!breadcrumbs.isEmpty()) {
            Vector2D waypoint = breadcrumbs.peekFirst();
            if (waypoint == null || position.distance(waypoint) > WAYPOINT_REACH_DISTANCE) {
                break;
            }
            breadcrumbs.removeFirst();
        }
    }

    private boolean moveSmart(Vector2D target, double moveDistance, GameWorld world) {
        Vector2D difference = target.copy().subtract(position);
        double distance = difference.length();

        if (distance <= 0.001) return false;

        difference.normalize();
        double actualMovement = Math.min(moveDistance, distance);
        double moveX = difference.getX() * actualMovement;
        double moveY = difference.getY() * actualMovement;

        if (tryMove(moveX, moveY, world)) return true;

        boolean preferX = Math.abs(moveX) >= Math.abs(moveY);
        if (preferX) {
            if (tryMove(moveX, 0.0, world)) return true;
            if (tryMove(0.0, moveY, world)) return true;
        } else {
            if (tryMove(0.0, moveY, world)) return true;
            if (tryMove(moveX, 0.0, world)) return true;
        }

        double baseAngle = Math.atan2(difference.getY(), difference.getX());
        double[] angleOffsets = {
                Math.toRadians(22.5), Math.toRadians(-22.5),
                Math.toRadians(45.0), Math.toRadians(-45.0),
                Math.toRadians(67.5), Math.toRadians(-67.5),
                Math.toRadians(90.0), Math.toRadians(-90.0)
        };

        for (double offset : angleOffsets) {
            double angle = baseAngle + offset;
            double altX = Math.cos(angle) * actualMovement;
            double altY = Math.sin(angle) * actualMovement;
            if (tryMove(altX, altY, world)) return true;
        }

        return false;
    }

    private boolean tryMove(double moveX, double moveY, GameWorld world) {
        if (Math.abs(moveX) < 0.0001 && Math.abs(moveY) < 0.0001) return false;

        Vector2D candidate = position.copy().add(moveX, moveY);
        if (!world.canMoveTo(candidate, COLLISION_RADIUS)) return false;

        position.set(candidate);
        return true;
    }

    private void updateStuckState(boolean moved, double deltaSeconds, Vector2D playerPosition, GameWorld world) {
        double actualMovement = lastPetPosition.distance(position);
        if (moved && actualMovement > 0.2) {
            stuckTime = 0.0;
            return;
        }

        stuckTime += deltaSeconds;
        if (stuckTime >= STUCK_TIME_LIMIT) {
            state = State.STUCK;
            if (!breadcrumbs.isEmpty()) breadcrumbs.removeFirst();
        }

        if (stuckTime >= FORCE_TELEPORT_TIME) {
            teleportToSafePosition(playerPosition, world);
        }
    }

    private void teleportToSafePosition(Vector2D playerPosition, GameWorld world) {
        double[][] offsets = {
                {-30.0, 15.0}, {30.0, 15.0}, {-20.0, -20.0}, {20.0, -20.0},
                {0.0, 30.0}, {0.0, -30.0}
        };

        for (double[] offset : offsets) {
            Vector2D candidate = playerPosition.copy().add(offset[0], offset[1]);
            if (world.canMoveTo(candidate, COLLISION_RADIUS)) {
                position.set(candidate);
                resetPath(playerPosition);
                return;
            }
        }

        position.set(playerPosition);
        resetPath(playerPosition);
    }

    private void resetPath(Vector2D playerPosition) {
        breadcrumbs.clear();
        lastRecordedPlayerPosition = playerPosition.copy();
        lastPetPosition = position.copy();
        stuckTime = 0.0;
        state = State.IDLE;
    }

    private double calculateSpeedMultiplier(double distanceToPlayer) {
        if (distanceToPlayer >= 340.0) return 2.1;
        if (distanceToPlayer >= CATCH_UP_DISTANCE) return 1.55;
        return 1.0;
    }

    private void updateFacingDirection() {
        double diffX = position.getX() - lastPetPosition.getX();
        if (diffX > 0.1) facingRight = true;
        else if (diffX < -0.1) facingRight = false;
    }

    public void onRoomChanged(double playerX, double playerY) {
        position.set(playerX - 20.0, playerY);
        resetPath(new Vector2D(playerX, playerY));
    }

    /**
     * RENDER SPRITE SHEET ANIMATION
     */
    public void render(GraphicsContext graphicsContext, Camera camera) {
        if (!active || graphicsContext == null || camera == null) return;

        // Xử lý chọn Sprite Sheet và tính Frame hiện tại
        boolean isRunning = (state == State.FOLLOWING || state == State.CATCHING_UP || state == State.CHASING);
        Image currentSheet = isRunning ? runSpriteSheet : idleSpriteSheet;

        if (currentSheet == null) return;

        int totalFrames = isRunning ? type.getRunFrameCount() : type.getIdleFrameCount();
        if (totalFrames <= 0) totalFrames = 1;

        // Tính chỉ số Frame hiện tại theo thời gian
        int currentFrame = (int) (animTimer / type.getFrameDuration()) % totalFrames;

        // Tọa độ cắt từ Sprite Sheet (Nằm ngang)
        double sx = currentFrame * type.getFrameWidth();
        double sy = 0.0; // Giả định sprite sheet trải ngang 1 hàng
        double sw = type.getFrameWidth();
        double sh = type.getFrameHeight();

        // Tọa độ trên màn hình
        double zoom = camera.getZoom();
        double renderX = camera.worldToScreenX(position.getX());
        double renderY = camera.worldToScreenY(position.getY());

        double destWidth = type.getRenderWidth() * zoom;
        double destHeight = type.getRenderHeight() * zoom;

        // Căn giữa tâm chân của Pet
        double drawX = renderX - destWidth / 2.0;
        double drawY = renderY - destHeight / 2.0;

        if (facingRight) {
            graphicsContext.drawImage(
                    currentSheet,
                    sx, sy, sw, sh,
                    drawX, drawY, destWidth, destHeight
            );
        } else {
            graphicsContext.save();
            graphicsContext.translate(renderX, 0.0);
            graphicsContext.scale(-1.0, 1.0);
            graphicsContext.drawImage(
                    currentSheet,
                    sx, sy, sw, sh,
                    -destWidth / 2.0, drawY, destWidth, destHeight
            );
            graphicsContext.restore();
        }
    }

    private Image loadImage(String path) {
        if (path == null || path.isBlank()) return null;
        URL resource = Pet.class.getResource(path);
        if (resource == null) {
            System.err.println("Không tìm thấy ảnh Pet Sprite Sheet: " + path);
            return null;
        }
        return new Image(resource.toExternalForm(), false);
    }

    public void teleport(double newX, double newY) {
        position.set(newX, newY);
        breadcrumbs.clear();
        lastPetPosition.set(position);
        lastRecordedPlayerPosition.set(position);
        stuckTime = 0.0;
        state = State.IDLE;
    }

    public PetType getType() { return type; }
    public State getState() { return state; }
    public Vector2D getPosition() { return position; }
    public double getX() { return position.getX(); }
    public double getY() { return position.getY(); }
    public double getRadius() { return COLLISION_RADIUS; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}