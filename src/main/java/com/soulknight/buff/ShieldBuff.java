package com.soulknight.buff;

import com.soulknight.entity.Player;

public class ShieldBuff extends Buff {

    public ShieldBuff(BuffType type) {
        super(type);
    }

    @Override
    protected void onActivate(Player player) {
        player.setBuffDamageReduction(getType().getValue());
    }

    @Override
    protected void onRemove(Player player) {
        player.setBuffDamageReduction(0.0);
    }
}