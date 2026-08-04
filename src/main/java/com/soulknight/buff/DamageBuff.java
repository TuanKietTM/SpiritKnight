package com.soulknight.buff;

import com.soulknight.entity.Player;

public class DamageBuff extends Buff {

    public DamageBuff(BuffType type) {
        super(type);
    }

    @Override
    protected void onActivate(Player player) {
        player.setBuffDamageMultiplier(1.0 + getType().getValue());
    }

    @Override
    protected void onRemove(Player player) {
        player.setBuffDamageMultiplier(1.0);
    }
}