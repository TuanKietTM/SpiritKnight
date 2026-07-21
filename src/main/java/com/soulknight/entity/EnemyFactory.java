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
            case SLIME -> createSlime(position);
            case SKELETON_ARCHER -> createSkeletonArcher(position);
            case ELITE_MINION -> createEliteMinion(position);
            case GRAND_KNIGHT -> createGrandKnight(position);
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
            if (roll < 65) {
                chosenArchetype = EnemyArchetype.SLIME;
            } else if (roll < 90) {
                chosenArchetype = EnemyArchetype.ELITE_MINION;
            } else {
                chosenArchetype = EnemyArchetype.SKELETON_ARCHER;
            }
        } else if (waveNumber == 2) {
            // Wave 2 (Trung bình): 35% Slime, 35% Lợn rừng húc, 30% Cung thủ
            if (roll < 35) {
                chosenArchetype = EnemyArchetype.SLIME;
            } else if (roll < 70) {
                chosenArchetype = EnemyArchetype.ELITE_MINION;
            } else {
                chosenArchetype = EnemyArchetype.SKELETON_ARCHER;
            }
        } else {
            // Wave 3+ (Thử thách): 15% Slime, 45% Lợn rừng húc, 40% Cung thủ
            if (roll < 15) {
                chosenArchetype = EnemyArchetype.SLIME;
            } else if (roll < 60) {
                chosenArchetype = EnemyArchetype.ELITE_MINION;
            } else {
                chosenArchetype = EnemyArchetype.SKELETON_ARCHER;
            }
        }

        return createEnemy(chosenArchetype, position);
    }


    public Enemy createSlime(Vector2D position) {
        int health = levelManager.scaleEnemyHealth(20);
        int damage = levelManager.scaleEnemyDamage(4);
        return new Enemy(EnemyArchetype.SLIME, position, Constants.ENEMY_RADIUS, health, 70.0, damage, null, eventListener);
    }

    public Enemy createSkeletonArcher(Vector2D position) {
        int health = levelManager.scaleEnemyHealth(16);
        int damage = levelManager.scaleEnemyDamage(3);
        Weapon weapon = new Gun("Skeleton Bow", damage, 1.5, 350.0, 0.0);
        return new Enemy(EnemyArchetype.SKELETON_ARCHER, position, Constants.ENEMY_RADIUS, health, 90.0, damage, weapon, eventListener);
    }

    public Enemy createEliteMinion(Vector2D position) {
        int health = levelManager.scaleEnemyHealth(30);
        int damage = levelManager.scaleEnemyDamage(5);
        return new Enemy(EnemyArchetype.ELITE_MINION, position, Constants.ENEMY_RADIUS + 2, health, 60.0, damage, null, eventListener);
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

    public List<Enemy> createInitialEnemiesAtPoints(Random random, Vector2D playerSpawn, List<Vector2D> spawnPoints) {
        int count = spawnPoints.size();
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

        if (level.bossLevel() && count > 0) {
            enemies.add(createGrandKnight(new Vector2D(playerSpawn.getX() + 700.0, playerSpawn.getY() + 500.0)));
        }

        return enemies;
    }
}