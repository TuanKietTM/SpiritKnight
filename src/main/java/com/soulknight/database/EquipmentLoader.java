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
 * Class này chịu trách nhiệm tải dữ liệu trang bị
 * của người chơi từ cơ sở dữ liệu và áp dụng chúng vào trò chơi.
 */
public final class EquipmentLoader {

    private static final String ITEM_TYPE_PET = "PET";
    private static final String ITEM_TYPE_WEAPON = "WEAPON";
    private static final String ITEM_TYPE_HERO = "HERO";

    private final ShopDAO shopDAO = new ShopDAO();

    public void loadForUser(int userId) {
        if (userId <= 0) {
            applyPet(null);
            applyWeapon(null);
            applyHero(null);
            BuffInventoryManager.getInstance().clear();
            return;
        }

        String petCode = shopDAO.getEquippedItem(userId, ITEM_TYPE_PET);
        String weaponCode = shopDAO.getEquippedItem(userId, ITEM_TYPE_WEAPON);
        String heroCode = shopDAO.getEquippedItem(userId, ITEM_TYPE_HERO);
        Map<String, Integer> buffQuantities = shopDAO.getBuffQuantities(userId);

        applyPet(petCode);
        applyWeapon(weaponCode);
        applyHero(heroCode);
        applyBuffs(buffQuantities);
    }

    private void applyPet(String code) {
        PetType pet = PetType.NONE;

        if (code != null && !code.isBlank()) {
            try {
                pet = PetType.valueOf(code);
            } catch (IllegalArgumentException exception) {
                System.err.println("Pet trong DB khong hop le: " + code);
            }
        }

        PetSelectionManager.getInstance().selectPet(pet);
    }

    private void applyWeapon(String code) {
        // LUÔN LUÔN bắt đầu với Old Pistol, bỏ qua database
        // Để người chơi phải tìm hộp quà để nâng cấp vũ khí
        WeaponType weapon = WeaponType.OLD_PISTOL;

        // Bỏ qua giá trị từ database, luôn dùng OLD_PISTOL
        // if (code != null && !code.isBlank()) {
        //     try {
        //         weapon = WeaponType.valueOf(code);
        //     } catch (IllegalArgumentException exception) {
        //         System.err.println("Weapon trong DB khong hop le: " + code);
        //     }
        // }

        WeaponSelectionManager.getInstance().selectWeapon(weapon);
    }

    private void applyHero(String code) {
        HeroType hero = HeroType.KNIGHT;

        if (code != null && !code.isBlank()) {
            try {
                hero = HeroType.valueOf(code);
            } catch (IllegalArgumentException exception) {
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
                    System.err.println("Buff trong DB khong hop le: " + code);
                }
            }
        }

        BuffInventoryManager.getInstance().replaceAll(parsedBuffs);
    }

}