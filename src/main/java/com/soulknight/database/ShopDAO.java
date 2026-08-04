package com.soulknight.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class ShopDAO {

    public static final String ITEM_TYPE_PET = "PET";
    public static final String ITEM_TYPE_WEAPON = "WEAPON";
    public static final String ITEM_TYPE_HERO = "HERO";
    public static final String ITEM_TYPE_BUFF = "BUFF";

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
                while (resultSet.next()) ownedItems.add(resultSet.getString("item_code"));
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
                INSERT IGNORE INTO user_inventory(user_id, item_type, item_code, equipped)
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
        return purchaseItem(userId, itemType, itemCode, price);
    }

    public PurchaseResult purchaseItem(int userId, String itemType, String itemCode, int price) {
        String checkOwnedSql = """
                SELECT 1
                FROM user_inventory
                WHERE user_id = ?
                  AND item_type = ?
                  AND item_code = ?
                """;

        String getBankGoldSql = """
                SELECT gold_bank
                FROM users
                WHERE id = ?
                FOR UPDATE
                """;

        String deductBankGoldSql = """
                UPDATE users
                SET gold_bank = gold_bank - ?
                WHERE id = ?
                  AND gold_bank >= ?
                """;

        String insertInventorySql = """
                INSERT INTO user_inventory(user_id, item_type, item_code, equipped)
                VALUES (?, ?, ?, FALSE)
                """;

        String safeItemType = normalizeItemType(itemType);
        String safeItemCode = itemCode == null ? "" : itemCode.trim();
        int safePrice = Math.max(0, price);

        if (userId <= 0 || safeItemCode.isBlank()) return PurchaseResult.SAVE_NOT_FOUND;

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

                try (PreparedStatement statement = connection.prepareStatement(getBankGoldSql)) {
                    statement.setInt(1, userId);

                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (!resultSet.next()) {
                            connection.rollback();
                            return PurchaseResult.SAVE_NOT_FOUND;
                        }
                        currentGold = resultSet.getInt("gold_bank");
                    }
                }

                if (currentGold < safePrice) {
                    connection.rollback();
                    return PurchaseResult.NOT_ENOUGH_GOLD;
                }

                try (PreparedStatement statement = connection.prepareStatement(deductBankGoldSql)) {
                    statement.setInt(1, safePrice);
                    statement.setInt(2, userId);
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


    public Map<String, Integer> getBuffQuantities(int userId) {
        String sql = """
                SELECT buff_type, quantity
                FROM user_buffs
                WHERE user_id = ?
                  AND quantity > 0
                """;

        Map<String, Integer> quantities = new HashMap<>();

        if (userId <= 0) {
            return quantities;
        }

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String buffType = resultSet.getString("buff_type");
                    int quantity = Math.max(0, resultSet.getInt("quantity"));

                    if (buffType != null && !buffType.isBlank() && quantity > 0) {
                        quantities.put(buffType.trim().toUpperCase(), quantity);
                    }
                }
            }

        } catch (SQLException exception) {
            throw new IllegalStateException("Khong the tai so luong buff cua nguoi choi.", exception);
        }

        return quantities;
    }

    public BuffPurchaseResult purchaseBuff(int userId, String username, String buffType, int price) {
        return purchaseBuff(userId, buffType, price);
    }

    public BuffPurchaseResult purchaseBuff(int userId, String buffType, int price) {
        String getBankGoldSql = """
                SELECT gold_bank
                FROM users
                WHERE id = ?
                FOR UPDATE
                """;

        String deductBankGoldSql = """
                UPDATE users
                SET gold_bank = gold_bank - ?
                WHERE id = ?
                  AND gold_bank >= ?
                """;

        String upsertBuffSql = """
                INSERT INTO user_buffs(user_id, buff_type, quantity)
                VALUES (?, ?, 1)
                ON DUPLICATE KEY UPDATE quantity = quantity + 1
                """;

        String safeBuffType = normalizeItemType(buffType);
        int safePrice = Math.max(0, price);

        if (userId <= 0 || safeBuffType.isBlank()) {
            return BuffPurchaseResult.SAVE_NOT_FOUND;
        }

        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);

            try {
                int currentGold;

                try (PreparedStatement statement = connection.prepareStatement(getBankGoldSql)) {
                    statement.setInt(1, userId);

                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (!resultSet.next()) {
                            connection.rollback();
                            return BuffPurchaseResult.SAVE_NOT_FOUND;
                        }

                        currentGold = resultSet.getInt("gold_bank");
                    }
                }

                if (currentGold < safePrice) {
                    connection.rollback();
                    return BuffPurchaseResult.NOT_ENOUGH_GOLD;
                }

                try (PreparedStatement statement = connection.prepareStatement(deductBankGoldSql)) {
                    statement.setInt(1, safePrice);
                    statement.setInt(2, userId);
                    statement.setInt(3, safePrice);

                    if (statement.executeUpdate() == 0) {
                        connection.rollback();
                        return BuffPurchaseResult.NOT_ENOUGH_GOLD;
                    }
                }

                try (PreparedStatement statement = connection.prepareStatement(upsertBuffSql)) {
                    statement.setInt(1, userId);
                    statement.setString(2, safeBuffType);
                    statement.executeUpdate();
                }

                connection.commit();
                return BuffPurchaseResult.SUCCESS;

            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }

        } catch (SQLException exception) {
            exception.printStackTrace();
            return BuffPurchaseResult.ERROR;
        }
    }

    public boolean consumeBuff(int userId, String buffType) {
        String updateSql = """
                UPDATE user_buffs
                SET quantity = quantity - 1
                WHERE user_id = ?
                  AND buff_type = ?
                  AND quantity > 0
                """;

        String deleteSql = """
                DELETE FROM user_buffs
                WHERE user_id = ?
                  AND buff_type = ?
                  AND quantity <= 0
                """;

        String safeBuffType = normalizeItemType(buffType);

        if (userId <= 0 || safeBuffType.isBlank()) {
            return false;
        }

        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);

            try {
                int updated;

                try (PreparedStatement statement = connection.prepareStatement(updateSql)) {
                    statement.setInt(1, userId);
                    statement.setString(2, safeBuffType);
                    updated = statement.executeUpdate();
                }

                if (updated == 0) {
                    connection.rollback();
                    return false;
                }

                try (PreparedStatement statement = connection.prepareStatement(deleteSql)) {
                    statement.setInt(1, userId);
                    statement.setString(2, safeBuffType);
                    statement.executeUpdate();
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
            throw new IllegalStateException("Khong the su dung buff.", exception);
        }
    }

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

        if (userId <= 0 || safeItemCode.isBlank()) return false;

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

                try (PreparedStatement statement = connection.prepareStatement(equipSql)) {
                    statement.setInt(1, userId);
                    statement.setString(2, safeItemType);
                    statement.setString(3, safeItemCode);

                    if (statement.executeUpdate() == 0) {
                        connection.rollback();
                        return false;
                    }
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
                SELECT gold_bank
                FROM users
                WHERE username = ?
                """;

        String safeUsername = username == null ? "" : username.trim();
        if (safeUsername.isBlank()) return 0;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, safeUsername);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) return resultSet.getInt("gold_bank");
            }

        } catch (SQLException exception) {
            throw new IllegalStateException("Khong the tai vang tich luy.", exception);
        }

        return 0;
    }

    public int getGold(int userId) {
        String sql = """
                SELECT gold_bank
                FROM users
                WHERE id = ?
                """;

        if (userId <= 0) return 0;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) return resultSet.getInt("gold_bank");
            }

        } catch (SQLException exception) {
            throw new IllegalStateException("Khong the tai vang tich luy.", exception);
        }

        return 0;
    }

    public int getGems(int userId) {
        String sql = """
                SELECT gem_bank
                FROM users
                WHERE id = ?
                """;

        if (userId <= 0) return 0;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) return resultSet.getInt("gem_bank");
            }

        } catch (SQLException exception) {
            throw new IllegalStateException("Khong the tai gem tich luy.", exception);
        }

        return 0;
    }

    public boolean addBankGold(int userId, int amount) {
        if (userId <= 0 || amount <= 0) return false;

        String sql = """
                UPDATE users
                SET gold_bank = gold_bank + ?
                WHERE id = ?
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, amount);
            statement.setInt(2, userId);
            return statement.executeUpdate() > 0;

        } catch (SQLException exception) {
            throw new IllegalStateException("Khong the cong vang tich luy.", exception);
        }
    }

    public boolean addBankGem(int userId, int amount) {
        if (userId <= 0 || amount <= 0) return false;

        String sql = """
                UPDATE users
                SET gem_bank = gem_bank + ?
                WHERE id = ?
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, amount);
            statement.setInt(2, userId);
            return statement.executeUpdate() > 0;

        } catch (SQLException exception) {
            throw new IllegalStateException("Khong the cong gem tich luy.", exception);
        }
    }

    public boolean depositRunRewards(int userId, int goldAmount, int gemAmount) {
        int safeGold = Math.max(0, goldAmount);
        int safeGem = Math.max(0, gemAmount);

        if (userId <= 0 || (safeGold == 0 && safeGem == 0)) return false;

        String sql = """
                UPDATE users
                SET gold_bank = gold_bank + ?,
                    gem_bank = gem_bank + ?
                WHERE id = ?
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, safeGold);
            statement.setInt(2, safeGem);
            statement.setInt(3, userId);
            return statement.executeUpdate() > 0;

        } catch (SQLException exception) {
            throw new IllegalStateException("Khong the luu phan thuong tich luy.", exception);
        }
    }

    private String normalizeItemType(String itemType) {
        if (itemType == null) return "";
        return itemType.trim().toUpperCase();
    }


    public enum PurchaseResult {
        SUCCESS,
        ALREADY_OWNED,
        NOT_ENOUGH_GOLD,
        SAVE_NOT_FOUND,
        ERROR
    }
    public enum BuffPurchaseResult {
        SUCCESS,
        NOT_ENOUGH_GOLD,
        SAVE_NOT_FOUND,
        ERROR
    }
}