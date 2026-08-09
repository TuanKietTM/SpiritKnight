package com.soulknight.weapon;

public final class WeaponSelectionManager {

    private static final WeaponSelectionManager INSTANCE =
            new WeaponSelectionManager();

    /*
     * Loadout truoc khi vao game.
     *
     * Shop chi thay doi 2 bien nay.
     */
    private WeaponType slot1 =
            WeaponType.BLASTER;

    private WeaponType slot2 =
            WeaponType.OLD_SWORD;

    private WeaponSelectionManager() {
    }

    public static WeaponSelectionManager getInstance() {
        return INSTANCE;
    }

    public WeaponType getSlot1() {
        return slot1;
    }

    public WeaponType getSlot2() {
        return slot2;
    }

    public boolean equipSlot1(WeaponType weapon) {
        if (weapon == null) {
            return false;
        }
        if (weapon == slot2) {
            return false;
        }
        slot1 = weapon;
        System.out.println("[LOADOUT] Slot 1 = " + weapon.name());
        return true;
    }

    public boolean equipSlot2(WeaponType weapon) {
        if (weapon == null) {
            return false;
        }
        if (weapon == slot1) {
            return false;
        }

        slot2 = weapon;
        System.out.println("[LOADOUT] Slot 2 = " + weapon.name());
        return true;
    }

    public boolean isSlot1(WeaponType weapon) {
        return weapon != null && weapon == slot1;
    }

    public boolean isSlot2(WeaponType weapon) {
        return weapon != null && weapon == slot2;
    }

    public boolean isEquipped(WeaponType weapon) {
        return isSlot1(weapon) || isSlot2(weapon);
    }

    public void loadLoadout(WeaponType slot1, WeaponType slot2) {
        WeaponType safeSlot1 = slot1 != null ? slot1 : WeaponType.BLASTER;
        WeaponType safeSlot2 = slot2 != null ? slot2 : WeaponType.OLD_SWORD;
        if (safeSlot1 == safeSlot2) {
            safeSlot2 = safeSlot1 != WeaponType.OLD_SWORD ? WeaponType.OLD_SWORD : WeaponType.BLASTER;
        }

        this.slot1 = safeSlot1;
        this.slot2 = safeSlot2;
    }
    public void reset() {
        slot1 = WeaponType.BLASTER;
        slot2 = WeaponType.OLD_SWORD;
    }
}