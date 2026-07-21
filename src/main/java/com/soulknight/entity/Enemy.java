package com.soulknight.entity;

import com.soulknight.entity.Boss;
import com.soulknight.engine.GameWorld;
import com.soulknight.event.GameEventListener;
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

        Vector2D playerPos = world.getPlayer().getPosition();
        Vector2D enemyPos = getPosition();
        double distanceToPlayer = enemyPos.distance(playerPos);

        // Chia hành vi AI theo từng loại quái vật (Archetype)
        switch (archetype) {
            case SLIME -> {
                // AI Mặc định: Đi thẳng tới Player, nếu sát bên thì gây sát thương va chạm
                standardChaseAndContact(world, playerPos, deltaSeconds);
            }
            case SKELETON_ARCHER -> {
                // AI Lính canh: Giữ khoảng cách, tìm cách né/nấp khi Player đến quá gần
                guardianAI(world, playerPos, distanceToPlayer, deltaSeconds);
            }
            case ELITE_MINION -> {
                // Sử dụng cho Lợn Rừng (hoặc chỉnh lại tên Archetype tùy bạn): Cơ chế Húc (Dash) tốc độ cao
                wildBoarAI(world, playerPos, distanceToPlayer, deltaSeconds);
            }
            case GRAND_KNIGHT -> {
                // Logic của Boss giữ nguyên hoặc tùy biến thêm
                standardChaseAndContact(world, playerPos, deltaSeconds);
            }
        }
    }
    /**
     * Xu li va cham giua quai va quai tranh de chung de len nhau
     */
    public void separateFromOtherEnemies(GameWorld world,List<Enemy> allEnemies, double deltaSeconds) {
        for (Enemy other : allEnemies) {
            if (other == this || !other.isAlive()) continue;

            double dist = getPosition().distance(other.getPosition());
            double minDist = this.getRadius() + other.getRadius(); // khoang cach nho nhat tong ban kinh va cham

            // neu hai con quai de len nhau
            if (dist < minDist && dist > 0) {
                Vector2D pushDir = getPosition().copy().subtract(other.getPosition());
                pushDir.normalize();

                // day nhau ra
                double overlap = minDist - dist;
                double pushSpeed = 100.0; // toc do day

                this.move(world, pushDir.getX() * overlap * pushSpeed * deltaSeconds,
                        pushDir.getY() * overlap * pushSpeed * deltaSeconds);
            }
        }
    }

    // ==================== CÁC HÀM AI CHI TIẾT ====================

    /**
     * AI Lính canh bắn tên (SKELETON_ARCHER):
     * - Ở xa: Bắn tên bình thường.
     * - Ở quá gần (dưới 120px): Hoảng loạn lùi lại / di chuyển vuông góc để né đâm thẳng vào player.
     * - Phản công khi áp sát: Nếu cooldown hồi xong, kích hoạt đòn đánh cận chiến đẩy lui.
     */
    private void guardianAI(GameWorld world, Vector2D playerPos, double distance, double deltaSeconds) {
        Vector2D toPlayer = playerPos.copy().subtract(getPosition());

        if (distance < 120.0) {
            // TRẠNG THÁI Ở QUÁ GẦN: Tìm cách rút lui hoặc nấp sau (di chuyển ngược hướng hoặc dạt sang bên)
            Vector2D escapeDirection = toPlayer.copy().scale(-1.0); // Hướng đi lùi

            // Thêm một chút góc lệch (vuông góc) để quái đi vòng quanh/né tránh thay vì lùi lùi thẳng tuột
            escapeDirection.add(new Vector2D(-toPlayer.getY(), toPlayer.getX()).scale(0.5));

            if (escapeDirection.length() > 0.0) {
                escapeDirection.normalize().scale(moveSpeed * 1.2 * deltaSeconds); // Chạy trốn nhanh hơn tí
                move(world, escapeDirection.getX(), escapeDirection.getY());
            }

            // Phản công cận chiến khi ở quá gần (Cơ chế phản công khác biệt)
            if (distance <= 35.0 && attackCooldown <= 0.0) {
                world.getPlayer().takeDamage(contactDamage + 2); // Sát thương cận chiến đau hơn
                this.attackCooldown = 1.2; // Cooldown lâu hơn
                System.out.println("[AI LOG] Lính canh phản công cận chiến!");
            }
        } else {
            // TRẠNG THÁI AN TOÀN: Giữ khoảng cách và xả súng/vũ khí tầm xa
            if (distance > 250.0) {
                // Thong thả tiến lại gần nếu player ở quá xa tầm bắn
                Vector2D walkDir = toPlayer.copy();
                if (walkDir.length() > 0.0) {
                    walkDir.normalize().scale(moveSpeed * deltaSeconds);
                    move(world, walkDir.getX(), walkDir.getY());
                }
            }

            // Tấn công tầm xa bằng vũ khí bow/gun
            if (rangedWeapon != null && world.getPlayer().isAlive()) {
                rangedWeapon.attack(world, this, playerPos);
            }
        }
    }

    /**
     * AI Lợn rừng / Quái húc (ELITE_MINION):
     * - Đi chậm để định vị mục tiêu.
     * - Khi vào tầm ngắm (dưới 180px), khóa mục tiêu và TĂNG TỐC ĐỘ (húc mạnh) thẳng vào player.
     */
    /**
     * AI Lợn rừng / Quái húc (ELITE_MINION) - Đã sửa lỗi chồng hình
     */
    private void wildBoarAI(GameWorld world, Vector2D playerPos, double distance, double deltaSeconds) {
        Vector2D direction = playerPos.copy().subtract(getPosition());
        double currentSpeed = this.moveSpeed;

        // VÙNG CẤM: Giữ khoảng cách không cho đè hình
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
            // Cơ chế sửa lỗi lún hình khi húc quá nhanh: Đẩy ngược quái ra lại rìa
            Vector2D pushOut = getPosition().copy().subtract(playerPos);
            if (pushOut.length() > 0.0) {
                pushOut.normalize().scale(currentSpeed * 0.8 * deltaSeconds);
                move(world, pushOut.getX(), pushOut.getY());
            }
        }

        // Gây sát thương va chạm ngay tại rìa
        if (distance <= minAllowedDistance + 3.0 && attackCooldown <= 0.0) {
            world.getPlayer().takeDamage(contactDamage);
            this.attackCooldown = 1.0;
        }
    }

    /**
     * AI Đuổi bắt cơ bản (Slime và các quái thông thường)
     */
//    logic vung cam de tranh chong lan hinh anh , xu li va cham giua entity va entity
    private void standardChaseAndContact(GameWorld world, Vector2D playerPos, double deltaSeconds) {
        Vector2D direction = playerPos.copy().subtract(getPosition());
        double distance = direction.length();

        // VÙNG CẤM: Khoảng cách tối thiểu = Bán kính Player + Bán kính Quái + Khoảng cách an toàn nhỏ (ví dụ: 5px)
        double minAllowedDistance = world.getPlayer().getRadius() + this.getRadius() + 5.0;

        if (distance > minAllowedDistance) {
            // Chỉ di chuyển nếu còn ở xa ngoài vùng cấm
            if (distance > 0.0) {
                direction.normalize().scale(moveSpeed * deltaSeconds);
                move(world, direction.getX(), direction.getY());
            }
        } else {
            // Nếu lỡ đi quá sâu vào vùng cấm, chủ động đẩy nhẹ quái lùi ra ngoài
            Vector2D pushOut = getPosition().copy().subtract(playerPos);
            if (pushOut.length() > 0.0) {
                pushOut.normalize().scale(40.0 * deltaSeconds); // Tốc độ đẩy ra vừa phải
                move(world, pushOut.getX(), pushOut.getY());
            }
        }

        // Kích hoạt tấn công khi vừa chạm vào rìa (Không cần chờ lao vào tâm)
        if (distance <= minAllowedDistance + 2.0 && attackCooldown <= 0.0) {
            world.getPlayer().takeDamage(contactDamage);
            attackCooldown = 0.9;
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

    protected static Color colorFor(EnemyArchetype archetype) {
        return switch (archetype) {
            case SLIME -> Color.BLUE; // dam thang vao minh de tap ban
            case SKELETON_ARCHER -> Color.GRAY; // linh canh ne xong moi tan cong truc dienbi
            case ELITE_MINION -> Color.RED; // lon rung mau do luc dau ne sau do dam thang vao minh
            case GRAND_KNIGHT -> Color.PURPLE;
        };
    }
}
