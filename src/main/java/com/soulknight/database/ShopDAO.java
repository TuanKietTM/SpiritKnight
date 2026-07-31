package com.soulknight.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashSet;
import java.util.Set;

public final class ShopDAO {

    public Set<String> getOwnedItems(int userId, String itemType) {
        String sql = """
                SELECT item_code
                FROM user_inventory
                WHERE user_id = ?
                  AND item_type = ?
                """;

        Set<String> ownedItems = new HashSet<>();

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            statement.setString(2, itemType);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ownedItems.add(resultSet.getString("item_code"));
                }
            }

        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Không thể tải danh sách vật phẩm của người chơi.",
                    exception
            );
        }

        return ownedItems;
    }

    public String getEquippedItem(int userId, String itemType) {
        String sql = """
                SELECT item_code
                FROM user_inventory
                WHERE user_id = ?
                  AND item_type = ?
                  AND equipped = TRUE
                LIMIT 1
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            statement.setString(2, itemType);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString("item_code");
                }
            }

        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Không thể tải vật phẩm đang trang bị.",
                    exception
            );
        }

        return null;
    }

    public void grantStarterItems(
            int userId,
            String starterPet,
            String starterWeapon
    ) {
        String insertSql = """
                INSERT IGNORE INTO user_inventory
                    (user_id, item_type, item_code, equipped)
                VALUES
                    (?, 'PET', ?, TRUE),
                    (?, 'WEAPON', ?, TRUE)
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(insertSql)) {

            statement.setInt(1, userId);
            statement.setString(2, starterPet);
            statement.setInt(3, userId);
            statement.setString(4, starterWeapon);

            statement.executeUpdate();

        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Không thể cấp vật phẩm khởi đầu.",
                    exception
            );
        }
    }

    public PurchaseResult purchaseItem(
            int userId,
            String username,
            String itemType,
            String itemCode,
            int price
    ) {
        String checkOwnedSql = """
                SELECT 1
                FROM user_inventory
                WHERE user_id = ?
                  AND item_type = ?
                  AND item_code = ?
                """;

        String getGoldSql = """
        SELECT gold
        FROM player_saves
        WHERE player_name = ?
        FOR UPDATE
        """;

        String deductGoldSql = """
        UPDATE player_saves
        SET gold = gold - ?
        WHERE player_name = ?
          AND gold >= ?
        """;

        String insertInventorySql = """
                INSERT INTO user_inventory
                    (user_id, item_type, item_code, equipped)
                VALUES (?, ?, ?, FALSE)
                """;

        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);

            try {
                try (PreparedStatement checkOwned =
                             connection.prepareStatement(checkOwnedSql)) {

                    checkOwned.setInt(1, userId);
                    checkOwned.setString(2, itemType);
                    checkOwned.setString(3, itemCode);

                    try (ResultSet resultSet = checkOwned.executeQuery()) {
                        if (resultSet.next()) {
                            connection.rollback();
                            return PurchaseResult.ALREADY_OWNED;
                        }
                    }
                }

                int currentGold;

                try (PreparedStatement getGold =
                             connection.prepareStatement(getGoldSql)) {

                    getGold.setString(1, username);

                    try (ResultSet resultSet = getGold.executeQuery()) {
                        if (!resultSet.next()) {
                            connection.rollback();
                            return PurchaseResult.SAVE_NOT_FOUND;
                        }

                        currentGold = resultSet.getInt("gold");
                    }
                }

                if (currentGold < price) {
                    connection.rollback();
                    return PurchaseResult.NOT_ENOUGH_GOLD;
                }

                try (PreparedStatement deductGold =
                             connection.prepareStatement(deductGoldSql)) {


                    deductGold.setInt(1, price);
                    deductGold.setString(2, username);
                    deductGold.setInt(3, price);

                    int updatedRows = deductGold.executeUpdate();

                    if (updatedRows == 0) {
                        connection.rollback();
                        return PurchaseResult.NOT_ENOUGH_GOLD;
                    }
                }

                try (PreparedStatement insertInventory =
                             connection.prepareStatement(insertInventorySql)) {

                    insertInventory.setInt(1, userId);
                    insertInventory.setString(2, itemType);
                    insertInventory.setString(3, itemCode);
                    insertInventory.executeUpdate();
                }

                connection.commit();
                return PurchaseResult.SUCCESS;

            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }

        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Không thể mua vật phẩm.",
                    exception
            );
        }
    }

    public boolean equipItem(
            int userId,
            String itemType,
            String itemCode
    ) {
        String checkOwnedSql = """
                SELECT 1
                FROM user_inventory
                WHERE user_id = ?
                  AND item_type = ?
                  AND item_code = ?
                """;

        String unequipSql = """
                UPDATE user_inventory
                SET equipped = FALSE
                WHERE user_id = ?
                  AND item_type = ?
                """;

        String equipSql = """
                UPDATE user_inventory
                SET equipped = TRUE
                WHERE user_id = ?
                  AND item_type = ?
                  AND item_code = ?
                """;

        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);

            try {
                try (PreparedStatement checkOwned =
                             connection.prepareStatement(checkOwnedSql)) {

                    checkOwned.setInt(1, userId);
                    checkOwned.setString(2, itemType);
                    checkOwned.setString(3, itemCode);

                    try (ResultSet resultSet = checkOwned.executeQuery()) {
                        if (!resultSet.next()) {
                            connection.rollback();
                            return false;
                        }
                    }
                }

                try (PreparedStatement unequip =
                             connection.prepareStatement(unequipSql)) {

                    unequip.setInt(1, userId);
                    unequip.setString(2, itemType);
                    unequip.executeUpdate();
                }

                int rows;

                try (PreparedStatement equip =
                             connection.prepareStatement(equipSql)) {

                    equip.setInt(1, userId);
                    equip.setString(2, itemType);
                    equip.setString(3, itemCode);
                    rows = equip.executeUpdate();
                }

                connection.commit();
                return rows > 0;

            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }

        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Không thể trang bị vật phẩm.",
                    exception
            );
        }
    }

    public int getGold(String username) {
        String sql = """
            SELECT gold
            FROM player_saves
            WHERE player_name = ?
            """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, username);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("gold");
                }
            }

        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Không thể tải số vàng.",
                    exception
            );
        }

        return 0;
    }

    public enum PurchaseResult {
        SUCCESS,
        ALREADY_OWNED,
        NOT_ENOUGH_GOLD,
        SAVE_NOT_FOUND
    }
}