package com.soulknight.database;

import com.soulknight.buff.BuffInventoryManager;
import com.soulknight.buff.BuffType;
import com.soulknight.entity.HeroSelectionManager;
import com.soulknight.entity.HeroType;
import com.soulknight.pet.PetSelectionManager;
import com.soulknight.pet.PetType;
import com.soulknight.weapon.WeaponSelectionManager;
import com.soulknight.weapon.WeaponType;

import java.util.EnumMap;
import java.util.Map;

/**
 * Tải toàn bộ trạng thái trang bị của tài khoản khi đăng nhập.
 *
 * Weapon không còn dùng cơ chế equipped một vũ khí cũ.
 * Hai slot vũ khí được đọc từ user_weapon_loadout và đưa vào WeaponSelectionManager.
 */
public final class EquipmentLoader {

    private static final String ITEM_TYPE_PET = "PET";
    private static final String ITEM_TYPE_HERO = "HERO";

    private static final WeaponType DEFAULT_WEAPON_SLOT_1 = WeaponType.BLASTER;
    private static final WeaponType DEFAULT_WEAPON_SLOT_2 = WeaponType.OLD_SWORD;

    private final ShopDAO shopDAO = new ShopDAO();

    public void loadForUser(int userId) {
        if (userId <= 0) {
            resetEquipment();
            return;
        }

        String petCode = shopDAO.getEquippedItem(userId, ITEM_TYPE_PET);
        String heroCode = shopDAO.getEquippedItem(userId, ITEM_TYPE_HERO);
        Map<String, Integer> buffQuantities = shopDAO.getBuffQuantities(userId);

        applyPet(petCode);
        applyWeaponLoadout(userId);
        applyHero(heroCode);
        applyBuffs(buffQuantities);
    }

    /**
     * Reset state khi chưa có user hợp lệ hoặc đã đăng xuất.
     */
    private void resetEquipment() {
        applyPet(null);
        applyHero(null);
        WeaponSelectionManager.getInstance().reset();
        BuffInventoryManager.getInstance().clear();
    }

    private void applyPet(String code) {
        PetType pet = PetType.NONE;

        if (code != null && !code.isBlank()) {
            try {
                pet = PetType.valueOf(code.trim().toUpperCase());
            } catch (IllegalArgumentException exception) {
                System.err.println("Pet trong DB không hợp lệ: " + code);
            }
        }

        PetSelectionManager.getInstance().selectPet(pet);
    }

    /**
     * Điểm quan trọng:
     * EquipmentLoader chỉ tải 2 slot từ DB vào WeaponSelectionManager.
     * Nó không equip trực tiếp weapon cho Player; GameWorld sẽ snapshot 2 slot khi bắt đầu run.
     */
    private void applyWeaponLoadout(int userId) {
        ShopDAO.WeaponLoadout loadout = shopDAO.getWeaponLoadout(userId);

        // Tài khoản cũ chưa có loadout thì tạo 2 slot mặc định một lần trong DB.
        if (loadout == null) {
            ensureDefaultWeaponLoadout(userId);
            WeaponSelectionManager.getInstance().loadLoadout(DEFAULT_WEAPON_SLOT_1, DEFAULT_WEAPON_SLOT_2);
            return;
        }

        WeaponType slot1 = parseWeapon(loadout.slot1(), DEFAULT_WEAPON_SLOT_1);
        WeaponType slot2 = parseWeapon(loadout.slot2(), DEFAULT_WEAPON_SLOT_2);

        // Không cho cùng một weapon xuất hiện ở cả hai slot.
        if (slot1 == slot2) {
            System.err.println("Weapon loadout bị trùng slot trong DB: " + slot1);
            slot2 = getFallbackSecondWeapon(slot1);
        }

        WeaponSelectionManager.getInstance().loadLoadout(slot1, slot2);

        System.out.println("[EQUIPMENT] Weapon loadout: " + slot1 + " / " + slot2);
    }

    /**
     * Đảm bảo account có quyền sở hữu 2 weapon mặc định trước khi tạo loadout.
     * Nếu ShopDAO của bạn đã cấp starter weapon ở lúc đăng ký thì đoạn grant này vẫn an toàn
     * vì grantStarterWeapons nên dùng INSERT IGNORE / kiểm tra tồn tại.
     */
    private void ensureDefaultWeaponLoadout(int userId) {
        shopDAO.grantStarterWeapons(userId, DEFAULT_WEAPON_SLOT_1.name(), DEFAULT_WEAPON_SLOT_2.name());
        shopDAO.ensureDefaultWeaponLoadout(userId, DEFAULT_WEAPON_SLOT_1.name(), DEFAULT_WEAPON_SLOT_2.name());
    }

    private WeaponType parseWeapon(String code, WeaponType fallback) {
        if (code == null || code.isBlank()) {
            return fallback;
        }

        try {
            return WeaponType.valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            System.err.println("Weapon trong DB không hợp lệ: " + code);
            return fallback;
        }
    }

    private WeaponType getFallbackSecondWeapon(WeaponType slot1) {
        return slot1 != DEFAULT_WEAPON_SLOT_2 ? DEFAULT_WEAPON_SLOT_2 : DEFAULT_WEAPON_SLOT_1;
    }

    private void applyHero(String code) {
        HeroType hero = HeroType.KNIGHT;

        if (code != null && !code.isBlank()) {
            try {
                hero = HeroType.valueOf(code.trim().toUpperCase());
            } catch (IllegalArgumentException exception) {
                System.err.println("Hero trong DB không hợp lệ: " + code);
            }
        }

        HeroSelectionManager.getInstance().selectHero(hero);
    }

    private void applyBuffs(Map<String, Integer> rawBuffQuantities) {
        Map<BuffType, Integer> parsedBuffs = new EnumMap<>(BuffType.class);

        if (rawBuffQuantities != null) {
            for (Map.Entry<String, Integer> entry : rawBuffQuantities.entrySet()) {
                String code = entry.getKey();
                Integer quantity = entry.getValue();

                if (code == null || code.isBlank() || quantity == null || quantity <= 0) {
                    continue;
                }

                try {
                    BuffType type = BuffType.valueOf(code.trim().toUpperCase());
                    parsedBuffs.put(type, quantity);
                } catch (IllegalArgumentException exception) {
                    System.err.println("Buff trong DB không hợp lệ: " + code);
                }
            }
        }

        BuffInventoryManager.getInstance().replaceAll(parsedBuffs);
    }
}