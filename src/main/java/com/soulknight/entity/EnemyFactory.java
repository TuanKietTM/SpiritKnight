package com.soulknight.entity;

import com.soulknight.event.GameEventListener;
import com.soulknight.level.LevelManager;
import com.soulknight.utils.Constants;
import com.soulknight.utils.Vector2D;
import com.soulknight.weapon.Gun;
import com.soulknight.weapon.Weapon;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class EnemyFactory {

    private final LevelManager levelManager;
    private final GameEventListener eventListener;

    public EnemyFactory(LevelManager levelManager, GameEventListener eventListener) {
        this.levelManager = levelManager;
        this.eventListener = eventListener;
    }

    public List<Enemy> createInitialEnemies(Random random, Vector2D playerSpawn, List<Vector2D> spawnPoints) {
        int count = levelManager.scaleEnemyCount(levelManager.getCurrentLevel().baseEnemyCount());
        List<Enemy> enemies = new ArrayList<>(count);
        LevelManager.LevelDefinition level = levelManager.getCurrentLevel();

        for (int i = 0; i < count; i++) {
            Vector2D spawnPoint = spawnPoints.get(i % spawnPoints.size());
            if (level.number() == 1) {
                enemies.add(createSlime(spawnPoint));
            } else if (level.number() == 2) {
                enemies.add(i % 3 == 0 ? createSkeletonArcher(spawnPoint) : createSlime(spawnPoint));
            } else {
                enemies.add(createEliteMinion(spawnPoint));
            }
        }

        if (level.bossLevel()) {
            enemies.add(createGrandKnight(new Vector2D(playerSpawn.getX() + 700.0, playerSpawn.getY() + 500.0)));
        }

        return enemies;
    }

    public Enemy createSlime(Vector2D position) {
        int health = levelManager.scaleEnemyHealth(20);
        int damage = levelManager.scaleEnemyDamage(4);
        return new Enemy(EnemyArchetype.SLIME, position, Constants.ENEMY_RADIUS, health, 70.0, damage, null, eventListener);
    }

    public Enemy createSkeletonArcher(Vector2D position) {
        int health = levelManager.scaleEnemyHealth(16);
        int damage = levelManager.scaleEnemyDamage(3);
        // Vũ khí tầm xa cho lính canh bắn cung
        Weapon weapon = new Gun("Skeleton Bow", damage, 1.5, 350.0, 0.0);

        // Tốc độ 90.0, khi lùi/né sẽ tăng tốc lên rất thông minh
        return new Enemy(EnemyArchetype.SKELETON_ARCHER, position, Constants.ENEMY_RADIUS, health, 90.0, damage, weapon, eventListener);
    }

    public Enemy createEliteMinion(Vector2D position) {
        int health = levelManager.scaleEnemyHealth(30);
        int damage = levelManager.scaleEnemyDamage(5); // Sát thương húc nặng đô

        // Đóng vai trò lợn rừng: Tốc độ cơ sở là 60.0 (khi húc sẽ nhân hệ số lên cực nhanh)
        return new Enemy(EnemyArchetype.ELITE_MINION, position, Constants.ENEMY_RADIUS + 2, health, 60.0, damage, null, eventListener);
    }
    public Boss createGrandKnight(Vector2D position) {
        int health = levelManager.scaleEnemyHealth(180);
        int damage = levelManager.scaleEnemyDamage(8);
        return new Boss(position, health, damage, eventListener);
    }
    // Hàm mới 1: Tách riêng logic tính toán số lượng quái gốc của màn chơi ra
    public int calculateEnemyCount() {
        return levelManager.scaleEnemyCount(levelManager.getCurrentLevel().baseEnemyCount());
    }

    // Hàm mới 2: Sinh quái dựa chính xác vào số lượng điểm sinh đã được GameWorld giới hạn
    public List<Enemy> createInitialEnemiesAtPoints(Random random, Vector2D playerSpawn, List<Vector2D> spawnPoints) {
        int count = spawnPoints.size(); // Số lượng quái chốt hạ dựa theo điểm sinh
        List<Enemy> enemies = new ArrayList<>(count);
        LevelManager.LevelDefinition level = levelManager.getCurrentLevel();

        for (int i = 0; i < count; i++) {
            Vector2D spawnPoint = spawnPoints.get(i);
            if (level.number() == 1) {
                enemies.add(createSlime(spawnPoint));
            } else if (level.number() == 2) {
                enemies.add(i % 3 == 0 ? createSkeletonArcher(spawnPoint) : createSlime(spawnPoint));
            } else {
                enemies.add(createEliteMinion(spawnPoint));
            }
        }

        // Chỉ sinh thêm Boss nếu đây là phòng Boss và chưa có con quái nào (hoặc tùy bạn thiết kế)
        if (level.bossLevel() && count > 0) {
            enemies.add(createGrandKnight(new Vector2D(playerSpawn.getX() + 700.0, playerSpawn.getY() + 500.0)));
        }

        return enemies;
    }
}
