package com.soulknight.database;

import com.soulknight.entity.HeroSelectionManager;
import com.soulknight.entity.HeroType;
import com.soulknight.pet.PetSelectionManager;
import com.soulknight.pet.PetType;
import com.soulknight.weapon.WeaponSelectionManager;
import com.soulknight.weapon.WeaponType;

public final class EquipmentLoader {

    private static final String ITEM_TYPE_PET = "PET";
    private static final String ITEM_TYPE_WEAPON = "WEAPON";
    private static final String ITEM_TYPE_HERO = "HERO";

    private final ShopDAO shopDAO = new ShopDAO();

    public void loadForUser(int userId) {
        String petCode = shopDAO.getEquippedItem(userId, ITEM_TYPE_PET);
        String weaponCode = shopDAO.getEquippedItem(userId, ITEM_TYPE_WEAPON);
        String heroCode = shopDAO.getEquippedItem(userId, ITEM_TYPE_HERO);

        applyPet(petCode);
        applyWeapon(weaponCode);
        applyHero(heroCode);
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
        WeaponType weapon = WeaponType.BLASTER;

        if (code != null && !code.isBlank()) {
            try {
                weapon = WeaponType.valueOf(code);
            } catch (IllegalArgumentException exception) {
                System.err.println("Weapon trong DB khong hop le: " + code);
            }
        }

        WeaponSelectionManager.getInstance().selectWeapon(weapon);
    }

    private void applyHero(String code) {
        HeroType hero = HeroType.KNIGHT;

        if (code != null && !code.isBlank()) {
            try {
                hero = HeroType.valueOf(code);
            } catch (IllegalArgumentException exception) {
                System.err.println("Hero trong DB khong hop le: " + code);
            }
        }

        HeroSelectionManager.getInstance().selectHero(hero);
    }
}