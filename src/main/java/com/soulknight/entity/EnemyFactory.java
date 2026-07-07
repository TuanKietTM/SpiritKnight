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
        Weapon weapon = new Gun("Skeleton Bow", damage, 1.35, 300.0, 0.0);
        return new Enemy(EnemyArchetype.SKELETON_ARCHER, position, Constants.ENEMY_RADIUS, health, 110.0, damage, weapon, eventListener);
    }

    public Enemy createEliteMinion(Vector2D position) {
        int health = levelManager.scaleEnemyHealth(32);
        int damage = levelManager.scaleEnemyDamage(6);
        return new Enemy(EnemyArchetype.ELITE_MINION, position, Constants.ENEMY_RADIUS + 2, health, 145.0, damage, null, eventListener);
    }

    public Boss createGrandKnight(Vector2D position) {
        int health = levelManager.scaleEnemyHealth(180);
        int damage = levelManager.scaleEnemyDamage(8);
        return new Boss(position, health, damage, eventListener);
    }
}
