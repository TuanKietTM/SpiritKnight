package com.soulknight.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
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
    public boolean hasProgress(String playerName) {
        String sql = """
            SELECT 1
            FROM player_saves
            WHERE player_name = ?
              AND (
                    current_room > 1
                    OR level > 1
                    OR score > 0
                    OR gold > 0
              )
            LIMIT 1
            """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, playerName);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }

        } catch (SQLException exception) {
            System.err.println(
                    "Loi kiem tra tien trinh: "
                            + exception.getMessage()
            );
            return false;
        }
    }
    public List<LeaderboardEntry> getLeaderboard(int limit) {
        String sql = """
            SELECT player_name, score, gold
            FROM player_saves
            ORDER BY score DESC, gold DESC
            LIMIT ?
            """;

        List<LeaderboardEntry> entries = new ArrayList<>();

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, Math.max(1, limit));

            try (ResultSet resultSet = statement.executeQuery()) {
                int rank = 1;

                while (resultSet.next()) {
                    entries.add(new LeaderboardEntry(
                            rank++,
                            resultSet.getString("player_name"),
                            resultSet.getInt("score"),
                            resultSet.getInt("gold")
                    ));
                }
            }

        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Khong the tai leaderboard.",
                    exception
            );
        }

        return entries;
    }
}