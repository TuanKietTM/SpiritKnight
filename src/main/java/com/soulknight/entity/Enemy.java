package com.soulknight.entity;

import com.soulknight.engine.GameWorld;
import com.soulknight.event.GameEventListener;
import com.soulknight.map.Obstacle;
import com.soulknight.utils.Vector2D;
import com.soulknight.weapon.Weapon;
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

    public Enemy(EnemyArchetype archetype, Vector2D spawnPoint, double radius, int health, double moveSpeed,
                 int contactDamage, Weapon rangedWeapon, GameEventListener eventListener) {
        super(spawnPoint, radius, health, colorFor(archetype));
        this.archetype = archetype;
        this.moveSpeed = moveSpeed;
        this.contactDamage = contactDamage;
        this.rangedWeapon = rangedWeapon;
        this.eventListener = eventListener;
    }

    @Override
    public void update(GameWorld world, double deltaSeconds) {
        attackCooldown = Math.max(0.0, attackCooldown - deltaSeconds);
        if (rangedWeapon != null) {
            rangedWeapon.tick(deltaSeconds);
        }

        // Tự động đẩy nhau ra để tránh chồng lấp hình ảnh giữa các quái
        if (world.getEnemies() != null) {
            separateFromOtherEnemies(world, world.getEnemies(), deltaSeconds);
        }

        Vector2D playerPos = world.getPlayer().getPosition();
        Vector2D enemyPos = getPosition();
        double distanceToPlayer = enemyPos.distance(playerPos);

        // Chia hành vi AI theo từng loại quái vật (Archetype)
        switch (archetype) {
            case SLIME -> {
                // AI Mặc định: Đi thẳng tới Player, gây sát thương va chạm
                standardChaseAndContact(world, playerPos, deltaSeconds);
            }
            case SKELETON_ARCHER -> {
                // AI Lính canh: Giữ khoảng cách, bắn tầm xa, né/nấp vật cản khi Player đến quá gần
                guardianAI(world, playerPos, distanceToPlayer, deltaSeconds);
            }
            case ELITE_MINION -> {
                // AI Lợn Rừng: Cơ chế Húc (Dash) tốc độ cao khi đi vào tầm ngắm
                wildBoarAI(world, playerPos, distanceToPlayer, deltaSeconds);
            }
            case GRAND_KNIGHT -> {
                // Logic Boss đuổi bắt và gây sát thương
                standardChaseAndContact(world, playerPos, deltaSeconds);
            }
        }
    }

    /**
     * Xử lý va chạm giữa các quái để tránh đè chồng hình lên nhau
     * VÀ tự đẩy ra nếu bị lỡ lún vào vật cản.
     */
    public void separateFromOtherEnemies(GameWorld world, List<Enemy> allEnemies, double deltaSeconds) {
        // 1. Đẩy ra khỏi các quái khác
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

        // 2. Đẩy ra khỏi Vật cản (Obstacle) nếu lỡ bị kẹt/chồng hình
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

                    // Đẩy quái ra phía ngoài tâm vật cản
                    double pushDistance = 150.0 * deltaSeconds;
                    getPosition().add(pushDir.getX() * pushDistance, pushDir.getY() * pushDistance);
                }
            }
        }
    }
    public void move(GameWorld world, double dx, double dy) {
        if (dx == 0 && dy == 0) return;

        Vector2D currentPos = getPosition();

        // 1. Thử di chuyển cả 2 hướng X và Y
        Vector2D newPos = new Vector2D(currentPos.getX() + dx, currentPos.getY() + dy);
        if (world.canMoveTo(newPos, getRadius())) {
            currentPos.set(newPos);
            return;
        }

        // 2. Nếu vướng vật cản/tường: Thử trượt theo trục X
        Vector2D posXOnly = new Vector2D(currentPos.getX() + dx, currentPos.getY());
        if (world.canMoveTo(posXOnly, getRadius())) {
            currentPos.set(posXOnly);
            return;
        }

        // 3. Thử trượt theo trục Y
        Vector2D posYOnly = new Vector2D(currentPos.getX(), currentPos.getY() + dy);
        if (world.canMoveTo(posYOnly, getRadius())) {
            currentPos.set(posYOnly);
        }
    }

    // ==================== CÁC HÀM AI CHI TIẾT ====================

    /**
     * AI Lính canh bắn tên (SKELETON_ARCHER):
     * - Ở xa: Bắn tên và kết hợp tìm điểm nấp đằng sau vật cản (`Obstacle`).
     * - Ở quá gần (dưới 120px): Tháo chạy dạt sang bên / lùi lại.
     * - Khi bị áp sát sát sạt (<= 35px): Phản công cận chiến đẩy lui.
     */
    private void guardianAI(GameWorld world, Vector2D playerPos, double distance, double deltaSeconds) {
        Vector2D toPlayer = playerPos.copy().subtract(getPosition());

        if (distance < 120.0) {
            // TRẠNG THÁI NGUY HIỂM: Rút lui hoặc né dạt sang bên
            Vector2D escapeDirection = toPlayer.copy().scale(-1.0); // Hướng lùi lại
            escapeDirection.add(new Vector2D(-toPlayer.getY(), toPlayer.getX()).scale(0.5)); // Lệch góc né tránh

            if (escapeDirection.length() > 0.0) {
                escapeDirection.normalize().scale(moveSpeed * 1.2 * deltaSeconds);
                move(world, escapeDirection.getX(), escapeDirection.getY());
            }

            // Phản công cận chiến nếu player áp sát
            if (distance <= 35.0 && attackCooldown <= 0.0) {
                world.getPlayer().takeDamage(contactDamage + 2);
                this.attackCooldown = 1.2;
            }
        } else {
            // TRẠNG THÁI AN TOÀN: Bắn tầm xa & ưu tiên di chuyển tìm chỗ nấp sau vật cản
            List<Obstacle> obstacles = world.getObstacles();
            if (obstacles != null && !obstacles.isEmpty() && distance < 280.0) {
                coverAI(world, playerPos, deltaSeconds);
            } else if (distance > 250.0) {
                // Di chuyển chậm lại gần nếu quá xa tầm bắn
                Vector2D walkDir = toPlayer.copy();
                if (walkDir.length() > 0.0) {
                    walkDir.normalize().scale(moveSpeed * deltaSeconds);
                    move(world, walkDir.getX(), walkDir.getY());
                }
            }

            // Tấn công tầm xa bằng vũ khí
            if (rangedWeapon != null && world.getPlayer().isAlive()) {
                rangedWeapon.attack(world, this, playerPos);
            }
        }
    }

    /**
     * AI Lợn rừng / Quái húc (ELITE_MINION):
     * Tăng tốc húc mạnh khi đi vào vùng kích hoạt (<= 180px) và xử lý chống lún hình.
     */
    private void wildBoarAI(GameWorld world, Vector2D playerPos, double distance, double deltaSeconds) {
        Vector2D direction = playerPos.copy().subtract(getPosition());
        double currentSpeed = this.moveSpeed;

        // VÙNG CẤM: Bán kính Player + Bán kính Quái + Khoảng an toàn
        double minAllowedDistance = world.getPlayer().getRadius() + this.getRadius() + 4.0;

        if (distance <= 180.0) {
            // Trạng thái húc tốc độ cao
            currentSpeed = this.moveSpeed * 2.2;
        }

        if (distance > minAllowedDistance) {
            if (direction.length() > 0.0) {
                direction.normalize().scale(currentSpeed * deltaSeconds);
                move(world, direction.getX(), direction.getY());
            }
        } else {
            // Đẩy ngược quái ra lại rìa nếu húc quá nhanh bị lún hình
            Vector2D pushOut = getPosition().copy().subtract(playerPos);
            if (pushOut.length() > 0.0) {
                pushOut.normalize().scale(currentSpeed * 0.8 * deltaSeconds);
                move(world, pushOut.getX(), pushOut.getY());
            }
        }

        // Gây sát thương va chạm
        if (distance <= minAllowedDistance + 3.0 && attackCooldown <= 0.0) {
            world.getPlayer().takeDamage(contactDamage);
            this.attackCooldown = 1.0;
        }
    }

    /**
     * AI Đuổi bắt cơ bản (Slime và Boss)
     */
    private void standardChaseAndContact(GameWorld world, Vector2D playerPos, double deltaSeconds) {
        Vector2D direction = playerPos.copy().subtract(getPosition());
        double distance = direction.length();

        double minAllowedDistance = world.getPlayer().getRadius() + this.getRadius() + 5.0;

        if (distance > minAllowedDistance) {
            if (distance > 0.0) {
                direction.normalize().scale(moveSpeed * deltaSeconds);
                move(world, direction.getX(), direction.getY());
            }
        } else {
            Vector2D pushOut = getPosition().copy().subtract(playerPos);
            if (pushOut.length() > 0.0) {
                pushOut.normalize().scale(40.0 * deltaSeconds);
                move(world, pushOut.getX(), pushOut.getY());
            }
        }

        if (distance <= minAllowedDistance + 2.0 && attackCooldown <= 0.0) {
            world.getPlayer().takeDamage(contactDamage);
            attackCooldown = 0.9;
        }
    }

    /**
     * AI NẤP VẬT CẢN: Tìm vị trí đằng sau vật cản gần nhất so với góc nhìn của Player.
     */
    public void coverAI(GameWorld world, Vector2D playerPos, double deltaSeconds) {
        List<Obstacle> obstacles = world.getObstacles();

        if (obstacles == null || obstacles.isEmpty()) {
            standardChaseAndContact(world, playerPos, deltaSeconds);
            return;
        }

        // 1. Tìm vật cản gần nhất
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
            // 2. Tính điểm nấp an toàn (Bán kính quái + một nửa đường chéo vật cản + khoảng an toàn)
            Vector2D obsCenter = closestObs.getCenter();
            Vector2D awayFromPlayer = obsCenter.copy().subtract(playerPos).normalize();

            // Tính khoảng cách nấp an toàn để không chui vào ruột vật cản
            double obsRadius = Math.max(closestObs.getWidth(), closestObs.getHeight()) / 2.0;
            double safeHideDistance = obsRadius + getRadius() + 15.0;

            Vector2D coverPoint = obsCenter.copy().add(
                    awayFromPlayer.getX() * safeHideDistance,
                    awayFromPlayer.getY() * safeHideDistance
            );

            // 3. Di chuyển tới điểm nấp nếu vị trí đó hợp lệ
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
                if (archetype == EnemyArchetype.GRAND_KNIGHT) {
                    eventListener.onBossDefeated((Boss) this);
                } else {
                    eventListener.onEnemyDefeated(this);
                }
            }
        }
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
            case GRAND_KNIGHT -> Color.PURPLE;
        };
    }
}