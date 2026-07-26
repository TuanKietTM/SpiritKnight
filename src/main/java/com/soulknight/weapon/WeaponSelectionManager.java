package com.soulknight.weapon;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Lưu vũ khí hiện đang được trang bị (chọn từ Shop).
 */
public final class WeaponSelectionManager {

    private static final WeaponSelectionManager INSTANCE =
            new WeaponSelectionManager();

    /*
     * Vũ khí mặc định khi bắt đầu game.
     */
    private WeaponType selectedWeapon = WeaponType.OLD_PISTOL;

    private Consumer<WeaponType> selectionListener;

    private WeaponSelectionManager() {
    }

    public static WeaponSelectionManager getInstance() {
        return INSTANCE;
    }

    public WeaponType getSelectedWeapon() {
        return selectedWeapon;
    }

    public void selectWeapon(WeaponType type) {
        WeaponType safeType = Objects.requireNonNullElse(
                type,
                WeaponType.OLD_PISTOL
        );

        if (selectedWeapon == safeType) {
            return;
        }

        selectedWeapon = safeType;

        if (selectionListener != null) {
            selectionListener.accept(selectedWeapon);
        }
    }

    public boolean isSelected(WeaponType type) {
        return selectedWeapon == type;
    }

    public void setSelectionListener(
            Consumer<WeaponType> selectionListener
    ) {
        this.selectionListener = selectionListener;
    }
}
