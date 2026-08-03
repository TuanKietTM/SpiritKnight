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


    /**
     * Tạo quái vật dựa trên Archetype được truyền vào va theo cac wave
     */
    public Enemy createEnemy(EnemyArchetype archetype, Vector2D position) {
        return switch (archetype) {
            case MELEE_NORMAL -> createMeleeNormal(position);
            case RANGED_NORMAL -> createRangedNormal(position);
            case RANGED_ELITE -> createRangedElite(position);
        };
    }


    /**
     * Tỉ lệ ngẫu nhiên sinh loại quái dựa vào đợt (waveNumber)
     */
    public Enemy createEnemyByWave(Random random, Vector2D position, int waveNumber) {
        int roll = random.nextInt(100); // Roll từ 0 -> 99

        EnemyArchetype chosenArchetype;

        if (waveNumber == 1) {
            // Wave 1 (Dễ): 65% Slime, 25% Lợn rừng húc, 10% Cung thủ
            if (roll < 60) {
                chosenArchetype = EnemyArchetype.MELEE_NORMAL;
            } else if (roll < 20) {
                chosenArchetype = EnemyArchetype.RANGED_NORMAL;
            } else {
                chosenArchetype = EnemyArchetype.RANGED_ELITE;
            }
        } else if (waveNumber == 2) {
            // Wave 2 (Trung bình): 35% Slime, 35% Lợn rừng húc, 30% Cung thủ
            if (roll < 40) {
                chosenArchetype = EnemyArchetype.MELEE_NORMAL;
            } else if (roll < 30) {
                chosenArchetype = EnemyArchetype.RANGED_NORMAL;
            } else {
                chosenArchetype = EnemyArchetype.RANGED_ELITE;
            }
        } else {
            // Wave 3+ (Thử thách): 15% Slime, 45% Lợn rừng húc, 40% Cung thủ
            if (roll < 20) {
                chosenArchetype = EnemyArchetype.MELEE_NORMAL;
            } else if (roll < 40) {
                chosenArchetype = EnemyArchetype.RANGED_NORMAL;
            } else {
                chosenArchetype = EnemyArchetype.RANGED_ELITE;
            }
        }

        return createEnemy(chosenArchetype, position);
    }

    // Hàm tạo quái cận chiến thường
    public Enemy createMeleeNormal(Vector2D position) {
        int health = levelManager.scaleEnemyHealth(20);
        int damage = levelManager.scaleEnemyDamage(4);
        return new Enemy(EnemyArchetype.MELEE_NORMAL, position, Constants.ENEMY_RADIUS, health, 70.0, damage, null, eventListener);
    }

    // Hàm tạo quái đánh xa thường
    public Enemy createRangedNormal(Vector2D position) {
        int health = levelManager.scaleEnemyHealth(16);
        int damage = levelManager.scaleEnemyDamage(3);
        Weapon weapon = new Gun("Skeleton Bow", damage, 1.5, 350.0, 0.0);
        return new Enemy(EnemyArchetype.RANGED_NORMAL, position, Constants.ENEMY_RADIUS, health, 90.0, damage, weapon, eventListener);
    }

    // Hàm tạo quái đánh xa tiến hóa
    public Enemy createRangedElite(Vector2D position) {
        int health = levelManager.scaleEnemyHealth(28);
        int damage = levelManager.scaleEnemyDamage(3);
        Weapon weapon = new Gun("Elite Shotgun", damage, 2.0, 300.0, 0.0);
        return new Enemy(EnemyArchetype.RANGED_ELITE, position, Constants.ENEMY_RADIUS + 2, health, 80.0, damage, weapon, eventListener);
    }

    public Boss createGrandKnight(Vector2D position) {
        int health = levelManager.scaleEnemyHealth(180);
        int damage = levelManager.scaleEnemyDamage(8);
        return new Boss(position, health, damage, eventListener);
    }


    public int calculateEnemyCount() {
        return levelManager.scaleEnemyCount(levelManager.getCurrentLevel().baseEnemyCount());
    }

    public List<Enemy> createInitialEnemies(Random random, Vector2D playerSpawn, List<Vector2D> spawnPoints) {
        int count = levelManager.scaleEnemyCount(levelManager.getCurrentLevel().baseEnemyCount());
        List<Enemy> enemies = new ArrayList<>(count);
        LevelManager.LevelDefinition level = levelManager.getCurrentLevel();

        for (int i = 0; i < count; i++) {
            Vector2D spawnPoint = spawnPoints.get(i % spawnPoints.size());
            enemies.add(createEnemyByWave(random, spawnPoint, level.number()));
            if (level.number() == 1) {
                enemies.add(createMeleeNormal(spawnPoint));
            } else if (level.number() == 2) {
                enemies.add(i % 3 == 0 ? createRangedNormal(spawnPoint) : createMeleeNormal(spawnPoint));
            }
        }

        if (level.bossLevel()) {
            enemies.add(createGrandKnight(new Vector2D(playerSpawn.getX() + 700.0, playerSpawn.getY() + 500.0)));
        }

        return enemies;
    }

}