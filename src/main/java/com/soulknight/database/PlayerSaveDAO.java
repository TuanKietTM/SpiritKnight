package com.soulknight.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public final class PlayerSaveDAO {

    public boolean save(PlayerSave playerSave) {
        String sql = """
                INSERT INTO player_saves (
                    player_name,
                    level,
                    gold,
                    gems,
                    hp,
                    energy,
                    current_room,
                    score
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    level = VALUES(level),
                    gold = VALUES(gold),
                    gems = VALUES(gems),
                    hp = VALUES(hp),
                    energy = VALUES(energy),
                    current_room = VALUES(current_room),
                    score = VALUES(score)
                """;

        try (
                Connection connection =
                        DatabaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setString(
                    1,
                    playerSave.getPlayerName()
            );

            statement.setInt(2, playerSave.getLevel());
            statement.setInt(3, playerSave.getGold());
            statement.setInt(4, playerSave.getGems());
            statement.setDouble(5, playerSave.getHp());
            statement.setDouble(6, playerSave.getEnergy());
            statement.setInt(7, playerSave.getCurrentRoom());
            statement.setInt(8, playerSave.getScore());

            return statement.executeUpdate() > 0;

        } catch (SQLException exception) {
            System.err.println("Lỗi lưu dữ liệu người chơi: " + exception.getMessage());
            return false;
        }
    }

    public Optional<PlayerSave> findByName(
            String playerName
    ) {
        String sql = """
                SELECT
                    id,
                    player_name,
                    level,
                    gold,
                    gems,
                    hp,
                    energy,
                    current_room,
                    score
                FROM player_saves
                WHERE player_name = ?
                """;

        try (
                Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, playerName);

            try (
                    ResultSet resultSet = statement.executeQuery()
            ) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }

                PlayerSave playerSave = new PlayerSave();

                playerSave.setId(resultSet.getInt("id"));

                playerSave.setPlayerName(resultSet.getString("player_name"));

                playerSave.setLevel(resultSet.getInt("level"));

                playerSave.setGold(resultSet.getInt("gold"));

                playerSave.setGems(resultSet.getInt("gems"));

                playerSave.setHp(resultSet.getDouble("hp"));

                playerSave.setEnergy(resultSet.getDouble("energy"));

                playerSave.setCurrentRoom(resultSet.getInt("current_room"));

                playerSave.setScore(resultSet.getInt("score"));

                return Optional.of(playerSave);
            }

        } catch (SQLException exception) {
            System.err.println("Loi tai du lieu nguoi choi  " + exception.getMessage()
            );
            return Optional.empty();
        }
    }
}