package com.soulknight.entity;

import com.soulknight.engine.Camera;
import com.soulknight.engine.GameWorld;
import com.soulknight.event.GameEventListener;
import com.soulknight.map.Obstacle;
import com.soulknight.utils.Vector2D;
import com.soulknight.weapon.Bullet;
import com.soulknight.weapon.Weapon;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.List;

public class Enemy extends Entity {

    private final EnemyArchetype archetype;
    private final double moveSpeed;
    private final int contactDamage;
    private final Weapon rangedWeapon;
    private final GameEventListener eventListener;
    private double attackCooldown;
    private boolean defeatNotified;
    private double patrolStuckTimer = 0.0; // Bo dem thoi gian chong ket khi di tuan

    private final EnemyAnimator animator;
    private boolean isFacingLeft = false;
    // State cho meleeAI
    public enum State { PATROL, CHASE }
    public enum RangedState {
        PATROL,
        AIMING,
        REPOSITION
    }
    private State currentState = State.PATROL;

    // Biến cho logic Patrol (Đi tuần)
    private Vector2D spawnPoint;        // Điểm xuất phát ban đầu để quanh quẩn
    private Vector2D patrolTarget;     // Điểm ngẫu nhiên đang hướng tới
    private double patrolWaitTimer = 0; // Thời gian dừng nghỉ giữa các điểm tuần
    private final double detectionRadius = 120.0; // Bán kính phát hiện Player (nếu vào tầm)
    private Vector2D lastKnownPlayerPos = null; // Vị trí cuối cùng nhìn thấy Player

    // Các biến xử lí cho lỗi kẹt tường khi Chase
    private Vector2D lastPosition = new Vector2D(0, 0); // Lưu vị trí ở frame trước để so sánh
    private double stuckTimer = 0.0;                   // Thời gian đã bị kẹt tường

    // Các biến cho quái bắn xa
    private RangedState rangedState = RangedState.PATROL;
    private double attackRange = 150.0;
    private double bulletSpeed = 250.0;
    private int bulletDamage = 10;
    private double bulletRadius = 7.5;
    private double rangedAttackCooldown = 1.5;
    private double aimTimer = 0.0;
    private Vector2D repositionTarget = null;


    public Enemy(EnemyArchetype archetype, Vector2D spawnPoint, double radius, int health, double moveSpeed,
                 int contactDamage, Weapon rangedWeapon, GameEventListener eventListener) {
        super(spawnPoint, radius, health, colorFor(archetype));
        this.archetype = archetype;
        this.moveSpeed = moveSpeed;
        this.contactDamage = contactDamage;
        this.rangedWeapon = rangedWeapon;
        this.eventListener = eventListener;
        this.animator = new EnemyAnimator(archetype);

        this.spawnPoint = spawnPoint.copy();
        this.patrolTarget = spawnPoint.copy(); // Đặt tạm bằng spawnPoint
        this.currentState = State.PATROL;     // Mặc định ban đầu đi tuần
        this.patrolWaitTimer = 0.5;           // Sau 0.5s chạy update() nó sẽ tự tìm điểm tuần chuẩn có world
    }

    @Override
    public void update(GameWorld world, double deltaSeconds) {
        attackCooldown = Math.max(0.0, attackCooldown - deltaSeconds);
        if (rangedWeapon != null) {
            rangedWeapon.tick(deltaSeconds);
        }

        if (world.getEnemies() != null) {
            separateFromOtherEnemies(world, world.getEnemies(), deltaSeconds);
        }

        Vector2D playerPos = world.getPlayer().getPosition();
        Vector2D enemyPos = getPosition();
        double distanceToPlayer = enemyPos.distance(playerPos);

//      Tu xoay mat vao player
        double diffX = playerPos.getX() - enemyPos.getX();
        this.isFacingLeft = (diffX < 0);

//        Chia tung loai quai
        switch (archetype) {
            case SLIME -> meleeAI(world, playerPos, distanceToPlayer, deltaSeconds);
            case SKELETON_ARCHER -> rangedAI(world, playerPos, deltaSeconds);
            case ELITE_MINION -> meleeAI(world, playerPos, distanceToPlayer, deltaSeconds);
        }

        // Cập nhật khung hình Animator
        animator.update(deltaSeconds);
    }

    public void move(GameWorld world, double dx, double dy) {
        if (dx == 0 && dy == 0) return;

        Vector2D currentPos = getPosition();

        Vector2D newPos = new Vector2D(currentPos.getX() + dx, currentPos.getY() + dy);
        if (world.canMoveTo(newPos, getRadius())) {
            currentPos.set(newPos);
            return;
        }

        Vector2D posXOnly = new Vector2D(currentPos.getX() + dx, currentPos.getY());
        if (world.canMoveTo(posXOnly, getRadius())) {
            currentPos.set(posXOnly);
            return;
        }

        Vector2D posYOnly = new Vector2D(currentPos.getX(), currentPos.getY() + dy);
        if (world.canMoveTo(posYOnly, getRadius())) {
            currentPos.set(posYOnly);
        }
    }

    @Override
    public void render(GraphicsContext gc, Camera camera) {
        double renderWidth = getRadius() * 2.5;
        double renderHeight = getRadius() * 2.5;

        animator.render(
                gc,
                camera,
                getPosition().getX(),
                getPosition().getY(),
                renderWidth,
                renderHeight,
                getRadius(),
                isFacingLeft
        );
    }



    public void separateFromOtherEnemies(GameWorld world, List<Enemy> allEnemies, double deltaSeconds) {
        for (Enemy other : allEnemies) {
            if (other == this || !other.isAlive()) continue;

            double dist = getPosition().distance(other.getPosition());
            double minDist = this.getRadius() + other.getRadius();

            if (dist < minDist && dist > 0) {
                Vector2D pushDir = getPosition().copy().subtract(other.getPosition());
                pushDir.normalize();

                double overlap = minDist - dist;
                double pushSpeed = 80.0;

                this.move(world, pushDir.getX() * overlap * pushSpeed * deltaSeconds,
                        pushDir.getY() * overlap * pushSpeed * deltaSeconds);
            }
        }

        List<Obstacle> obstacles = world.getObstacles();
        if (obstacles != null) {
            for (Obstacle obstacle : obstacles) {
                if (obstacle.intersectsCircle(getPosition(), getRadius())) {
                    Vector2D obsCenter = obstacle.getCenter();
                    Vector2D pushDir = getPosition().copy().subtract(obsCenter);
                    if (pushDir.length() == 0) {
                        pushDir = new Vector2D(1.0, 0.0);
                    }
                    pushDir.normalize();

                    double pushDistance = 150.0 * deltaSeconds;
                    getPosition().add(pushDir.getX() * pushDistance, pushDir.getY() * pushDistance);
                }
            }
        }
    }

    private void guardianAI(GameWorld world, Vector2D playerPos, double distance, double deltaSeconds) {
        Vector2D toPlayer = playerPos.copy().subtract(getPosition());

        if (distance < 120.0) {
            Vector2D escapeDirection = toPlayer.copy().scale(-1.0);
            escapeDirection.add(new Vector2D(-toPlayer.getY(), toPlayer.getX()).scale(0.5));

            if (escapeDirection.length() > 0.0) {
                escapeDirection.normalize().scale(moveSpeed * 1.2 * deltaSeconds);
                move(world, escapeDirection.getX(), escapeDirection.getY());
            }

            if (distance <= 35.0 && attackCooldown <= 0.0) {
                world.getPlayer().takeDamage(contactDamage + 2);
                this.attackCooldown = 1.2;
            }
        } else {
            List<Obstacle> obstacles = world.getObstacles();
            if (obstacles != null && !obstacles.isEmpty() && distance < 280.0) {
                coverAI(world, playerPos, deltaSeconds);
            } else if (distance > 250.0) {
                Vector2D walkDir = toPlayer.copy();
                if (walkDir.length() > 0.0) {
                    walkDir.normalize().scale(moveSpeed * deltaSeconds);
                    move(world, walkDir.getX(), walkDir.getY());
                }
            }

//            Chi ban khi duong ngam ban ro rang khong co
            if (rangedWeapon != null && world.getPlayer().isAlive()) {
                boolean canSeePlayer = world.hasClearLineOfSight(getPosition(), playerPos);

                if (canSeePlayer) {
                    rangedWeapon.attack(world, this, playerPos);
                }
            }
        }
    }

    private void wildBoarAI(GameWorld world, Vector2D playerPos, double distance, double deltaSeconds) {
        Vector2D direction = playerPos.copy().subtract(getPosition());
        double currentSpeed = this.moveSpeed;

        double minAllowedDistance = world.getPlayer().getRadius() + this.getRadius() + 4.0;

        if (distance <= 180.0) {
            currentSpeed = this.moveSpeed * 2.2;
        }

        if (distance > minAllowedDistance) {
            if (direction.length() > 0.0) {
                direction.normalize().scale(currentSpeed * deltaSeconds);
                move(world, direction.getX(), direction.getY());
            }
        } else {
            Vector2D pushOut = getPosition().copy().subtract(playerPos);
            if (pushOut.length() > 0.0) {
                pushOut.normalize().scale(currentSpeed * 0.8 * deltaSeconds);
                move(world, pushOut.getX(), pushOut.getY());
            }
        }

        if (distance <= minAllowedDistance + 3.0 && attackCooldown <= 0.0) {
            world.getPlayer().takeDamage(contactDamage);
            this.attackCooldown = 1.0;
        }
    }

    private void meleeAI(GameWorld world, Vector2D playerPos, double distanceToPlayer, double deltaSeconds) {
        boolean canSeePlayer = world.hasClearLineOfSight(getPosition(), playerPos);

        // Chức năng quản lí trạng thái
        if (canSeePlayer && distanceToPlayer <= detectionRadius) {
            // Điều kiện xác định đuổi theo
            this.lastKnownPlayerPos = playerPos.copy(); // Vị trí cuối cùng của player
            this.currentState = State.CHASE;
            this.stuckTimer = 0.0;
        } else if (currentState == State.CHASE) {

            if (lastKnownPlayerPos != null) {
                // Khoảng cách hiện tại của enemy và lastKnownPlayerPos
                double distToLastPos = getPosition().distance(lastKnownPlayerPos);
                // Tính quãng đường enemy di chuyển giữa 2 frame liên tiếp
                double movedDistance = getPosition().distance(lastPosition);

                // Vòng điều kiện tính thời gian kẹt
                if (movedDistance < 0.5 * deltaSeconds) {
                    this.stuckTimer += deltaSeconds; // Tích lũy thời gian bị kẹt
                } else {
                    this.stuckTimer = Math.max(0.0, this.stuckTimer - deltaSeconds); // Di chuyển mượt thì giảm timer kẹt
                }

                // Điều kiện chuyển đổi từ Chase sang Patrol
                if (distToLastPos <= 20.0 || distanceToPlayer > detectionRadius * 1.5 || this.stuckTimer >= 1.5) {
                    this.currentState = State.PATROL;
                    this.lastKnownPlayerPos = null;
                    this.patrolWaitTimer = 1.0;
                    this.patrolTarget = generateNewPatrolTarget(world, getPosition(), 120.0);
                    this.stuckTimer = 0.0;
                }
            } else {
                this.currentState = State.PATROL;
                this.patrolTarget = generateNewPatrolTarget(world, getPosition(), 120.0);
                this.stuckTimer = 0.0;
            }
        }
        // cập nhật lại vị trí enemy qua từng frame để tính thời gian kẹt
        this.lastPosition = getPosition().copy();

        // Logic cho 2 trạng thái Chase và Patrol
        if (currentState == State.PATROL) {
            // Nếu thảo mãn vòng if này quái đứng im
            if (patrolWaitTimer > 0) {
                patrolWaitTimer -= deltaSeconds;
            } else {
                // Nếu chưa có điểm Patrol hoặc khoảng cachs đến < 15
                if (patrolTarget == null || getPosition().distance(patrolTarget) < 15.0) {
                    patrolWaitTimer = 1.0 + Math.random() * 1.5; // tính thời gian cho nghỉ
                    patrolTarget = generateNewPatrolTarget(world, getPosition(), 100.0);
                } else {
                    // Đi đến điểm Patrol
                    Vector2D dir = patrolTarget.copy().subtract(getPosition()); // Biến dir tác dụng tính hướng và khảng cách đén target
                    if (dir.length() > 0) {
                        dir.normalize().scale(moveSpeed * 0.4 * deltaSeconds); // tốc độ cho quái
                        move(world, dir.getX(), dir.getY()); // cập nhật vị trí mới
                    }
                }
            }
        }
        else {
            // Logic cho trạng thái Chase
            Vector2D targetPos = canSeePlayer ? playerPos : lastKnownPlayerPos;

            if (targetPos != null) {
                double distToTarget = getPosition().distance(targetPos);

                // Khoảng cách hợp lí giữa player và enemy
                double minAllowedDistance = world.getPlayer().getRadius() + getRadius() + 4.0;

                // Đến khoảng cách an toàn thì cho chạy
                if (distToTarget > minAllowedDistance) {
                    // Hàm chạy
                    smartMoveTo(world, targetPos, deltaSeconds);
                } else if (canSeePlayer) {
                    // đẩy ra khi va chạm
                    Vector2D pushOut = getPosition().copy().subtract(playerPos);
                    if (pushOut.length() > 0) {
                        pushOut.normalize().scale(30.0 * deltaSeconds);
                        move(world, pushOut.getX(), pushOut.getY());
                    }
                }

                // Logic cho đánh
                if (canSeePlayer && distanceToPlayer <= minAllowedDistance + 3.0 && attackCooldown <= 0.0) {
                    world.getPlayer().takeDamage(contactDamage);
                    attackCooldown = 1.0;
                }
            }
        }
    }

    public void rangedAI(GameWorld world, Vector2D playerPos, double deltaSeconds) {
        double distanceToPlayer = getPosition().distance(playerPos);

        // Trừ dần thời gian hồi chiêu BẮN ĐẠN
        if (rangedAttackCooldown > 0) {
            rangedAttackCooldown -= deltaSeconds;
        }

        // ==========================================
        // 1. QUẢN LÝ TRẠNG THÁI (FSM)
        // ==========================================
        switch (rangedState) {
            case PATROL:
                // Phát hiện Player trong tầm bắn VÀ đã hồi chiêu bắn xong
                if (distanceToPlayer <= attackRange && rangedAttackCooldown <= 0.0) {
                    this.rangedState = RangedState.AIMING;
                    this.aimTimer = 0.5; // Đứng khựng lại 0.25 giây để ngắm
                }
                break;

            case AIMING:
                // Trừ thời gian ngắm
                this.aimTimer -= deltaSeconds;
                if (this.aimTimer <= 0.0) {
                    // Bắn đạn về phía Player
                    shootBulletAt(world, playerPos);

                    // Gán Cooldown riêng cho bắn đạn (1.5 giây)
                    this.rangedAttackCooldown = 1.5;

                    // Chuyển sang chạy đổi vị trí
                    this.rangedState = RangedState.REPOSITION;
                    this.repositionTarget = generateNewPatrolTarget(world, getPosition(), 100.0);
                }
                break;

            case REPOSITION:
                // Đã hồi chiêu bắn xong VÀ Player vẫn trong tầm bắn -> Ngắm tiếp
                if (rangedAttackCooldown <= 0.0 && distanceToPlayer <= attackRange) {
                    this.rangedState = RangedState.AIMING;
                    this.aimTimer = 0.25;
                    this.repositionTarget = null;
                }
                // Player chạy quá xa tầm bắn -> Quay về Đi tuần
                else if (distanceToPlayer > attackRange * 1.3) {
                    this.rangedState = RangedState.PATROL;
                    this.repositionTarget = null;
                }
                break;
        }

        // ==========================================
        // 2. THỰC THI DI CHUYỂN
        // ==========================================
        if (rangedState == RangedState.PATROL) {
            if (patrolWaitTimer > 0) {
                patrolWaitTimer -= deltaSeconds;
            } else {
                if (patrolTarget == null || getPosition().distance(patrolTarget) < 15.0) {
                    patrolWaitTimer = 1.0 + Math.random() * 1.5;
                    patrolTarget = generateNewPatrolTarget(world, getPosition(), 100.0);
                } else {
                    smartMoveTo(world, patrolTarget, deltaSeconds * 0.4); // Tốc độ 40%
                }
            }

        } else if (rangedState == RangedState.AIMING) {
            // Đứng yên giơ súng ngắm

        } else if (rangedState == RangedState.REPOSITION) {
            if (repositionTarget != null) {
                if (getPosition().distance(repositionTarget) > 15.0) {
                    smartMoveTo(world, repositionTarget, deltaSeconds * 0.4); // Giữ tốc độ 40%
                } else {
                    // Đã tới điểm tản ra, tạo điểm tiếp theo nếu vẫn đang hồi chiêu bắn
                    this.repositionTarget = generateNewPatrolTarget(world, getPosition(), 80.0);
                }
            }
        }
    }

    // ==========================================
// HÀM PHỤ TRỢ: TÍNH HƯỚNG VÀ PHÓNG ĐẠN
// ==========================================
    private void shootBulletAt(GameWorld world, Vector2D targetPos) {
        // 1. Tính Vector hướng từ Quái tới vị trí Player tại thời điểm bắn
        Vector2D dir = targetPos.copy().subtract(getPosition());

        if (dir.length() > 0) {
            dir.normalize(); // Chuẩn hóa về Vector độ dài 1
        } else {
            dir = new Vector2D(1, 0); // Mặc định hướng sang phải nếu đứng trùng tọa độ
        }

        // 2. Tính Vector vận tốc đạn (Hướng * Tốc độ đạn)
        Vector2D bulletVelocity = dir.scale(bulletSpeed);

        // 3. Khởi tạo viên đạn mới theo đúng Constructor của class Bullet
        Bullet bullet = new Bullet(
                getPosition().copy(),   // Vị trí xuất phát (từ tâm Quái)
                bulletVelocity,         // Vận tốc đạn
                bulletDamage,           // Sát thương
                bulletRadius,           // Bán kính va chạm của đạn
                this,                   // Owner: Entity bắn ra đạn này (Enemy)
                Color.RED               // Màu dự phòng (nếu chưa load được ảnh dan.png)
        );

        // 4. Thêm viên đạn vào GameWorld
        // (Bạn lưu ý kiểm tra tên hàm thêm đạn trong GameWorld của bạn, ví dụ: addBullet hoặc spawnBullet)
        world.addBullet(bullet);
    }

    public void coverAI(GameWorld world, Vector2D playerPos, double deltaSeconds) {
        List<Obstacle> obstacles = world.getObstacles();
        double distanceToPlayer = getPosition().distance(playerPos);

        if (obstacles == null || obstacles.isEmpty()) {
            meleeAI(world, playerPos, distanceToPlayer, deltaSeconds);
            return;
        }

        Obstacle closestObs = null;
        double minDistance = Double.MAX_VALUE;
        for (Obstacle obs : obstacles) {
            double dist = getPosition().distance(obs.getCenter());
            if (dist < minDistance) {
                minDistance = dist;
                closestObs = obs;
            }
        }

        if (closestObs != null) {
            Vector2D obsCenter = closestObs.getCenter();
            Vector2D awayFromPlayer = obsCenter.copy().subtract(playerPos).normalize();

            double obsRadius = Math.max(closestObs.getWidth(), closestObs.getHeight()) / 2.0;
            double safeHideDistance = obsRadius + getRadius() + 15.0;

            Vector2D coverPoint = obsCenter.copy().add(
                    awayFromPlayer.getX() * safeHideDistance,
                    awayFromPlayer.getY() * safeHideDistance
            );

            double distToCover = getPosition().distance(coverPoint);
            if (distToCover > 10.0 && world.canMoveTo(coverPoint, getRadius())) {
                Vector2D moveDir = coverPoint.copy().subtract(getPosition()).normalize();
                double moveStep = getSpeed() * deltaSeconds;
                this.move(world, moveDir.getX() * moveStep, moveDir.getY() * moveStep);
            }
        }
    }

    @Override
    public void takeDamage(int amount) {
        super.takeDamage(amount);
        if (!isAlive() && !defeatNotified) {
            defeatNotified = true;
            if (eventListener != null) {
                eventListener.onEnemyDefeated(this);
            }
        }
    }

    private Vector2D generateNewPatrolTarget(GameWorld world, Vector2D center, double radius) {
        java.util.Random rand = new java.util.Random();

        for (int i = 0; i < 20; i++) {
            double angle = rand.nextDouble() * Math.PI * 2;
            // Giảm bán kính đi tuần xuống vừa phải (60px - 100px) để tránh đâm ra quá xa
            double dist = 40.0 + rand.nextDouble() * (radius - 40.0);
            double targetX = center.getX() + Math.cos(angle) * dist;
            double targetY = center.getY() + Math.sin(angle) * dist;

            Vector2D candidate = new Vector2D(targetX, targetY);

            if (world != null) {
                // Điểm đích phải di chuyển tới được với BÁN KÍNH CÓ KHOẢNG ĐỆM (+4px)
                if (!world.canMoveTo(candidate, getRadius() + 4.0)) continue;

                // Đường đi từ Quái tới Điểm đích phải hoàn toàn trống trải (LOS)
                if (world.hasClearLineOfSight(center, candidate)) {
                    return candidate;
                }
            }
        }
        // Nếu xung quanh quá chật hẹp, đứng im tại chỗ chờ lượt sau
        return center.copy();
    }

    private void smartMoveTo(GameWorld world, Vector2D targetPos, double deltaSeconds) {
        Vector2D dir = targetPos.copy().subtract(getPosition());
        if (dir.length() == 0) return;

        dir.normalize();
        double stepSize = moveSpeed * deltaSeconds;

        // 1. Thach thuc di thang trực tiep toi target
        Vector2D directStep = new Vector2D(
                getPosition().getX() + dir.getX() * stepSize,
                getPosition().getY() + dir.getY() * stepSize
        );

        // Neu di thang duoc va khong bi vuong tuong -> Di luon
        if (world.canMoveTo(directStep, getRadius())) {
            move(world, dir.getX() * stepSize, dir.getY() * stepSize);
            return;
        }

        // 2. Neu huong chinh bi vuong tuong -> Quet cac goc lach 30, 60, 90 do sang 2 ben
        double baseAngle = Math.atan2(dir.getY(), dir.getX());
        double[] offsets = { Math.PI / 6, -Math.PI / 6, Math.PI / 3, -Math.PI / 3, Math.PI / 2, -Math.PI / 2 };

        for (double offset : offsets) {
            double testAngle = baseAngle + offset;
            Vector2D testDir = new Vector2D(Math.cos(testAngle), Math.sin(testAngle));
            Vector2D testPos = new Vector2D(
                    getPosition().getX() + testDir.getX() * stepSize,
                    getPosition().getY() + testDir.getY() * stepSize
            );

            if (world.canMoveTo(testPos, getRadius())) {
                move(world, testDir.getX() * stepSize, testDir.getY() * stepSize);
                return;
            }
        }

        // 3. Neu tat ca goc nghieng deu nghen, dung Slide Physics X/Y mac dinh
        move(world, dir.getX() * stepSize, dir.getY() * stepSize);
    }

    public EnemyArchetype getArchetype() {
        return archetype;
    }

    public int getContactDamage() {
        return contactDamage;
    }

    public Weapon getRangedWeapon() {
        return rangedWeapon;
    }

    public double getSpeed() {
        return moveSpeed;
    }

    protected static Color colorFor(EnemyArchetype archetype) {
        return switch (archetype) {
            case SLIME -> Color.BLUE;
            case SKELETON_ARCHER -> Color.GRAY;
            case ELITE_MINION -> Color.RED;
            default -> Color.RED;
        };
    }
}