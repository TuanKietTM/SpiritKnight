package com.soulknight.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

public final class ShopDAO {

    public static final String ITEM_TYPE_PET = "PET";
    public static final String ITEM_TYPE_WEAPON = "WEAPON";
    public static final String ITEM_TYPE_HERO = "HERO";

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
            statement.setString(2, normalizeItemType(itemType));

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ownedItems.add(resultSet.getString("item_code"));
                }
            }

        } catch (SQLException exception) {
            throw new IllegalStateException("Khong the tai danh sach vat pham cua nguoi choi.", exception);
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
            statement.setString(2, normalizeItemType(itemType));

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) return resultSet.getString("item_code");
            }

        } catch (SQLException exception) {
            throw new IllegalStateException("Khong the tai vat pham dang trang bi.", exception);
        }

        return null;
    }

    /*
     * Cap pet, weapon va hero khoi dau trong cung mot transaction.
     * Item khoi dau chi duoc equipped neu loai do chua co item nao dang equipped.
     */
    public void grantStarterEquipment(int userId, String starterPet, String starterWeapon, String starterHero) {
        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);

            try {
                grantStarterItem(connection, userId, ITEM_TYPE_PET, starterPet);
                grantStarterItem(connection, userId, ITEM_TYPE_WEAPON, starterWeapon);
                grantStarterItem(connection, userId, ITEM_TYPE_HERO, starterHero);
                connection.commit();

            } catch (SQLException exception) {
                connection.rollback();
                throw exception;

            } finally {
                connection.setAutoCommit(true);
            }

        } catch (SQLException exception) {
            throw new IllegalStateException("Khong the cap trang bi khoi dau.", exception);
        }
    }

    /*
     * Giu lai ham cu de cac cho dang goi khong bi loi.
     * Hero co the duoc cap rieng bang grantStarterHero().
     */
    public void grantStarterItems(int userId, String starterPet, String starterWeapon) {
        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);

            try {
                grantStarterItem(connection, userId, ITEM_TYPE_PET, starterPet);
                grantStarterItem(connection, userId, ITEM_TYPE_WEAPON, starterWeapon);
                connection.commit();

            } catch (SQLException exception) {
                connection.rollback();
                throw exception;

            } finally {
                connection.setAutoCommit(true);
            }

        } catch (SQLException exception) {
            throw new IllegalStateException("Khong the cap vat pham khoi dau.", exception);
        }
    }

    public void grantStarterHero(int userId, String starterHero) {
        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);

            try {
                grantStarterItem(connection, userId, ITEM_TYPE_HERO, starterHero);
                connection.commit();

            } catch (SQLException exception) {
                connection.rollback();
                throw exception;

            } finally {
                connection.setAutoCommit(true);
            }

        } catch (SQLException exception) {
            throw new IllegalStateException("Khong the cap hero khoi dau.", exception);
        }
    }

    private void grantStarterItem(Connection connection, int userId, String itemType, String itemCode) throws SQLException {
        if (itemCode == null || itemCode.isBlank()) return;

        String sql = """
                INSERT IGNORE INTO user_inventory (user_id, item_type, item_code, equipped)
                SELECT ?, ?, ?, NOT EXISTS (
                    SELECT 1
                    FROM user_inventory
                    WHERE user_id = ?
                      AND item_type = ?
                      AND equipped = TRUE
                )
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setString(2, itemType);
            statement.setString(3, itemCode.trim());
            statement.setInt(4, userId);
            statement.setString(5, itemType);
            statement.executeUpdate();
        }
    }

    public PurchaseResult purchaseItem(int userId, String username, String itemType, String itemCode, int price) {
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
                INSERT INTO user_inventory (user_id, item_type, item_code, equipped)
                VALUES (?, ?, ?, FALSE)
                """;

        String safeItemType = normalizeItemType(itemType);
        String safeItemCode = itemCode == null ? "" : itemCode.trim();
        String safeUsername = username == null ? "" : username.trim();
        int safePrice = Math.max(0, price);

        if (safeUsername.isBlank() || safeItemCode.isBlank()) return PurchaseResult.SAVE_NOT_FOUND;

        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);

            try {
                try (PreparedStatement statement = connection.prepareStatement(checkOwnedSql)) {
                    statement.setInt(1, userId);
                    statement.setString(2, safeItemType);
                    statement.setString(3, safeItemCode);

                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (resultSet.next()) {
                            connection.rollback();
                            return PurchaseResult.ALREADY_OWNED;
                        }
                    }
                }

                int currentGold;

                try (PreparedStatement statement = connection.prepareStatement(getGoldSql)) {
                    statement.setString(1, safeUsername);

                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (!resultSet.next()) {
                            connection.rollback();
                            return PurchaseResult.SAVE_NOT_FOUND;
                        }

                        currentGold = resultSet.getInt("gold");
                    }
                }

                if (currentGold < safePrice) {
                    connection.rollback();
                    return PurchaseResult.NOT_ENOUGH_GOLD;
                }

                try (PreparedStatement statement = connection.prepareStatement(deductGoldSql)) {
                    statement.setInt(1, safePrice);
                    statement.setString(2, safeUsername);
                    statement.setInt(3, safePrice);

                    if (statement.executeUpdate() == 0) {
                        connection.rollback();
                        return PurchaseResult.NOT_ENOUGH_GOLD;
                    }
                }

                try (PreparedStatement statement = connection.prepareStatement(insertInventorySql)) {
                    statement.setInt(1, userId);
                    statement.setString(2, safeItemType);
                    statement.setString(3, safeItemCode);
                    statement.executeUpdate();
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
            throw new IllegalStateException("Khong the mua vat pham.", exception);
        }
    }

    /*
     * Moi loai PET, WEAPON hoac HERO chi duoc co mot item equipped.
     * Chi thay doi RAM sau khi ham nay tra ve true.
     */
    public boolean equipItem(int userId, String itemType, String itemCode) {
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

        String safeItemType = normalizeItemType(itemType);
        String safeItemCode = itemCode == null ? "" : itemCode.trim();

        if (safeItemCode.isBlank()) return false;

        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);

            try {
                try (PreparedStatement statement = connection.prepareStatement(checkOwnedSql)) {
                    statement.setInt(1, userId);
                    statement.setString(2, safeItemType);
                    statement.setString(3, safeItemCode);

                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (!resultSet.next()) {
                            connection.rollback();
                            return false;
                        }
                    }
                }

                try (PreparedStatement statement = connection.prepareStatement(unequipSql)) {
                    statement.setInt(1, userId);
                    statement.setString(2, safeItemType);
                    statement.executeUpdate();
                }

                int updatedRows;

                try (PreparedStatement statement = connection.prepareStatement(equipSql)) {
                    statement.setInt(1, userId);
                    statement.setString(2, safeItemType);
                    statement.setString(3, safeItemCode);
                    updatedRows = statement.executeUpdate();
                }

                if (updatedRows == 0) {
                    connection.rollback();
                    return false;
                }

                connection.commit();
                return true;

            } catch (SQLException exception) {
                connection.rollback();
                throw exception;

            } finally {
                connection.setAutoCommit(true);
            }

        } catch (SQLException exception) {
            throw new IllegalStateException("Khong the trang bi vat pham.", exception);
        }
    }

    public int getGold(String username) {
        String sql = """
                SELECT gold
                FROM player_saves
                WHERE player_name = ?
                """;

        String safeUsername = username == null ? "" : username.trim();
        if (safeUsername.isBlank()) return 0;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, safeUsername);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) return resultSet.getInt("gold");
            }

        } catch (SQLException exception) {
            throw new IllegalStateException("Khong the tai so vang.", exception);
        }

        return 0;
    }

    private String normalizeItemType(String itemType) {
        if (itemType == null) return "";
        return itemType.trim().toUpperCase();
    }

    public enum PurchaseResult {
        SUCCESS,
        ALREADY_OWNED,
        NOT_ENOUGH_GOLD,
        SAVE_NOT_FOUND
    }
}