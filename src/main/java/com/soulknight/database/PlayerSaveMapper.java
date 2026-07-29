package com.soulknight.database;

import com.soulknight.engine.GameWorld;
import com.soulknight.entity.Player;

public final class PlayerSaveMapper {

    private PlayerSaveMapper() {
    }

    /**
     * Chuyen du lieu dang chay trong  GameWorld
     * thanh du lieu luu vao  database.
     */
    public static PlayerSave fromWorld(GameWorld world) {
        if (world == null) {
            throw new IllegalArgumentException(
                    "GameWorld not null."
            );
        }

        Player player = world.getPlayer();

        double hp = 100.0;

        if (player != null) {
            hp = player.getHealth();
        }

        int levelNumber = 1;

        if (world.getLevelManager() != null
                && world.getLevelManager().getCurrentLevel() != null) {

            levelNumber = world.getLevelManager().getCurrentLevel().number();
        }

        return new PlayerSave(world.getCurrentPlayerName(), levelNumber, world.getGold(), world.getGems(),
                hp, world.getPlayerEnergy(), world.getCurrentRoomNumber(), world.getScore()
        );
    }

    /**
     * Ap dung du lieu da luu tu dataset vao gameworld
     */
    public static void applyToWorld(
            GameWorld world,
            PlayerSave save
    ) {
        if (world == null || save == null) {
            return;
        }

        world.setCurrentPlayerName(
                save.getPlayerName()
        );

        world.setGold(
                save.getGold()
        );

        world.setGems(
                save.getGems()
        );

        world.setPlayerEnergy(
                save.getEnergy()
        );

        world.setCurrentRoomNumber(
                save.getCurrentRoom()
        );

        world.setScore(
                save.getScore()
        );

        Player player = world.getPlayer();

        if (player != null) {
            player.setHealth(
                    (int) save.getHp()
            );
        }

        System.out.println(
                "Đã áp dụng save: level="
                        + save.getLevel()
                        + ", room="
                        + save.getCurrentRoom()
        );
    }
}