package com.soulknight.buff;

import com.soulknight.entity.Player;

/**
 * Buff Holy Nova.
 *
 * Khi Player sap nhan lethal damage,
 * Player se chan don danh va kich hoat Holy Nova.
 */
public final class HolyNovaBuff extends Buff {

    public HolyNovaBuff(BuffType type) {
        super(type);
    }

    @Override
    protected void onActivate(Player player) {
        // Khong thay doi chi so Player
    }

    @Override
    protected void onRemove(Player player) {
        // Khong can reset chi so
    }
}